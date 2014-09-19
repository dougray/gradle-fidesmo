/*
 * Copyright 2014 Fidesmo AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.fidesmo.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.InvalidUserDataException
import org.gradle.api.GradleException

import com.fidesmo.gradle.javacard.JavacardPlugin
import com.fidesmo.gradle.javacard.JavacardExtension

import com.fidesmo.sec.client.RetrofitSecClient
import com.fidesmo.sec.client.OperationClient
import com.fidesmo.sec.client.ClientCallback
import com.fidesmo.sec.transceivers.AbstractTransceiver

import retrofit.*
import retrofit.mime.TypedFile
import retrofit.RequestInterceptor.RequestFacade

import java.util.UUID

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import com.fidesmo.gradle.plugin.models.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FidesmoPlugin implements Plugin<Project> {

    Logger logger = LoggerFactory.getLogger(this.getClass())

    public static final String FIDESMO_RID = '0xa0:0x00:0x00:0x06:0x17'
    public static final String FIDESMO_APP_ID = 'fidesmoAppId'
    public static final String FIDESMO_APP_KEY = 'fidesmoAppKey'

    // TODO: add as input property of card interaction tasks (an abstract class that needs to be created)
    def cardTimeout = 10

    // TODO: replace with memonize
    private Map<String, ?> consoleCache = [:]
    private String getPropertieOrRead(Project project, String key, String msg) {
        if (project.hasProperty(key)) {
            project.property(key)
        } else {
            if (consoleCache[key]) {
                consoleCache[key]
            } else {
                def value = System.console().readLine(msg)
                consoleCache[key] = value
            }
        }
    }

    private String getFidesmoAppId(Project project) {
        getPropertieOrRead(project, FIDESMO_APP_ID, "\nPlease specify fidesmo app id: ")
    }

    private String getFidesmoAppKey(Project project) {
        getPropertieOrRead(project, FIDESMO_APP_KEY, "\nPlease specify fidesmo app key: ")
    }

    def getFidesmoService(Project project) {
        def restAdapter = new RestAdapter.Builder()
           .setEndpoint('https://api.fidesmo.com')
           .setRequestInterceptor(
               new RequestInterceptor(){
                   void intercept(RequestFacade request) {
                       request.addHeader('app_id', getFidesmoAppId(project))
                       request.addHeader('app_key', getFidesmoAppKey(project))
                   }
               })
           .setErrorHandler(
               new ErrorHandler(){
                   Throwable handleError(RetrofitError cause) {
                       if (cause.isNetworkError()) {
                           new GradleException('An network related error occured while uploading the cap file', cause)
                       } else {
                           try {
                               def errorMessage = cause.response.body.in().text
                               new GradleException("The fidemo server aborted the operation with '${errorMessage}'", cause)
                           } catch(any) {
                               cause
                           }
                       }
                   }
               })
           .build();

        restAdapter.create(FidesmoService.class)
    }

    def executeOperation(UUID operationId) {
        logger.info("Starting fidesmo sec-client to execute operation '${operationId}'")
        def latch = new CountDownLatch(1)
        def client = OperationClient.getInstance(
            operationId,
            new JnasmartcardioTransceiver(),
            new ClientCallback() {
                void success() {
                    latch.countDown()
                }
                void failure(String message) {
                    throw new GradleException("Writing to fidesmo card failed with: '${message}'")
                }
            },
            RetrofitSecClient.getClient()
        )

        client.transceive()

        if (! latch.await(cardTimeout, TimeUnit.SECONDS)) {
            throw new GradleException('Time out while writing to fidesmo card')
        }
    }

    void apply(Project project) {

        if (!project.plugins.hasPlugin(JavacardPlugin)) {
            project.plugins.apply(JavacardPlugin)
        }

        def jcExtension = project.getExtensions().findByType(JavacardExtension)
        jcExtension.metaClass.getFidesmoPrefix = {
            String serviceProviderAidSuffix = getFidesmoAppId(project).padLeft(10, '0')
            FIDESMO_RID + ':' + serviceProviderAidSuffix.collect{ it }.collate(2).collect{ "0x${it.join()}" }.join(':')
        }

        project.tasks.create("uploadExecutableLoadFile") {
            group = 'publish'
            description = 'Uploads the java card applet to the fidesmo executable load file storage for later installation'
            dependsOn(project.convertJavacard)

            doLast {
                def response = getFidesmoService(project).uploadExecutableLoadFile(
                    new TypedFile('application/octet-stream', project.convertJavacard.getCapFile()))
            }
        }

        project.tasks.create('deleteFromLocalCard') {
            group = 'publish'
            description = 'Deletes the executable load file from the fidesm card via a locally attached card reader'

            doLast {
                def ccmDelete = new CcmDelete()
                // TODO: should be inputs of the task
                ccmDelete.application = jcExtension.cap.applets.first().aid.hexString

                def response = getFidesmoService(project).deleteExecutableLoadFile('http://fidesmo.com/dummyCallback', ccmDelete)
                executeOperation(response.operationId)
            }
        }

        project.tasks.create('installToLocalCard') {
            group = 'publish'
            description = 'Installs the executable load file to fidesmo card via a locally attached card reader'
            dependsOn(project.uploadExecutableLoadFile)
            dependsOn(project.deleteFromLocalCard)

            doLast {
                def ccmInstall = new CcmInstall()
                // TODO: should be inputs of the task
                ccmInstall.executableLoadFile = jcExtension.cap.aid.hexString
                ccmInstall.executableModule = jcExtension.cap.applets.first().aid.hexString
                ccmInstall.application = jcExtension.cap.applets.first().aid.hexString
                ccmInstall.parameters = ''

                def response = getFidesmoService(project).installExecutableLoadFile('http://fidesmo.com/dummyCallback', ccmInstall)
                executeOperation(response.operationId)
            }
        }
    }
}

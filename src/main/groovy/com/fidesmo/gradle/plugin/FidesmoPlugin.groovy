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

import com.fidesmo.sec.client.Client
import com.fidesmo.sec.client.ClientCallback
import com.fidesmo.sec.transceivers.AbstractTransceiver

import retrofit.*
import retrofit.mime.TypedFile
import retrofit.RequestInterceptor.RequestFacade

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import javax.smartcardio.*

import com.fidesmo.gradle.plugin.models.*

class FidesmoPlugin implements Plugin<Project> {

    public static final String FIDESMO_RID = '0xa0:0x00:0x00:0x06:0x17'
    public static final String FIDESMO_APP_ID = 'fidesmoAppId'
    public static final String FIDESMO_APP_KEY = 'fidesmoAppKey'

    private String getPropertieOrRead(Project project, String key, String msg) {
        if (project.hasProperty(key)) {
            project.property(key)
        } else {
            val value = System.console().readLine(msg)
            project.properties[key] = value
            return value
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
                       // work-around for http://jira.codehaus.org/browse/GROOVY-6885
                       // should be FidesmoPlugin.this instead or maybe directly this
                       def fidesmoPlugin = project.plugins.findPlugin(FidesmoPlugin)
                       request.addHeader('app_id', fidesmoPlugin.getFidesmoAppId(project))
                       request.addHeader('app_key', fidesmoPlugin.getFidesmoAppKey(project))
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
                               new GradleException("The server reject the operation with ${errorMessage}", cause)
                           } catch(any) {
                               cause
                           }
                       }
                   }
               })
           .build();

        restAdapter.create(FidesmoService.class)
    }


    void apply(Project project) {

        if (!project.plugins.hasPlugin(JavacardPlugin)) {
            project.plugins.apply(JavacardPlugin)
        }

        project.getExtensions().findByType(JavacardExtension).metaClass.withFidesmoPrefix = { suffix ->
            String appId = getFidesmoAppId(project)
            String appIdSegment = [ appId[0..1], appId[2..3], appId[4..5], appId[6..7] ].collect { nibble ->
                "0x${nibble}" }.join(':')


            if (suffix && suffix.length() > 0 ) {
                "${FIDESMO_RID}:0x00:${appIdSegment}:${suffix}"
            } else {
                "${FIDESMO_RID}:0x00:${appIdSegment}"
            }
        }

        project.tasks.create("uploadExecutableLoadFile") {
            group = 'publish'
            description = 'Uploads the java card applet to the fidesmo executable load file storage for later installation'
            dependsOn(project.convertJavacard)

            doLast {
                def response = getFidesmoService(project).uploadExecutableLoadFile(
                    new TypedFile('application/octet-stream', project.convertJavacard.getCapFile()))

                // TODO: assert values
                println(response.executableLoadFile)
                println(response.executableModules)
            }
        }

        project.tasks.create("installToLocalCard") {
            group = 'publish'
            description = 'Installs the executable load file to fidesmo card via a locally attached card reader'
            dependsOn(project.uploadExecutableLoadFile)

            def cardTimeout = 10


            doLast {

                // TODO: make configurable
                def ccmInstall = new CcmInstall()
                ccmInstall.executableLoadFile = 'a000000617009bc07ddb01'
                ccmInstall.executableModule = 'a000000617009bc07ddb0101'
                ccmInstall.application = 'a000000617009bc07ddb0101'
                ccmInstall.parameters = ''

                def response = getFidesmoService(project).installExecutableLoadFile('http://fidesmo.com/dummyCallback', ccmInstall)
                println(response.operationId)

                def latch = new CountDownLatch(1)
                def client = Client.getInstance(
                    response.operationId,
                    new AbstractTransceiver() {
                        Card card
                        public byte[] open() {
                            TerminalFactory factory = TerminalFactory.default
                            CardTerminal terminal = factory.terminals().list(CardTerminals.State.CARD_PRESENT).first()
                            card = terminal.connect("*")
                            card.ATR.bytes
                        }
                        public void close() {
                            card.disconnect(false)
                        }
                        public byte[] transceive(byte[] command) {
                            CardChannel cardChannel = card.basicChannel
                            ResponseAPDU responseApdu = cardChannel.transmit(new CommandAPDU(command))
                            responseApdu.bytes
                        }
                    },
                    new ClientCallback() {
                        void success() {
                            latch.countDown()
                        }
                        void failure(String message) {
                            throw new GradleException("Writing to fidesmo card failed with: ${message}")
                        }
                    })

                client.transceive()
                if (! latch.await(cardTimeout, TimeUnit.SECONDS)) {
                    throw new GradleException("Time out while writing to fidesmo card")
                }
            }
        }
    }
}

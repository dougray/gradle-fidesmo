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

import retrofit.*
import retrofit.mime.TypedFile
import retrofit.RequestInterceptor.RequestFacade

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

        project.getTasks().create("uploadExecutableLoadFile") {

            group = 'publish'
            description = 'Uploads the java card applet to the fidesmo executable load file storage for later installation'
            dependsOn(project.convertJavacard)

            doLast {
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
                                   new GradleException("An network related error occured while uploading the cap file", cause)
                               } else {
                                   cause
                               }
                           }
                       })
                   .build();
                
                def fidesmoService = restAdapter.create(FidesmoService.class);
                def response = fidesmoService.uploadExecutableLoadFile(
                    new TypedFile('application/octet-stream', project.convertJavacard.getCapFile()))

                println(response.body.in().text)
            }
        }
    }
}

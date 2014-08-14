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

import com.fidesmo.gradle.javacard.JavacardPlugin
import com.fidesmo.gradle.javacard.JavacardExtension

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

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

                def http = new HTTPBuilder('https://api.fidesmo.com')

                http.request(POST, JSON) {
                    uri.path = '/executableLoadFiles'

                    headers.app_id = getFidesmoAppId(project)
                    headers.app_key = getFidesmoAppKey(project)

                    requestContentType = BINARY
                    body = project.convertJavacard.getCapFile().bytes

                    response.success = { resp, json ->
                        logger.info('Executable load file was uploaded succesfull and is available as' +
                                    "aid: ${json.packageAid}")
                    }

                    response.failure = { resp ->
                        logger.error('Executable load file could not be stored server responded' +
                                     "'${resp.statusLine}' caused by '${resp.entity.content.text}'")
                    }
                }
            }
        }
    }
}

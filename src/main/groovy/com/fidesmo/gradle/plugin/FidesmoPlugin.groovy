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

import retrofit.mime.TypedFile

import java.util.UUID

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.fidesmo.gradle.plugin.models.*

class FidesmoPlugin implements Plugin<Project> {

    Logger logger = LoggerFactory.getLogger(this.getClass())

    public static final String FIDESMO_RID = '0xa0:0x00:0x00:0x06:0x17'
    public static final String FIDESMO_APP_ID = 'fidesmoAppId'
    public static final String FIDESMO_APP_KEY = 'fidesmoAppKey'

    void apply(Project project) {

        if (!project.plugins.hasPlugin(JavacardPlugin)) {
            project.plugins.apply(JavacardPlugin)
        }

        def jcExtension = project.extensions.findByType(JavacardExtension)
        jcExtension.metaClass.getFidesmoPrefix = {
            String fidesmoAppId = FidesmoBaseTask.getPropertieOrRead(project, FIDESMO_APP_ID, "\nPlease specify fidesmo app id: ")
            String serviceProviderAidSuffix = fidesmoAppId.padLeft(10, '0')
            FIDESMO_RID + ':' + serviceProviderAidSuffix.collect{ it }.collate(2).collect{ "0x${it.join()}" }.join(':')
        }

        project.tasks.create("uploadExecutableLoadFile", OperationTask) {
            group = 'publish'
            description = 'Uploads the java card applet to the fidesmo executable load file storage for later installation'
            dependsOn(project.convertJavacard)

            doLast {
                def response = fidesmoService.uploadExecutableLoadFile(
                    new TypedFile('application/octet-stream', project.convertJavacard.getCapFile()))
            }
        }

        project.tasks.create('deleteFromLocalCard', OperationTask) {
            group = 'publish'
            description = 'Deletes the executable load file from the fidesm card via a locally attached card reader'

            doLast {
                // TODO: should be inputs of the task
                def ccmDelete = new CcmDelete(application: jcExtension.cap.applets.first().aid.hexString)

                def response = fidesmoService.deleteExecutableLoadFile('https://api.fidesmo.com/status', ccmDelete)
                executeOperation(response.operationId)
            }
        }

        project.tasks.create('installToLocalCard', OperationTask) {
            group = 'publish'
            description = 'Installs the executable load file to fidesmo card via a locally attached card reader'
            dependsOn(project.uploadExecutableLoadFile)
            dependsOn(project.deleteFromLocalCard)

            doLast {
                // TODO: should be inputs of the task
                def ccmInstall = new CcmInstall(executableLoadFile: jcExtension.cap.aid.hexString,
                                                executableModule: jcExtension.cap.applets.first().aid.hexString,
                                                application: jcExtension.cap.applets.first().aid.hexString,
                                                parameters: '')

                def response = fidesmoService.installExecutableLoadFile('https://api.fidesmo.com/status', ccmInstall)
                executeOperation(response.operationId)
            }
        }
    }
}

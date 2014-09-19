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

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import groovy.transform.Memoized

abstract class FidesmoBaseTask extends DefaultTask {

    @Memoized
    private static String getPropertieOrRead(Project project, String key, String msg) {
        if (project.hasProperty(key)) {
            project.property(key)
        } else {
            System.console().readLine(msg)
        }
    }

    protected static String getFidesmoAppId(project) {
        def extension = project.extensions.findByType(FidesmoExtension)
        if (extension?.appId) {
            extension.appId
        } else {
            getPropertieOrRead(project, FidesmoPlugin.FIDESMO_APP_ID, "\nPlease specify fidesmo app id: ")
        }
    }

    String getFidesmoAppId() {
        getFidesmoAppId(project)
    }

    String getFidesmoAppKey() {
        getPropertieOrRead(project, FidesmoPlugin.FIDESMO_APP_KEY, "\nPlease specify fidesmo app key: ")
    }
}

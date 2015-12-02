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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

import retrofit.*
import retrofit.RequestInterceptor.RequestFacade

import com.fidesmo.sec.client.RetrofitSecClient
import com.fidesmo.sec.client.OperationClientImpl
import com.fidesmo.sec.client.ClientCallback
import com.fidesmo.sec.transceivers.Transceiver

class OperationTask extends FidesmoBaseTask {

    int getOperationTimeout() {
        def extension = project.extensions.findByType(FidesmoExtension).operationTimeout
    }

    def executeOperation(UUID operationId) {

        /* semantically both options differ. On android after return
         * the operation has been started, locally the last apdu was
         * already send.
         */
        if (project.hasProperty('fidesmo.adb_reader')) {
            // call activity (and therefore sec-client) on android phone
            def android_home = System.env.ANDROID_HOME
            def cmd = "${android_home}/platform-tools/adb shell " +
                "am start -W " +
                "--es descriptionExtra 'Run `${name}` task' " +
                "--es operationIdExtra '${operationId}' " +
                "com.fidesmo.sec.android/.ui.OperationActivity_"
            def proc = cmd.execute()
            proc.waitFor()
        } else {
            // execute operation by implementing sec-client flow
            def client = (new OperationClientImpl()).get(
                operationId,
                (Transceiver) new SmartcardioTransceiver(),
                RetrofitSecClient.client)
            client.transceive().toBlocking().last()
        }

        // check operation result by querying the status service
        int maxRetries = 30
        for(int i = 0; i <= maxRetries; i ++) { // try ten times
            try {
                def response = fidesmoService.getStatus(operationId)
                if (response.statusCode != 200) {
                    throw new GradleException("Operation ${operationId} failed with ${response.statusCode}")
                }
                return
            } catch (RetrofitError retrofitError){
                if (i == maxRetries || retrofitError?.response?.status != 404) {
                    throw new Exception("Unable to determine out come of operation", retrofitError)
                } else {
                    sleep(1000)
                    logger.info("Failed to fetch operation result for ${operationId} retrying ${i+1}/${maxRetries}")
                }
            }
        }
    }


    def getFidesmoService() {
        def restAdapter = new RestAdapter.Builder()
           .setEndpoint('https://api.fidesmo.com')
           .setRequestInterceptor(
               new RequestInterceptor(){
                   void intercept(RequestFacade request) {
                       request.addHeader('app_id', getFidesmoAppId())
                       request.addHeader('app_key', getFidesmoAppKey())
                   }
               })
           .setErrorHandler(
               new ErrorHandler(){
                   Throwable handleError(RetrofitError cause) {
                       if (cause.isNetworkError()) {
                           new GradleException('An network related error occured while uploading the cap file', cause)
                       } else {
                           try {
                               if (cause.response.status >= 500) {
                                   def errorMessage = cause.response.body.in().text
                                   new GradleException("The fidemo server aborted the operation with '${errorMessage}'", cause)
                               } else {
                                   cause
                               }
                           } catch (any) {
                               cause
                           }
                       }
                   }
               })
           .build();

        restAdapter.create(FidesmoService.class)
    }
}

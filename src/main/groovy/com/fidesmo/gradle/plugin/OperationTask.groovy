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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import retrofit.*
import retrofit.RequestInterceptor.RequestFacade

import com.fidesmo.sec.client.RetrofitSecClient
import com.fidesmo.sec.client.OperationClient
import com.fidesmo.sec.client.ClientCallback

class OperationTask extends FidesmoBaseTask {

    // TODO: add as input property of card interaction tasks (an abstract class that needs to be created)
    int cardTimeout = 10

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

        int maxRetries = 9
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
                    sleep(100)
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

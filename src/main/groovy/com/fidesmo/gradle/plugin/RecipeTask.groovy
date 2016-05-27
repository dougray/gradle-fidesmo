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

import com.fidesmo.sec.client.OperationClientImpl
import com.fidesmo.sec.delivery.ServiceDeliveryClient
import com.fidesmo.sec.client.RetrofitDeliverySecClient
import com.fidesmo.sec.client.HexTypeAdapter
import com.fidesmo.sec.client.TranslationsAdapter

import com.fidesmo.sec.models.*
import com.fidesmo.sec.local.CardInfoClient

import retrofit.*
import retrofit.mime.TypedByteArray
import retrofit.RequestInterceptor.RequestFacade
import retrofit.converter.GsonConverter;
import com.google.gson.*;

class RecipeTask extends FidesmoBaseTask {

    OperationClientImpl clientFactory = new OperationClientImpl()

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new HexTypeAdapter())
            .registerTypeAdapter(Translations.class, new TranslationsAdapter())
            .create()

    int getOperationTimeout() {
        def extension = project.extensions.findByType(FidesmoExtension).operationTimeout
    }

    def executeDeleteRecipe(String aid) {
        String serviceId = UUID.randomUUID().toString()

        try {
            Map<String, String> translations = new HashMap<>()
            translations.put("en", "Delete")
            ServiceDescription description = new ServiceDescription(new Translations(translations), null, null, null, null, null, null, null, null, null)

            DeleteRecipe recipe = new DeleteRecipe(description, aid)

            fidesmoService.uploadDeleteRecipe(getFidesmoAppId(), serviceId, recipe)

            ServiceDeliveryClient client = new ServiceDeliveryClient(RetrofitDeliverySecClient.getClient(), SmartCardIoCard.getCard(), clientFactory, 5)

            ServiceDeliveryResponse response = client.deliver(getServiceDeliveryRequired(description, serviceId)).toBlocking().last()

            FetchOperationResponse fetch = client.deliver(response).toBlocking().last()

            handleResponse(fetch)
        } finally {
            fidesmoService.deleteServiceRecipe(getFidesmoAppId(), serviceId)
        }
    }

    def executeInstallRecipe(String elf, String em, String aid, boolean encryptLoad) {
        String serviceId = UUID.randomUUID().toString()
        try {
            Map<String, String> translations = new HashMap<>()
            translations.put("en", "Delete")
            ServiceDescription description = new ServiceDescription(new Translations(translations), null, null, null, null, null, null, null, null, null)

            InstallRecipe recipe = new InstallRecipe(description, new InstallRecipe.InstallContent(elf, em, aid, encryptLoad))

            fidesmoService.uploadInstallRecipe(getFidesmoAppId(), serviceId, recipe)

            ServiceDeliveryClient client = new ServiceDeliveryClient(RetrofitDeliverySecClient.getClient(), SmartCardIoCard.getCard(), clientFactory, 5)

            ServiceDeliveryResponse response = client.deliver(getServiceDeliveryRequired(description, serviceId)).toBlocking().last()

            FetchOperationResponse fetch = client.deliver(response).toBlocking().last()

            handleResponse(fetch)
        } finally {
            fidesmoService.deleteServiceRecipe(getFidesmoAppId(), serviceId)
        }
    }

    def handleResponse(FetchOperationResponse fetch) {
        if(!fetch.completed) {
            throw new IOException("Failed executing all operations")
        } else {
            if(!fetch.status.success) {
                throw new IOException(fetch.status.message.toString())
            } else {
                logger.info(fetch.status.message.toString())
            }
        }
    }

    def getServiceDeliveryRequired(ServiceDescription description, String serviceId) {
        CardInfoClient cardClient = new CardInfoClient(SmartCardIoCard.getCard())
        CardInfo cardInfo = cardClient.getCardInfo()
        ServiceDeliveryRequired required = new ServiceDeliveryRequired(
            getFidesmoAppId(),
            serviceId,
            description,
            cardInfo.getCardId(),
            null,
            null,
            null,
            null,
            null,
            cardInfo.getCapabilities(),
            cardInfo.getAccountId(),
            cardInfo.getIsdAid(),
            null
        )
        return required
    }

    def getFidesmoService() {
        def restAdapter = new RestAdapter.Builder()
           .setEndpoint('https://api.fidesmo.com')
        .setConverter(new GsonConverter(gson))
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
                                   new GradleException("The fidesmo server aborted the operation with '${errorMessage}'", cause)
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

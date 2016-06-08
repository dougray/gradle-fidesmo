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


import retrofit.http.*
import retrofit.client.Response
import retrofit.mime.TypedFile
import retrofit.mime.TypedInput
import com.fidesmo.gradle.plugin.models.*

public interface FidesmoService {
    @POST('/executableLoadFiles')
    ElfReceiveSuccess uploadExecutableLoadFile(@Body TypedFile file)

    @PUT('/apps/{appId}/services/{serviceId}/recipe')
    Response uploadDeleteRecipe(@Path("appId") String appId, @Path("serviceId") String serviceId, @Body DeleteRecipe recipe)

    @PUT('/apps/{appId}/services/{serviceId}/recipe')
    Response uploadInstallRecipe(@Path("appId") String appId, @Path("serviceId") String serviceId, @Body InstallRecipe recipe)

    @DELETE('/apps/{appId}/services/{serviceId}/recipe')
    Response deleteServiceRecipe(@Path("appId") String appId, @Path("serviceId") String serviceId)
}

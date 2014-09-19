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
import com.fidesmo.gradle.plugin.models.*

public interface FidesmoService {
    @POST('/executableLoadFiles')
    ElfReceiveSuccess uploadExecutableLoadFile(@Body TypedFile file)

    @PUT('/ccm/install')
    OperationAccepted installExecutableLoadFile(@Header('callbackUrl') String callbackUrl, @Body CcmInstall install)

    @PUT('/ccm/delete')
    OperationAccepted deleteExecutableLoadFile(@Header('callbackUrl') String callbackUrl, @Body CcmDelete delete)

    @GET('/status/{operationId}')
    OperationResponse getStatus(@Path("operationId") UUID operationId)
}

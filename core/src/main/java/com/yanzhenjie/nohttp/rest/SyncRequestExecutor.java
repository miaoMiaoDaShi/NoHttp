/*
 * Copyright © Yan Zhenjie. All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yanzhenjie.nohttp.rest;

import com.yanzhenjie.nohttp.CoreConfig;

/**
 * <p>Request executor.</p>
 * Created by Yan Zhenjie on 2016/10/12.
 */
public enum SyncRequestExecutor {

    INSTANCE;

    private RestProtocol mRestProtocol;

    SyncRequestExecutor() {
        mRestProtocol = new RestProtocol(CoreConfig.getInstance().getCacheStore(),
                CoreConfig.getInstance().getNetworkExecutor());
    }

    /**
     * Perform a request.
     *
     * @param request {@link IProtocolRequest}.
     * @param <T>     Want to request to the data types.
     * @return {@link Response}.
     */
    public <T> Response<T> execute(IProtocolRequest<T> request) {
        request.start();
        Response<T> response = mRestProtocol.request(request);
        request.finish();
        return response;

    }
}

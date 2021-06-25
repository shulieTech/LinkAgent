/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch.action.admin.indices.warmer.put;

import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/26 6:07 下午
 */
public class PutWarmerRequest {

    public List<?> requests() {
        return null;
    }

    public String[] indices() {
        return null;
    }

    public final void indices(String... indices) {
    }

}

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
package com.rabbitmq.client.impl;

/**
 * Provider interface for establishing credentials for connecting to the broker. Especially useful
 * for situations where credentials might change before a recovery takes place or where it is
 * convenient to plug in an outside custom implementation.
 *
 * @since 4.5.0
 */
public interface CredentialsProvider {

    String getUsername();

    String getPassword();

}

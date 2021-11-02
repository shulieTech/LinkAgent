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
package com.pamirs.attach.plugin.dynamic.template;

import java.lang.annotation.*;

/**
 * @Auther: vernon
 * @Date: 2021/8/20 18:14
 * @Description:
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
public @interface Info {
    String describe() default "";

    /**
     * 是否可变字段
     *
     * @return
     */
    ModifierType modifier() default ModifierType.MODIFIABLE;

    enum ModifierType {
        MODIFIABLE,
        UNMODIFIABLE
    }
}

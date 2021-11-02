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
package com.pamirs.attach.plugin.dynamic;

import com.pamirs.attach.plugin.dynamic.resource.ConcurrentWeakHashMap;
import com.pamirs.attach.plugin.dynamic.resource.DestroyHook;
import com.pamirs.pradar.internal.PradarInternalService;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Auther: vernon
 * @Date: 2021/8/18 11:21
 * @Description: 公共资源存储器
 */
public class ResourceManager {


    private static Object target = new Object();


    public static void set(Attachment attachment) {
        if (PradarInternalService.isClusterTest()) {
            return;
        }
        Object index = attachment.index;
        if (index == null) {
            set(null, attachment);
        } else if (index instanceof List) {
            for (Object in : (List) index) {
                set(in.toString(), attachment);
            }
        } else {
            set(index.toString(), attachment);
        }
    }


    public static void set(String index, Attachment attachment) {
        if (PradarInternalService.isClusterTest()) {
            return;
        }


        if (index == null && (attachment.traceTypeList == null ||
                attachment.traceTypeList.length == 0)) {
            return;
        }
        if (index == null) {
            for (int i = 0; i < attachment.traceTypeList.length; i++) {
                String key = (attachment.traceTypeList[i]).toLowerCase();

                if (!hasDynamicField(target, key)) {
                    removeField(target, key);
                    setDynamicField(target, key, attachment);
                }
            }
            return;
        }

        for (int i = 0; i < attachment.traceTypeList.length; i++) {
            String key = (attachment.traceTypeList[i] + index).toLowerCase();

            if (!hasDynamicField(target, key)) {
                removeField(target, key);
                setDynamicField(target, key, attachment);
            }
        }
    }

    public static Object get(Object index, String traceType) {
        if (PradarInternalService.isClusterTest()) {
            return null;
        }

        if (index == null && traceType == null) {
            return null;
        }
        String key = null;
        if (index == null) {
            key = (traceType).toLowerCase();
        } else if (traceType == null) {
            key = index.toString().toLowerCase();
        } else {
            key = (traceType + index).toLowerCase();
        }

        return getDynamicField(target, key);
    }


    /**
     * 所有动态属性的集合,可能很多的模块根本不需要动态属性，所以这里使用延迟初始化
     * 防止浪费不必要的内存
     */
    private static ConcurrentWeakHashMap<Object, DynamicField> dynamicFields;

    /**
     * 当前所属的模块 ID, 留一个当前模块的属性，方便以后此处出问题的排查
     */
    private String moduleId;

    public ResourceManager(String moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * 延迟初始化
     */

    private static void lazyInit() {
        if (dynamicFields == null) {
            synchronized (target) {
                if (dynamicFields == null) {
                    dynamicFields = new ConcurrentWeakHashMap<Object, DynamicField>(new ConcurrentWeakHashMap.HashCodeGenerateFunction() {
                        @Override
                        public int hashcode(Object key) {
                            return System.identityHashCode(key);
                        }
                    }, new DestroyHook<Object, DynamicField>() {
                        @Override
                        public void destroy(Object key, DynamicField dynamicField) {
                            dynamicField.destroy();
                        }
                    });
                }
            }
        }
    }

    public static boolean hasDynamicField(Object target, String fieldName) {
        if (dynamicFields == null) {
            return false;
        }

        DynamicField dynamicField = dynamicFields.get(target);
        if (dynamicField == null) {
            return false;
        }
        return dynamicField.hasField(fieldName);
    }

    public static <T> T getDynamicField(Object target, String fieldName) {
        return getDynamicField(target, fieldName, null);
    }

    public static <T> T getDynamicField(Object target, String fieldName, T defaultValue) {
        if (dynamicFields == null) {
            return defaultValue;
        }
        DynamicField dynamicField = dynamicFields.get(target);
        if (dynamicField == null) {
            return defaultValue;
        }
        T value = dynamicField.getField(fieldName);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public void removeAll(Object target) {
        if (dynamicFields == null) {
            return;
        }
        DynamicField dynamicField = dynamicFields.remove(target);
        if (dynamicField != null) {
            dynamicField.destroy();
        }
    }

    static public void setDynamicField(Object target, String fieldName, Object value) {
        if (value == null || target == null || fieldName == null) {
            return;
        }
        lazyInit();
        DynamicField field = dynamicFields.get(target);
        if (field == null) {
            field = new DynamicField();
            DynamicField old = dynamicFields.putIfAbsent(target, field);
            if (old != null) {
                field = old;
            }
        }
        field.setField(fieldName, value);
    }

    public static <T> T removeField(Object target, String fieldName) {
        if (dynamicFields == null) {
            return null;
        }
        DynamicField field = dynamicFields.get(target);
        if (field == null) {
            return null;
        }

        return (T) field.removeField(fieldName);
    }

    public void destroy() {
        if (dynamicFields == null) {
            return;
        }
        Iterator<Map.Entry<Object, DynamicField>> it = dynamicFields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, DynamicField> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        dynamicFields.clear();
    }

    private static class DynamicField implements Closeable, Serializable {
        /**
         * 缓存的对应只有当 gc 回收之后才会空，所以可以根据这个来判断该对象是否可以回收
         */
        private ConcurrentHashMap<String, Object> fields = new ConcurrentHashMap<String, Object>(4, 1.0f, 4);

        public synchronized void setField(String field, Object value) {
            this.fields.put(field, value);
        }

        public <T> T getField(String field) {
            return (T) this.fields.get(field);
        }

        /**
         * 判断是否具有该属性的时候做一下优化，如果值已经不存在了，则直接删除该值
         *
         * @param field
         * @return
         */
        public boolean hasField(String field) {
            return this.fields.containsKey(field);
        }

        public synchronized Object removeField(String field) {
            return this.fields.remove(field);
        }

        public boolean isEmpty() {
            return fields.isEmpty();
        }

        /**
         * 销毁
         */
        public void destroy() {
            fields.clear();
            fields = null;
        }

        @Override
        public void close() {
            destroy();
        }
    }


}

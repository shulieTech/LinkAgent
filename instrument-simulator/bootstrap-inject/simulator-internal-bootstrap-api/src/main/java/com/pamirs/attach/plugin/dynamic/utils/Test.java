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
package com.pamirs.attach.plugin.dynamic.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Auther: vernon
 * @Date: 2021/8/23 03:20
 * @Description:注解解析工具类
 */
public class Test {

    public static Annotation getAnnotation(Object t,
                                           Class<? extends Annotation> annotation) {
        if (Field.class.isAssignableFrom(t.getClass())) {
            Field f = (Field) t;
            if (f.isAnnotationPresent(annotation)) {
                return f.getAnnotation(annotation);
            }
            return ((Field) t).getAnnotation(annotation);
        } else if (Method.class.isAssignableFrom(t.getClass())) {
            Method m = (Method) t;
            if (m.isAnnotationPresent(annotation)) {
                return m.getAnnotation(annotation);
            }
        } else {
            Class c = (Class) t;
            if (c.isAnnotationPresent(annotation)) {
                return c.getAnnotation(annotation);
            }
        }
        return null;
    }

    public static Field[] getFieldsWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        final List<Field> annotatedFieldsList = getFieldsListWithAnnotation(cls, annotationCls);
        return annotatedFieldsList.toArray(new Field[0]);
    }

    public static List<Field> getFieldsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        final List<Field> allFields = getAllFieldsList(cls);
        final List<Field> annotatedFields = new ArrayList<Field>();
        for (final Field field : allFields) {
            if (field.getAnnotation(annotationCls) != null) {
                annotatedFields.add(field);
            }
        }
        return annotatedFields;
    }

    public static List<Field> getAllFieldsList(final Class<?> cls) {
        final List<Field> allFields = new ArrayList<Field>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            Collections.addAll(allFields, declaredFields);
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

}

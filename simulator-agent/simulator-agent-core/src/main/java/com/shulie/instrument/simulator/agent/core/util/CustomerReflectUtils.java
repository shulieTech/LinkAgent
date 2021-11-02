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
package com.shulie.instrument.simulator.agent.core.util;

import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.FileNamePattern;

import java.lang.reflect.Field;

/**
 * @author angju
 * @date 2021/8/20 10:41
 */
public class CustomerReflectUtils {

    private static Field headTokenConverterField = null;
    public static Converter<Object> getHeadTokenConverter(FileNamePattern fileNamePattern) {
        if (headTokenConverterField == null){
            try {
                headTokenConverterField = fileNamePattern.getClass().getDeclaredField("headTokenConverter");
                headTokenConverterField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        try {
            Converter<Object> converter = (Converter<Object>)headTokenConverterField.get(fileNamePattern);
            return converter;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static CompressionMode getCompressionMode(Object object){
        Field field = null;
        try {
            field = object.getClass().getSuperclass().getDeclaredField("compressionMode");
            field.setAccessible(true);
            return (CompressionMode)field.get(object);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            //ignore
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            //ignore
        } finally {
            if (field != null){
                field.setAccessible(false);
            }
        }
        return null;
    }

    public static String getFileNamePatternStr(Object object){
        Field field = null;
        try {
            field = object.getClass().getSuperclass().getDeclaredField("fileNamePatternStr");
            field.setAccessible(true);
            return (String)field.get(object);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            //ignore
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            //ignore
        } finally {
            if (field != null){
                field.setAccessible(false);
            }
        }
        return null;
    }

    public static FileNamePattern getFileNamePattern(Object object){
        Field field = null;
        try {
            field = object.getClass().getSuperclass().getSuperclass().getDeclaredField("fileNamePattern");
            field.setAccessible(true);
            return (FileNamePattern)field.get(object);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            //ignore
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            //ignore
        } finally {
            if (field != null){
                field.setAccessible(false);
            }
        }
        return null;
    }


    public static void setFileNamePattern(FileNamePattern fileNamePattern, String fieldName, Object object){
        setFieldValue(fileNamePattern, fieldName, object);
    }


    public static void setFieldValue(Object value, String fieldName, Object object){
        Field field = null;
        try {
//            field = object.getClass().getSuperclass().getDeclaredField(fieldName);
            field = object.getClass().getSuperclass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException e) {
            //ignore
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            //ignore
            e.printStackTrace();
        } finally {
            if (field != null){
                field.setAccessible(false);
            }
        }
    }
}

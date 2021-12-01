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
package com.pamirs.attach.plugin.log4j.interceptor.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mocheng
 * @since 2021/09/18 09:38
 */
public class BeanUtil {

    static Logger logger = LoggerFactory.getLogger(BeanUtil.class);

    public static Object copyBean(Object origin) {
        return copyBean(origin, null);
    }

    /**
     * 对象copy
     * @param origin 原对象
     * @param paramsReplacedIndexAndValueMap 需要更换的参数
     * @return
     */
    public static Object copyBean(Object origin, Map<Integer, Object> paramsReplacedIndexAndValueMap) {
        Object dest = newInstance(origin, paramsReplacedIndexAndValueMap);
        copyProperties(dest, origin);
        return dest;
    }

    private static Object newInstance(Object origin, Map<Integer, Object> paramsReplacedIndexAndValueMap) {
        Object dest = null;
        if (origin != null) {
            Constructor<?>[] constructors = origin.getClass().getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                dest = newInstance(constructor, paramsReplacedIndexAndValueMap);
                if (dest != null) {
                    break;
                }
            }
        }
        return dest;
    }

    /**
     * 初始化对象
     * @param constructor 构造函数
     * @param paramsReplacedIndexAndValueMap
     * @return
     */
    private static Object newInstance(Constructor<?> constructor, Map<Integer, Object> paramsReplacedIndexAndValueMap) {
        Object object = null;
        if (constructor != null) {
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            try {
                if (parameterTypes.length > 0) {
                    Object[] parameters = new Object[parameterTypes.length];
                    injectFields(parameterTypes, parameters);
                    replaceParamsIfNeeded(paramsReplacedIndexAndValueMap, parameters);
                    object = constructor.newInstance(parameters);
                } else {
                    object = constructor.newInstance();
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    /**
     * 处理不能为空的属性
     * @param paramsReplacedIndexAndValueMap </><replacedParamIndex, replacedValue>
     *          更换 @parameters 中索引为 replacedParamIndex 位置的值为 replacedValue
     * @param parameters 参数列表
     */
    private static void replaceParamsIfNeeded(Map<Integer, Object> paramsReplacedIndexAndValueMap, Object[] parameters) {
        if (paramsReplacedIndexAndValueMap != null && paramsReplacedIndexAndValueMap.size() > 0) {
            for (Integer index : paramsReplacedIndexAndValueMap.keySet()) {
                parameters[index] = paramsReplacedIndexAndValueMap.get(index);
            }
        }
    }

    private static void injectFields(Class<?>[] parameterTypes, Object[] parameters) {
        if (parameterTypes != null && parameters != null && parameters.length == parameterTypes.length && parameters.length > 0) {
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = injectFieldsByType(parameterTypes[i]);
            }
        }
    }

    private static Object injectFieldsByType (Class<?> type) {
        if (type == String.class) {
            return "";
        } else if (type == int.class || type == Integer.class || type == byte.class || type == Byte.class) {
            return 0;
        } else if (type == float.class || type == Float.class) {
            return 0f;
        } else if (type == long.class || type == Long.class) {
            return 0L;
        } else if (type == double.class || type == Double.class) {
            return 0D;
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.FALSE;
        } else if (type == char.class || type == Character.class) {
            return '0';
        }
        // 其他类型
        return null;
    }

    public static void copyProperties(Object dest, Object orig) {
        if (dest != null && orig != null) {
            Class<?> clazz = orig.getClass();
            if (dest.getClass().getName().equals(clazz.getName())) {
                Field[] fields = getAllFields(clazz);
                for (Field field : fields) {
                    String name = field.getName();
                    try {
                        Reflect.on(dest).set(name, Reflect.on(orig).get(name));
                    } catch (Exception e) {
                       logger.warn(orig.getClass().getName() + " skip set value for field [{}], reason: {}", name, e.getMessage());
                    }

                }
            }
        }
    }

    /**
     * 获取所有属性对象
     * @param object
     * @return
     */
    public static Field[] getAllFields(Object object) {
        if (object != null) {
            return getAllFields(object.getClass());
        }
        return null;
    }

    /**
     * 获取所有属性对象
     * @param clazz
     * @return
     */
    public static Field[] getAllFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<Field>();  // 保存属性对象数组到列表
        while (clazz != null) {  // 遍历所有父类字节码对象
            Field[] declaredFields = clazz.getDeclaredFields();  // 获取字节码对象的属性对象数组
            fieldList.addAll(Arrays.asList(declaredFields));
            clazz = clazz.getSuperclass();  // 获得父类的字节码对象
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        return fields;
    }

    /**
     * 给 object 的 属性名为 fieldName 的属性，设置值为 fieldValue
     * 注：类型不匹配会报错
     * @param object
     * @param fieldName
     * @param fieldValue
     * @return
     */
    public static boolean set(Object object, String fieldName, Object fieldValue) {
        if (hasField(object, fieldName)) {
            Reflect.on(object).set(fieldName, fieldValue);
            return true;
        }
        logger.warn(object.getClass().getName() + "not contain field: " + fieldName);
        return false;
    }

    /**
     * 从 origin 中copy属性名为 fieldName 的属性值(若存在才复制)，到 ptObject 对象中
     * @param ptObject
     * @param fieldName
     * @param origin
     * @return
     */
    public static boolean setIfPresent(Object ptObject, String fieldName, Object origin) {
        if (hasField(ptObject, fieldName)) {
            Reflect.on(ptObject).set(fieldName, copyBean(Reflect.on(origin).get(fieldName)));
            return true;
        }
        logger.warn(ptObject.getClass().getName() + " not contain field: " + fieldName);
        return false;
    }

    /**
     * 从 origin 中copy多个属性值(若存在才复制)，到 ptObject 对象中
     * @param ptObject
     * @param origin
     * @param fieldNames
     */
    public static void setIfPresent(Object ptObject, Object origin, String... fieldNames) {
        if (fieldNames != null && fieldNames.length > 0) {
            // 有优化空间
            for (String fieldName : fieldNames) {
                setIfPresent(ptObject ,fieldName, origin);
            }
        }
    }

    /**
     * 判断 object 是否有名称为 fieldName 的属性
     * @param object
     * @param fieldName
     * @return
     */
    public static boolean hasField(Object object, String fieldName) {
        return hasField(object.getClass(), fieldName);
    }

    public static boolean hasField(Class<?> clazz, String fieldName) {
        Field[] fields = getAllFields(clazz);
        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }
}

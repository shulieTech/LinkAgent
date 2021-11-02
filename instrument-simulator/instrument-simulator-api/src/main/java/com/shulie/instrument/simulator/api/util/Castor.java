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
package com.shulie.instrument.simulator.api.util;

/**
 * Class utils
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/8/27 4:06 下午
 */
public class Castor {

    /**
     * value can cast to type
     *
     * @param type  target type
     * @param value target value
     * @throws ClassCastException
     */
    public static void canCast(Class type, Object value) throws ClassCastException {
        if (value == null) {
            return;
        }
        if (type.isPrimitive()) {
            if (value == null) {
                if (void.class != type) {
                    throw new ClassCastException("return type is not correct. required type is " + type.getName() + " but value is null.");
                }
                return;
            } else if (type == byte.class) {
                canCastByte(value);
            } else if (type == short.class) {
                canCastShort(value);
            } else if (type == int.class) {
                canCastInt(value);
            } else if (type == long.class) {
                canCastLong(value);
            } else if (type == float.class) {
                canCastFloat(value);
            } else if (type == double.class) {
                canCastDouble(value);
            } else if (type == char.class) {
                canCastChar(value);
            } else if (type == boolean.class) {
                canCastBoolean(value);
            }
            return;
        } else if (value == null) {
            return;
        } else if (type == Byte.class) {
            canCastWrapByte(value);
        } else if (type == Short.class) {
            canCastWrapShort(value);
        } else if (type == Integer.class) {
            canCastWrapInt(value);
        } else if (type == Long.class) {
            canCastWrapLong(value);
        } else if (type == Float.class) {
            canCastWrapFloat(value);
        } else if (type == Double.class) {
            canCastWrapDouble(value);
        } else if (type == Character.class) {
            canCastWrapChar(value);
        } else if (type == Boolean.class) {
            canCastWrapBoolean(value);
        } else {
            canCastObject(type, value);
        }
    }

    private static void canCastObject(Class type, Object value) {
        if (type.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is " + type.getName() + " but value is " + value.getClass().getName() + ".");
    }

    private static void canCastByte(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || Byte.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is byte but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapByte(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || Byte.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Byte but value is " + value.getClass().getName() + ".");
    }

    private static void canCastShort(Object value) {
        if (short.class.isAssignableFrom(value.getClass())
                || Short.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is short but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapShort(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || short.class.isAssignableFrom(value.getClass())
                || Short.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Short but value is " + value.getClass().getName() + ".");
    }

    private static void canCastInt(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || short.class.isAssignableFrom(value.getClass())
                || int.class.isAssignableFrom(value.getClass())
                || char.class.isAssignableFrom(value.getClass())
                || Integer.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is int but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapInt(Object value) {
        if (int.class.isAssignableFrom(value.getClass())
                || Integer.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Integer but value is " + value.getClass().getName() + ".");
    }

    private static void canCastLong(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || short.class.isAssignableFrom(value.getClass())
                || int.class.isAssignableFrom(value.getClass())
                || long.class.isAssignableFrom(value.getClass())
                || char.class.isAssignableFrom(value.getClass())
                || Long.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is long but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapLong(Object value) {
        if (long.class.isAssignableFrom(value.getClass())
                || Long.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Long but value is " + value.getClass().getName() + ".");
    }

    private static void canCastFloat(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || short.class.isAssignableFrom(value.getClass())
                || int.class.isAssignableFrom(value.getClass())
                || long.class.isAssignableFrom(value.getClass())
                || char.class.isAssignableFrom(value.getClass())
                || float.class.isAssignableFrom(value.getClass())
                || Float.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is float but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapFloat(Object value) {
        if (float.class.isAssignableFrom(value.getClass())
                || Float.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Float but value is " + value.getClass().getName() + ".");
    }

    private static void canCastDouble(Object value) {
        if (byte.class.isAssignableFrom(value.getClass())
                || short.class.isAssignableFrom(value.getClass())
                || int.class.isAssignableFrom(value.getClass())
                || long.class.isAssignableFrom(value.getClass())
                || char.class.isAssignableFrom(value.getClass())
                || float.class.isAssignableFrom(value.getClass())
                || double.class.isAssignableFrom(value.getClass())
                || Double.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is double but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapDouble(Object value) {
        if (double.class.isAssignableFrom(value.getClass())
                || Double.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Double but value is " + value.getClass().getName() + ".");
    }

    private static void canCastChar(Object value) {
        if (char.class.isAssignableFrom(value.getClass())
                || Character.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is char but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapChar(Object value) {
        if (char.class.isAssignableFrom(value.getClass())
                || Character.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Character but value is " + value.getClass().getName() + ".");
    }

    private static void canCastBoolean(Object value) {
        if (boolean.class.isAssignableFrom(value.getClass())
                || Boolean.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is boolean but value is " + value.getClass().getName() + ".");
    }

    private static void canCastWrapBoolean(Object value) {
        if (boolean.class.isAssignableFrom(value.getClass())
                || Boolean.class.isAssignableFrom(value.getClass())) {
            return;
        }
        throw new ClassCastException("return type is not correct. required type is Boolean but value is " + value.getClass().getName() + ".");
    }
}

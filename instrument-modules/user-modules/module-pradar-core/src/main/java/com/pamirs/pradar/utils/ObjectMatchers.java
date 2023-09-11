package com.pamirs.pradar.utils;

//import com.alibaba.fastjson2.JSONPath;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.util.StringUtil;

import java.util.regex.Pattern;

/**
 * 对象匹配器
 *
 * @author vincent
 * @date 2023/03/09 14:13
 **/
public class ObjectMatchers {
    public static ObjectMatcher getMatcher(String matcherType, String matcheResult, String pattern) {
        if ("direct".equals(matcherType)) {
            return new DirectObjectMatcher(matcheResult);
        } else if ("regular".equals(matcherType)) {
            return new PatternObjectMatcher(matcheResult, pattern);
        }/* else if ("jsonPath".equals(matcherType)) {
            return new JsonPathObjectMatcher(matcheResult, pattern);
        }*/ else if ("reflect".equals(matcherType)) {
            return new ReflectObjectMatcher(matcheResult, pattern);
        } else {
            throw new RuntimeException(
                    String.format("unrecognizable chain config type for : %s replaceValue:%s pattern:%s", matcherType,
                            matcheResult, pattern));
        }
    }

    public interface ObjectMatcher {
        String matcher(Object obj, String defaultValue);
    }

    private abstract static class AbstractObjectMatcher implements ObjectMatcher {

        protected final String matcheResult;

        private AbstractObjectMatcher(String matcheResult) {
            this.matcheResult = matcheResult;
        }

        @Override
        public String matcher(Object obj, String defaultValue) {
            return this.doMatcher(obj) ? matcheResult : defaultValue;
        }

        protected abstract boolean doMatcher(Object obj);
    }

    private static class PatternObjectMatcher extends AbstractObjectMatcher implements ObjectMatcher {

        private final Pattern pattern;

        private PatternObjectMatcher(String matcheResult, String pattern) {
            super(matcheResult);
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        protected boolean doMatcher(Object obj) {
            return pattern.matcher(String.valueOf(obj)).find();
        }

        @Override
        public String toString() {
            return "PatternChainMatcher{" +
                    "pattern=" + pattern.pattern() +
                    "} " + super.toString();
        }
    }
/*
    private static class JsonPathObjectMatcher extends AbstractObjectMatcher implements ObjectMatcher {

        private final JSONPath jsonPath;

        private JsonPathObjectMatcher(String matcheResult, String pattern) {
            super(matcheResult);
            this.jsonPath = JSONPath.compile(pattern);
        }

        @Override
        protected boolean doMatcher(Object obj) {
            return jsonPath.contains(obj);
        }

        @Override
        public String toString() {
            return "JsonPathChainMatcher{" +
                    "jsonPath=" + jsonPath.toString() +
                    "} " + super.toString();
        }
    }*/

    /**
     * 反射匹配器使用的表达式支持几种类型取值方式，
     * <pre>
     *     支持获取属性值 [].field
     *     支持调用外部方法 [].method()
     *     支持数组参数[0].field
     *     支持匹配，目前支持==
     *     例如，
     *     字符串匹配：[0].field == abc
     *     控制匹配: [0].field == null
     * </pre>
     * //方法@getOperationSucceeded()
     * //变量.operationSucceeded[0]
     * //[3]@getOperationSucceeded == null
     */
    protected static class ReflectObjectMatcher extends AbstractObjectMatcher implements ObjectMatcher {

        private int objectIndex = -1;
        private String callMethod;
        private String fieldName;

        private Object returnValue;

        private boolean equals = true;

        private int methodReturnValueIndex = -1;


        protected ReflectObjectMatcher(String matcheResult, String pattern) {
            super(matcheResult);
            parsePattern(pattern);
        }

        protected void parsePattern(String pattern) {
            if (StringUtil.isEmpty(pattern)) {
                return;
            }
            char[] chars = pattern.toCharArray();
            int index = 0;
            String objectIndexStr = getString(chars, index, '[', ']');
            if (!StringUtil.isEmpty(objectIndexStr)) {
                objectIndex = Integer.valueOf(objectIndexStr);
                index += objectIndexStr.length() + 2;
            } else {
                index += 2;
            }
            if (chars[index] == '.') {
                fieldName = getString(chars, index + 1, Character.MIN_VALUE, ' ');
                index = index + fieldName.length() + 2;
            } else if (chars[index] == '@') {
                callMethod = getString(chars, index, '@', '(');
                index = index + callMethod.length() + 2;
            }
            int notEqualIndex = findIndex(chars, index, '!');
            if (-1 != notEqualIndex) {
                if (chars[notEqualIndex + 1] == '=') {
                    index = findIndex(chars, notEqualIndex, ' ');
                    returnValue = trim(getLeastString(chars, index + 1));
                    equals = false;
                }
            } else {
                index = findIndex(chars, index, '=');
                if (chars[index + 1] == '=') {
                    index = findIndex(chars, index, ' ');
                    returnValue = trim(getLeastString(chars, index + 1));
                }
            }
            if (returnValue != null && "null".equals(returnValue)) {
                returnValue = null;
            }
            if (returnValue != null && "true".equals(returnValue)) {
                returnValue = true;
            }

            if (returnValue != null && "false".equals(returnValue)) {
                returnValue = false;
            }
        }

        private String trim(String str) {
            if (!StringUtil.isEmpty(str)) {
                return str.trim();
            }
            return str;
        }

        /**
         * 获取int值
         *
         * @param chars
         * @param start
         * @param end
         */
        protected String getString(char[] chars, Integer index, char start, char end) {
            int i = index;
            if (start != Character.MIN_VALUE) {
                i = findIndex(chars, i, start);
                if (-1 == i) {
                    return null;
                }
                i++;
            }

            StringBuffer sb = new StringBuffer();
            if (i > chars.length) {
                return null;
            }
            while (i < chars.length && chars[i] != end) {
                sb.append(chars[i]);
                i++;
            }
            if (i > chars.length || sb.length() == 0) {
                return null;
            }
            return sb.toString();
        }


        /**
         * 找到匹配字符位置 index为当前1位的前一位
         *
         * @param chars     字符数组
         * @param index     数组索引
         * @param charactor 匹配字符
         * @return 匹配字符位置
         */
        protected int findIndex(char[] chars, int index, char charactor) {
            int i = index;
            if (i > chars.length) {
                return -1;
            }
            while (i < chars.length && chars[i] != charactor) {
                i++;
            }
            if (i >= chars.length) {
                return -1;
            }
            return i;
        }


        protected String getLeastString(char[] chars, int index) {
            int i = index;
            if (i > chars.length) {
                return null;
            }
            char[] leastChars = new char[chars.length - index];
            System.arraycopy(chars, index, leastChars, 0, leastChars.length);
            return new String(leastChars);
        }


        @Override
        protected boolean doMatcher(Object obj) {
            Object matchObject = obj;
            if (obj == null) {
                return false;
            }
            matchObject = getArrayObject(obj, objectIndex);

            Object returnValue = null;
            if (!StringUtil.isEmpty(callMethod)) {
                returnValue = callMethod(matchObject, callMethod);
            } else if (!StringUtil.isEmpty(fieldName)) {
                returnValue = getField(matchObject, fieldName);
            } else {
                return false;
            }
            if (equals) {
                return equals(this.returnValue, returnValue);
            } else {
                return !equals(this.returnValue, returnValue);
            }
        }


        /**
         * <pre>
         * 获取对象，
         * 如果为数组中则获取某一位的对象
         * 如果数组索引为-1则直接返回对象
         * </pre>
         *
         * @param obj
         * @param index
         * @return
         */
        protected Object getArrayObject(Object obj, int index) {
            Object matchObject = obj;
            if (index == -1) {
                return matchObject;
            }

            if (obj instanceof Object[]) {
                Object[] objects = (Object[]) obj;
                if (index > objects.length) {
                    return null;
                }
                matchObject = objects[index];
            }
            //如果不为数组则直接返回对象本身
            else {
                return matchObject;
            }
            return matchObject;
        }

        /**
         * 获取field
         *
         * @param obj
         * @param fieldName
         * @return
         */
        private Object getField(Object obj, String fieldName) {
            if (StringUtil.isEmpty(fieldName)) {
                return null;
            }
            try {
                Object fieldValue = Reflect.on(obj).field(fieldName).get();
                return fieldValue;
            } catch (Exception e) {
                //ignore
            }
            return null;
        }


        /**
         * 执行方法
         *
         * @param obj
         * @param methodName
         * @return
         */
        private Object callMethod(Object obj, String methodName) {
            if (StringUtil.isEmpty(methodName)) {
                return null;
            }
            try {
                Object matchObject = Reflect.on(obj).call(methodName).get();
                return matchObject;
            } catch (Exception e) {
                //ignore
            }
            return null;
        }

        private boolean equals(final Object object1, final Object object2) {
            if (object1 == object2) {
                return true;
            }
            if (object1 == null || object2 == null) {
                return false;
            }
            return object1.equals(object2);
        }
    }

    private static class DirectObjectMatcher extends AbstractObjectMatcher implements ObjectMatcher {

        private DirectObjectMatcher(String matcheResult) {
            super(matcheResult);
        }

        @Override
        protected boolean doMatcher(Object obj) {
            return true;
        }
    }
}

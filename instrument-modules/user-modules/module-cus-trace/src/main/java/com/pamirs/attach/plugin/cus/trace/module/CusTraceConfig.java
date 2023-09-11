/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.cus.trace.module;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * mock 挡板的配置
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/24 6:15 下午
 */
public class CusTraceConfig implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 类全路径
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法参数的全路径
     * 如果为空，不判断参数；
     * 如果有多个实现，这类需要根据入参的对象进行查找；
     */
    private List<String> methodArgClasses;

    private boolean isRequestOn;
    private boolean isResponseOn;

    /**
     * 匹配表达式
     */
    private boolean resetContext = false;

    /**
     * 重置上下文表达式,表达式格式：
     * <pre>
     *     支持获取属性值 [].field
     *     支持调用外部方法 [].method()
     *     支持数组参数[0].field
     *     支持匹配，目前支持==
     *     例如，
     *     字符串匹配：[0].field == abc
     *     控制匹配: [0].field == null
     * </pre>
     */
    private String resetContextExpression;


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<String> getMethodArgClasses() {
        return methodArgClasses;
    }

    public boolean isRequestOn() {
        return isRequestOn;
    }

    public void setRequestOn(boolean requestOn) {
        isRequestOn = requestOn;
    }

    public boolean isResponseOn() {
        return isResponseOn;
    }

    public void setResponseOn(boolean responseOn) {
        isResponseOn = responseOn;
    }

    public void setMethodArgClasses(List<String> methodArgClasses) {
        this.methodArgClasses = methodArgClasses;
    }

    public String getKey() {
        if (methodArgClasses == null || methodArgClasses.isEmpty()) {
            return className + '#' + methodName;
        }
        return className + '#' + methodName + '(' + methodArgClasses.toString() + ")";
    }

    @Override
    public String toString() {
        return "CusTraceConfig{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodArgClasses=" + methodArgClasses +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CusTraceConfig that = (CusTraceConfig) o;
        return className.equals(that.className) && methodName.equals(that.methodName) && Objects.equals(methodArgClasses, that.methodArgClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, methodArgClasses);
    }

    public boolean isResetContext() {
        return resetContext;
    }

    public void setResetContext(boolean resetContext) {
        this.resetContext = resetContext;
    }

    public String getResetContextExpression() {
        return resetContextExpression;
    }

    public void setResetContextExpression(String resetContextExpression) {
        this.resetContextExpression = resetContextExpression;
    }
}

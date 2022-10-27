package com.pamirs.pradar.bean;

import java.lang.ref.WeakReference;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObjectData {
    private Object[] args;
    private Object returnObj;
    private Object otherData;
    private String method;

    private Class[] paramTypes;

    private WeakReference<Object> target;

    public SyncObjectData(Object target, String method, Object[] args, Class[] paramTypes, Object returnObj) {
        this.target = new WeakReference<Object>(target);
        this.args = args;
        this.returnObj = returnObj;
        this.method = method;
        this.paramTypes = paramTypes;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Class[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Object getTarget() {
        return target.get();
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public void setReturnObj(Object returnObj) {
        this.returnObj = returnObj;
    }

    public Object getOtherData() {
        return otherData;
    }

    public void setOtherData(Object otherData) {
        this.otherData = otherData;
    }
}

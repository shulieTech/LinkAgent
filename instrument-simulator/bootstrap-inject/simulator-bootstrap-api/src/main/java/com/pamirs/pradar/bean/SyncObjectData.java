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

    private Object target;
    private WeakReference<Object> reference;


    public SyncObjectData(Object target, String method, Object[] args, Class[] paramTypes, Object returnObj) {
        this(target, method, args, paramTypes, returnObj, true);
    }

    public SyncObjectData(Object target, String method, Object[] args, Class[] paramTypes, Object returnObj, boolean strongReference) {
        this.args = args;
        this.returnObj = returnObj;
        this.method = method;
        this.paramTypes = paramTypes;
        if (strongReference) {
            this.target = target;
        } else {
            this.reference = new WeakReference<Object>(target);
        }
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
        return target != null ? target : reference.get();
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

    @Override
    public int hashCode() {
        return target != null ? target.hashCode() : reference.get().hashCode();
    }

    @Override
    public boolean equals(Object otherData) {
        if (!(otherData instanceof SyncObjectData)) {
            return false;
        }
        SyncObjectData other = (SyncObjectData) otherData;
        if ((this.reference != null && other.reference == null) || (this.reference == null && other.reference != null)) {
            return false;
        }
        if (this.reference != null && other.reference != null && this.reference.get() != other.reference.get()) {
            return false;
        }
        if ((this.target != null && other.target == null) || (this.target == null && other.target != null)) {
            return false;
        }
        if (this.target != null && other.target != null && this.target != other.target) {
            return false;
        }
        if (!this.method.equals(other.method) || this.args.length != other.args.length) {
            return false;
        }
        return true;
    }
}

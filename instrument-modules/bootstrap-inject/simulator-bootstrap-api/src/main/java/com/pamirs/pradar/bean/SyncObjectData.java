package com.pamirs.pradar.bean;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObjectData {
    private Object[] args;
    private Object returnObj;
    private Object otherData;

    private Object target;

    public SyncObjectData(Object target, Object[] args, Object returnObj) {
        this.target = target;
        this.args = args;
        this.returnObj = returnObj;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
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

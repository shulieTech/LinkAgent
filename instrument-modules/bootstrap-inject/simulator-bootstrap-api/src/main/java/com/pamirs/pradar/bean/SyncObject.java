package com.pamirs.pradar.bean;

import java.util.*;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObject {
    //切初始化方法的应该至少次数非常少， 这里用数量来控制
    private static int maxNum = 500;
    private List<SyncObjectData> datas = new Vector<SyncObjectData>();

    public SyncObject addData(SyncObjectData data) {
        if (this.datas.size() > maxNum) {
            throw new RuntimeException("SyncObject is max num ! with " + data.getTarget().getClass());
        }
        this.datas.add(data);
        return this;
    }

    public SyncObject addData(List<SyncObjectData> datas) {
        if (this.datas.size() > maxNum) {
            throw new RuntimeException("SyncObject is max num ! with " + this.datas.get(0).getTarget().getClass());
        }
        this.datas.addAll(datas);
        return this;
    }

    public List<SyncObjectData> getDatas() {
        return new ArrayList<SyncObjectData>(datas);
    }

    public void setDatas(List<SyncObjectData> datas) {
        this.datas = datas;
    }
}

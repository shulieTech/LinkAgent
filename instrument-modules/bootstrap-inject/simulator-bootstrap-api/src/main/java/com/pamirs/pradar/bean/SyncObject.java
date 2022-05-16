package com.pamirs.pradar.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObject {
    //切初始化方法的应该至少次数非常少， 这里用数量来控制
    private static int maxNum = 500;
    private List<SyncObjectData> datas = new Vector<SyncObjectData>();

    public SyncObject addData(SyncObjectData data) {
        if (datas.size() > maxNum) {
            throw new RuntimeException("SyncObject is max num ! with " + data.getTarget().getClass());
        }
        datas.add(data);
        return this;
    }

    public SyncObject addData(List<SyncObjectData> datas) {
        if (datas.size() > maxNum) {
            throw new RuntimeException("SyncObject is max num ! with " + this.datas.get(0).getTarget().getClass());
        }
        datas.addAll(datas);
        return this;
    }

    public List<SyncObjectData> getDatas() {

        return datas;
    }

    public void setDatas(List<SyncObjectData> datas) {
        this.datas = datas;
    }
}

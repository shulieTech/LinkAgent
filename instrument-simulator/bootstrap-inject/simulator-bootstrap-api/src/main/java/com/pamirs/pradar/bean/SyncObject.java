package com.pamirs.pradar.bean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncObject {
    //切初始化方法的应该至少次数非常少， 这里用数量来控制
    private static int maxNum;
    private Set<SyncObjectData> datas = Collections.newSetFromMap(new ConcurrentHashMap());

    static {
        String syncObjectMaxNumVar = System.getProperty("sync.objects.max.num");
        if (syncObjectMaxNumVar != null) {
            maxNum = Integer.parseInt(syncObjectMaxNumVar);
        } else {
            maxNum = 500;
        }
    }

    public SyncObject addData(SyncObjectData data) {
        if (this.datas.size() > maxNum) {
            throw new RuntimeException("SyncObject is max num ! with " + data.getTarget().getClass());
        }
        this.datas.add(data);
        return this;
    }

    public SyncObject addData(Collection<SyncObjectData> dataList) {
        if (this.datas.size() > maxNum) {
            throw new RuntimeException("SyncObject is max num ! with " + dataList.iterator().next().getTarget().getClass());
        }
        this.datas.addAll(dataList);
        return this;
    }

    public Set<SyncObjectData> getDatas() {
        Iterator<SyncObjectData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            SyncObjectData next = iterator.next();
            if (next.getTarget() == null) {
                iterator.remove();
            }
        }
        return datas;
    }

    public void setDatas(Set<SyncObjectData> datas) {
        this.datas = datas;
    }
}

package com.pamirs.pradar.pressurement.entry;


import com.shulie.instrument.simulator.api.util.StringUtil;

/**
 * @Auther: vernon
 * @Date: 2022/1/11 15:44
 * @Description:
 */
public class CheckerEntry {


    private String url;
    private String table;
    private String operateType;
    private String prefix;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CheckerEntry)) {
            return false;
        }
        CheckerEntry entry = (CheckerEntry) o;
        return StringUtil.equals(url, entry.getUrl()) &&
                StringUtil.equals(table, entry.getTable()) &&
                StringUtil.equals(operateType, entry.getOperateType()) &&
                StringUtil.equals(prefix, entry.getPrefix());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getOperateType() {
        return operateType;
    }

    public void setOperateType(String operateType) {
        this.operateType = operateType;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}

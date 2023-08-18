package com.pamirs.pradar.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Date;

/**
 * 解决Gson序列化时间格式为数字而不是YYYY...时间格式
 */
public class FastjsonDateToGsonTypeAdapter extends TypeAdapter<Date> {

    @Override
    public void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getTime());
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        if (in != null) {
            return new Date(in.nextLong());
        } else {
            return null;
        }
    }
}


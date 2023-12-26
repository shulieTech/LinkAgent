package com.pamirs.pradar.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.util.Date;

public class GsonFactory {

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private static volatile Gson gson;

    static {
        GSON_BUILDER.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        GSON_BUILDER.registerTypeAdapter(Date.class, new FastjsonDateToGsonTypeAdapter());
        GSON_BUILDER.addDeserializationExclusionStrategy(new SuperclassExclusionStrategy());
        GSON_BUILDER.addSerializationExclusionStrategy(new SuperclassExclusionStrategy());
        gson = GSON_BUILDER.create();
    }

    /**
     * 获取Gson实例.
     *
     * @return Gson实例
     */
    public static Gson getGson() {
        return gson;
    }

}

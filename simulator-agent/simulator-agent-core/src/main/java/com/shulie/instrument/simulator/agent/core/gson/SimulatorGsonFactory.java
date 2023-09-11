package com.shulie.instrument.simulator.agent.core.gson;

import com.google.gson.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class SimulatorGsonFactory {

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private static volatile Gson gson;

    static {
        GSON_BUILDER.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        GSON_BUILDER.registerTypeAdapter(Date.class, new FastjsonDateToGsonTypeAdapter());
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

package com.shulie.instrument.simulator.agent.core.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class SimulatorGsonFactory {

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private static volatile Gson gson;

    static {
        GSON_BUILDER.registerTypeAdapter(Date.class, new FastjsonDateToGsonTypeAdapter());
        List<TypeAdapterFactory> factories = null;
        try {
            Field field = GsonBuilder.class.getDeclaredField("factories");
            field.setAccessible(true);
            factories = (List<TypeAdapterFactory>) field.get(GSON_BUILDER);
        } catch (Exception e) {
        }
        factories.add(new ObjectTypeAdapterFactory(Object.class, new DataTypeAdapter()));
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

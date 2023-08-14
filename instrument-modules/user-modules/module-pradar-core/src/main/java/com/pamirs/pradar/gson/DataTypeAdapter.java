package com.pamirs.pradar.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.GsonBuildConfig;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 解决输出json字符串时数字类型属性会带'.0'后缀问题，比如 int i =1 输出为：{"i":1.0}, 同时解决解析数字默认转换为Double类型问题
 */
public class DataTypeAdapter extends TypeAdapter<Object> {

    private Gson gson;

    private List<TypeAdapterFactory> factories;
    private final Map<TypeToken<?>, TypeAdapter<?>> typeTokenCache = new ConcurrentHashMap<TypeToken<?>, TypeAdapter<?>>();

    private static final TypeToken<?> NULL_KEY_SURROGATE = TypeToken.get(Object.class);

    private final ThreadLocal<Map<TypeToken<?>, FutureTypeAdapter<?>>> calls = new ThreadLocal<Map<TypeToken<?>, FutureTypeAdapter<?>>>();

    @Override
    public Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<Object>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return list;

            case BEGIN_OBJECT:
                Map<String, Object> map = new LinkedTreeMap<String, Object>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), read(in));
                }
                in.endObject();
                return map;

            case STRING:
                return in.nextString();

            case NUMBER:
                String object = in.nextString();
                if (object.contains(".")) {
                    return Double.valueOf(object);
                }
                return Double.valueOf(object).longValue();

            case BOOLEAN:
                return in.nextBoolean();

            case NULL:
                in.nextNull();
                return null;

            default:
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        if (gson == null) {
            gson = GsonFactory.getGson();
            factories = new ArrayList<>(ReflectionUtils.get(gson, "factories"));
            for (Iterator<TypeAdapterFactory> iterator = factories.iterator(); iterator.hasNext(); ) {
                TypeAdapterFactory factory = iterator.next();
                if (factory instanceof ObjectTypeAdapterFactory) {
                    iterator.remove();
                }
            }
        }

        TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) getAdapter(TypeToken.get(value.getClass()));
        if (typeAdapter instanceof ObjectTypeAdapter) {
            out.beginObject();
            out.endObject();
            return;
        }

        typeAdapter.write(out, value);
    }

    public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
        TypeAdapter<?> cached = typeTokenCache.get(type == null ? NULL_KEY_SURROGATE : type);
        if (cached != null) {
            return (TypeAdapter<T>) cached;
        }

        Map<TypeToken<?>, FutureTypeAdapter<?>> threadCalls = calls.get();
        boolean requiresThreadLocalCleanup = false;
        if (threadCalls == null) {
            threadCalls = new HashMap<TypeToken<?>, FutureTypeAdapter<?>>();
            calls.set(threadCalls);
            requiresThreadLocalCleanup = true;
        }

        // the key and value type parameters always agree
        FutureTypeAdapter<T> ongoingCall = (FutureTypeAdapter<T>) threadCalls.get(type);
        if (ongoingCall != null) {
            return ongoingCall;
        }

        try {
            FutureTypeAdapter<T> call = new FutureTypeAdapter<T>();
            threadCalls.put(type, call);

            for (TypeAdapterFactory factory : factories) {
                TypeAdapter<T> candidate = factory.create(gson, type);
                if (candidate != null) {
                    call.setDelegate(candidate);
                    typeTokenCache.put(type, candidate);
                    return candidate;
                }
            }
            throw new IllegalArgumentException("GSON (" + GsonBuildConfig.VERSION + ") cannot handle " + type);
        } finally {
            threadCalls.remove(type);

            if (requiresThreadLocalCleanup) {
                calls.remove();
            }
        }
    }

    static class FutureTypeAdapter<T> extends TypeAdapter<T> {
        private TypeAdapter<T> delegate;

        public void setDelegate(TypeAdapter<T> typeAdapter) {
            if (delegate != null) {
                throw new AssertionError();
            }
            delegate = typeAdapter;
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (delegate == null) {
                throw new IllegalStateException();
            }
            return delegate.read(in);
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (delegate == null) {
                throw new IllegalStateException();
            }
            delegate.write(out, value);
        }
    }
}

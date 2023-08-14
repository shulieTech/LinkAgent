package com.pamirs.pradar.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class ObjectTypeAdapterFactory implements TypeAdapterFactory {

    private final Class clazz;
    private final TypeAdapter typeAdapter;

    public ObjectTypeAdapterFactory(Class clazz, TypeAdapter typeAdapter) {
        this.clazz = clazz;
        this.typeAdapter = typeAdapter;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        final Class<? super T> requestedType = typeToken.getRawType();
        if (!clazz.isAssignableFrom(requestedType)) {
            return null;
        }
        return typeAdapter;
    }
}

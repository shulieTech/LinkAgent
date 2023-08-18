package com.pamirs.pradar.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class ObjectTypeAdapterFactory implements TypeAdapterFactory {

    private final TypeAdapter typeAdapter;

    public ObjectTypeAdapterFactory(TypeAdapter typeAdapter) {
        this.typeAdapter = typeAdapter;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        return typeToken.getRawType() == Object.class ? typeAdapter : null;
    }
}

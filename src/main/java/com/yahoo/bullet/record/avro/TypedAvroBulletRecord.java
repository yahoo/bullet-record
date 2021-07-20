/*
 *  Copyright 2020, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.record.avro;

import com.yahoo.bullet.record.BulletRecord;
import com.yahoo.bullet.record.TypedBulletRecord;
import com.yahoo.bullet.typesystem.Type;
import com.yahoo.bullet.typesystem.TypedObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of {@link BulletRecord} using Avro for serialization. It is a {@link TypedBulletRecord}.
 *
 * By default, after serialization the record deserializes lazily. It will only deserialize when one of
 * the get/set methods are called. This makes the object cheap to send through repeated read-write cycles
 * without modifications. You can force a read by either calling a get/set method.
 */
@Slf4j @AllArgsConstructor
public class TypedAvroBulletRecord extends TypedBulletRecord {
    private static final long serialVersionUID = -2200480102971008734L;

    protected Map<String, Type> types;
    protected LazyBulletAvro data;

    /**
     * Constructor.
     */
    public TypedAvroBulletRecord() {
        types = new HashMap<>();
        data = new LazyBulletAvro();
    }

    @Override
    protected TypedAvroBulletRecord rawSet(String field, TypedObject object) {
        Objects.requireNonNull(field);
        types.put(field, object.getType());
        data.set(field, object.getValue());
        return this;
    }

    @Override
    protected Map<String, Serializable> getRawDataMap() {
        data.forceReadData();
        return (Map) data.getData();
    }

    @Override
    public TypedObject get(String field) {
        return makeTypedObject(field, data.get(field));
    }

    @Override
    public boolean hasField(String field) {
        return data.hasField(field);
    }

    @Override
    public int fieldCount() {
        return data.fieldCount();
    }

    @Override
    public TypedObject getAndRemove(String field) {
        TypedObject object = makeTypedObject(field, data.getAndRemove(field));
        types.remove(field);
        return object;
    }

    @Override
    public TypedAvroBulletRecord remove(String field) {
        data.remove(field);
        types.remove(field);
        return this;
    }

    @Override
    public TypedAvroBulletRecord copy() {
        return new TypedAvroBulletRecord(new HashMap<>(types), data.copy());
    }

    @Override
    public Iterator<Map.Entry<String, TypedObject>> iterator() {
        return data.iterator(this::makeTypedObject);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TypedAvroBulletRecord)) {
            return false;
        }
        TypedAvroBulletRecord that = (TypedAvroBulletRecord) object;
        return Objects.equals(types, that.types) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        // Value doesn't matter when data is null
        return data == null ? 42 : data.hashCode();
    }

    private TypedObject makeTypedObject(Map.Entry<String, Object> entry) {
        return makeTypedObject(entry.getKey(), (Serializable) entry.getValue());
    }

    private TypedObject makeTypedObject(String key, Serializable value) {
        if (value == null) {
            return TypedObject.NULL;
        }
        return new TypedObject(types.getOrDefault(key, Type.UNKNOWN), value);
    }
}

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
import lombok.extern.slf4j.Slf4j;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An implementation of {@link BulletRecord} using Avro for serialization.
 *
 * By default, after serialization the record deserializes lazily. It will only deserialize when one of
 * the get/set methods are called. This makes the object cheap to send through repeated read-write cycles
 * without modifications. You can force a read by either calling a get/set method.
 */
@Slf4j
public class TypedAvroBulletRecord extends TypedBulletRecord {
    private static final long serialVersionUID = -2200480102971008734L;

    private Map<String, Type> types = new HashMap<>();
    private LazyBulletAvro data = new LazyBulletAvro();

    @Override
    protected TypedAvroBulletRecord rawSet(String field, TypedObject object) {
        types.put(field, object.getType());
        data.set(field, object);
        return this;
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
        TypedAvroBulletRecord copy = new TypedAvroBulletRecord();
        copy.types.putAll(this.types);
        copy.data = new LazyBulletAvro(this.data);
        return copy;
    }

    @Override
    public Iterator<Map.Entry<String, TypedObject>> iterator() {
        final Iterator<Map.Entry<String, Object>> iterator = data.iterator();
        return new Iterator<Map.Entry<String, TypedObject>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry<String, TypedObject> next() {
                Map.Entry<String, Object> field = iterator.next();
                String key = field.getKey();
                return new AbstractMap.SimpleEntry<>(key, makeTypedObject(key, field.getValue()));
            }
        };
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TypedAvroBulletRecord)) {
            return false;
        }
        TypedAvroBulletRecord that = (TypedAvroBulletRecord) object;
        return data == that.data  || data != null && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        // Value doesn't matter when data is null
        return data == null ? 42 : data.hashCode();
    }

    private TypedObject makeTypedObject(String field, Object value) {
        if (value == null) {
            return TypedObject.NULL;
        }
        return new TypedObject(types.getOrDefault(field, Type.UNKNOWN), value);
    }
}
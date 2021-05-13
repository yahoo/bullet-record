/*
 *  Copyright 2020, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.record.avro;

import com.yahoo.bullet.record.BulletRecord;
import com.yahoo.bullet.record.UntypedBulletRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of {@link BulletRecord} using Avro for serialization. It is an {@link UntypedAvroBulletRecord}.
 *
 * By default, after serialization the record deserializes lazily. It will only deserialize when one of
 * the get/set methods are called. This makes the object cheap to send through repeated read-write cycles
 * without modifications. You can force a read by either calling a get/set method.
 */
@Slf4j
public class UntypedAvroBulletRecord extends UntypedBulletRecord {
    private static final long serialVersionUID = 926415013785021742L;
    protected LazyBulletAvro data = new LazyBulletAvro();

    @Override
    protected UntypedAvroBulletRecord rawSet(String field, Serializable object) {
        Objects.requireNonNull(field);
        data.set(field, object);
        return this;
    }

    @Override
    public Serializable get(String field) {
        return data.get(field);
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
    public Serializable getAndRemove(String field) {
        return data.getAndRemove(field);
    }

    @Override
    public UntypedAvroBulletRecord remove(String field) {
        data.remove(field);
        return this;
    }

    @Override
    public UntypedAvroBulletRecord copy() {
        UntypedAvroBulletRecord copy = new UntypedAvroBulletRecord();
        copy.data = new LazyBulletAvro(this.data);
        return copy;
    }

    @Override
    public Iterator<Map.Entry<String, Serializable>> iterator() {
        return data.iterator();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UntypedAvroBulletRecord)) {
            return false;
        }
        UntypedAvroBulletRecord that = (UntypedAvroBulletRecord) object;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        // Value doesn't matter when data is null
        return data == null ? 42 : data.hashCode();
    }

    /**
     * Hook to replace the {@link LazyBulletAvro} with a subclass.
     *
     * @return The created {@link LazyBulletAvro}.
     */
    protected LazyBulletAvro getLazyBulletAvro() {
        return new LazyBulletAvro();
    }

    /**
     * Hook to copy the {@link LazyBulletAvro} and replace it with a subclass.
     *
     * @param other The {@link LazyBulletAvro} to copy.
     * @return The copied {@link LazyBulletAvro}.
     */
    protected LazyBulletAvro copyLazyBulletAvro(LazyBulletAvro other) {
        return new LazyBulletAvro(other);
    }
}

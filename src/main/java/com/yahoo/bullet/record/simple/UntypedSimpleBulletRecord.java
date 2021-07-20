/*
 *  Copyright 2018, Oath Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.record.simple;

import com.yahoo.bullet.record.UntypedBulletRecord;
import lombok.AccessLevel;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * A simple implementation of {@link UntypedBulletRecord}.
 */
public class UntypedSimpleBulletRecord extends UntypedBulletRecord {
    private static final long serialVersionUID = -4045166645513428587L;

    // Exposed for testing.
    @Setter(AccessLevel.PACKAGE)
    private Map<String, Serializable> data = new HashMap<>();

    @Override
    protected UntypedSimpleBulletRecord rawSet(String field, Serializable object) {
        Objects.requireNonNull(field);
        data.put(field, object);
        return this;
    }

    @Override
    protected Map<String, Serializable> getRawDataMap() {
        return data;
    }

    @Override
    protected Serializable convert(Object object) {
        return (Serializable) object;
    }

    @Override
    public Serializable get(String field) {
        return data.get(field);
    }

    @Override
    public boolean hasField(String field) {
        return data.containsKey(field);
    }

    @Override
    public int fieldCount() {
        return data.size();
    }

    @Override
    public Serializable getAndRemove(String field) {
        return data.remove(field);
    }

    @Override
    public UntypedSimpleBulletRecord remove(String field) {
        data.remove(field);
        return this;
    }

    @Override
    public UntypedSimpleBulletRecord copy() {
        UntypedSimpleBulletRecord copy = new UntypedSimpleBulletRecord();
        copy.data.putAll(this.data);
        return copy;
    }

    @Override
    public Iterator<Map.Entry<String, Serializable>> iterator() {
        return data.entrySet().iterator();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UntypedSimpleBulletRecord)) {
            return false;
        }
        UntypedSimpleBulletRecord that = (UntypedSimpleBulletRecord) object;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        // Value doesn't matter when data is null
        return data == null ? 42 : data.hashCode();
    }
}

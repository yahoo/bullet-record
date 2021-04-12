/*
 *  Copyright 2021, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.record;

import com.yahoo.bullet.typesystem.TypedObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO choose a name that is actually appropriate
/**
 * Only the topmost record is expected to be modifiable.
 */
public class LateralViewBulletRecord extends BulletRecord {
    private static final long serialVersionUID = 1756000447237973392L;

    private List<BulletRecord> records;

    public LateralViewBulletRecord(List<BulletRecord> records) {
        this.records = new ArrayList<>(records);
    }

    public LateralViewBulletRecord(BulletRecord... records) {
        this.records = Arrays.asList(records);
    }

    @Override
    protected Serializable convert(Object object) {
        return topmost().convert(object);
    }

    @Override
    protected BulletRecord rawSet(String field, Serializable object) {
        return topmost().rawSet(field, object);
    }

    @Override
    protected Map<String, Serializable> getRawDataMap() {
        Map<String, Serializable> data = new HashMap<>();
        records.forEach(record -> data.putAll(record.getRawDataMap()));
        return data;
    }

    @Override
    public Serializable get(String field) {
        for (int i = records.size() - 1; i >= 0; i--) {
            Serializable s = records.get(i).get(field);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    @Override
    public boolean hasField(String field) {
        for (int i = records.size() - 1; i >= 0; i--) {
            boolean hasField = records.get(i).hasField(field);
            if (hasField) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int fieldCount() {
        // Not accurate if records have overlapping field names
        return records.stream().mapToInt(BulletRecord::fieldCount).sum();
    }

    @Override
    public Serializable getAndRemove(String field) {
        // Only topmost record
        return topmost().getAndRemove(field);
    }

    @Override
    public BulletRecord remove(String field) {
        // Only topmost record
        return topmost().remove(field);
    }

    @Override
    public TypedObject typedGet(String field) {
        for (int i = records.size() - 1; i >= 0; i--) {
            TypedObject value = records.get(i).typedGet(field);
            if (!value.isNull()) {
                return value;
            }
        }
        return TypedObject.NULL;
    }

    @Override
    public BulletRecord copy() {
        return new LateralViewBulletRecord(records.stream().map(BulletRecord::copy).collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public Iterator iterator() {
        throw new UnsupportedOperationException();
    }

    private BulletRecord topmost() {
        return records.get(records.size() - 1);
    }
}

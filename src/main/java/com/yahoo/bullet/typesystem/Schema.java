/*
 *  Copyright 2020, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.typesystem;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Schema implements Serializable {
    private static final long serialVersionUID = 4384778745518403997L;
    private static final Field MISSING = new Field("", Type.NULL);

    private final LinkedHashMap<String, Field> fieldMap;

    @Getter @RequiredArgsConstructor
    public static class Field {
        @NonNull
        private final String name;
        @NonNull
        private Type type;

        /**
         * Copies this field.
         *
         * @return The copied {@link Field}.
         */
        public Field copy() {
            return new Field(name, type);
        }
    }

    // Utility field classes with additional metadata

    @Getter @RequiredArgsConstructor
    public static class SubField {
        @NonNull
        private final String name;
        @NonNull
        private String description;

        /**
         * Copies this sub-field.
         *
         * @return The copied {@link SubField}.
         */
        public SubField copy() {
            return new SubField(name, description);
        }
    }

    @Getter
    public static class DetailedField extends Field {
        @NonNull
        private String description;

        /**
         * Constructor.
         *
         * @param name The String name of the field.
         * @param type The {@link Type} of the field.
         * @param description A String description for this field.
         */
        public DetailedField(String name, Type type, String description) {
            super(name, type);
            this.description = description;
        }

        /**
         * Copies this detailed field.
         *
         * @return The copied {@link DetailedField}.
         */
        @Override
        public DetailedField copy() {
            return new DetailedField(getName(), getType(), description);
        }
    }

    @Getter
    public static class DetailedMapField extends DetailedField {
        private List<SubField> subFields;

        /**
         * Constructor.
         *
         * @param name The String name of the field.
         * @param type The {@link Type} of the field.
         * @param description A String description for this field.
         * @param subFields A non-null {@link List} of {@link SubField} enumerations for this field.
         */
        public DetailedMapField(String name, Type type, String description, List<SubField> subFields) {
            super(name, type, description);
            this.subFields = subFields;
        }

        /**
         * Copies this detailed map field.
         *
         * @return The copied {@link DetailedMapField}.
         */
        @Override
        public DetailedMapField copy() {
            return new DetailedMapField(getName(), getType(), getDescription(), copy(subFields));
        }

        /**
         * Copies the sub-fields.
         *
         * @param subFields The {@link List} of {@link SubField} to copy.
         * @return The copied sub-fields.
         */
        static List<SubField> copy(List<SubField> subFields) {
            return subFields == null ? null : subFields.stream().map(SubField::copy).collect(Collectors.toList());
        }
    }

    @Getter
    public static class DetailedMapMapField extends DetailedMapField {
        private List<SubField> subSubFields;

        /**
         * Constructor.
         *
         * @param name The String name of the field.
         * @param type The {@link Type} of the field.
         * @param description A String description for this field.
         * @param subFields A {@link List} of {@link SubField} enumerations for this map. Can be null.
         * @param subSubFields A {@link List} of {@link SubField} enumerations for each map in this map of maps.
         */
        public DetailedMapMapField(String name, Type type, String description, List<SubField> subFields, List<SubField> subSubFields) {
            super(name, type, description, subFields);
            this.subSubFields = subSubFields;
        }

        /**
         * Copies this detailed map of map field.
         *
         * @return The copied {@link DetailedMapMapField}.
         */
        @Override
        public DetailedMapMapField copy() {
            return new DetailedMapMapField(getName(), getType(), getDescription(), copy(getSubFields()), copy(subSubFields));
        }
    }

    /**
     * Creates a schema from the provided fields.
     *
     * @param fields The {@link List} of {@link Field} instances. The order is preserved.
     */
    public Schema(List<? extends Field> fields) {
        Objects.requireNonNull(fields);
        fieldMap = new LinkedHashMap<>();
        fields.forEach(f -> fieldMap.put(f.getName(), f));
    }

    /**
     * Finds the type for a given field name.
     *
     * @param field The name of the field.
     * @return The {@link Type} of the given field or {@link Type#NULL} if the field is not present.
     */
    public Type getType(String field) {
        return fieldMap.getOrDefault(field, MISSING).getType();
    }

    /**
     * Retrieves a field for a given field name.
     *
     * @param name The name of the field.
     * @return The {@link Field} for the given name or null if the field is not present.
     */
    public Field getField(String name) {
        return fieldMap.get(name);
    }

    /**
     * Checks to see if this given field exists.
     *
     * @param name The name of the field.
     * @return A boolean denoting if this field exists.
     */
    public boolean hasField(String name) {
        return fieldMap.containsKey(name);
    }

    /**
     * Removes a field for a given field name.
     *
     * @param field The name of the field.
     * @return This schema for chaining.
     */
    public Schema removeField(String field) {
        fieldMap.remove(field);
        return this;
    }

    /**
     * Adds a {@link Field} by name and type.
     *
     * @param name The name of the field.
     * @param type The {@link Type} of the field.
     * @return This schema for chaining.
     */
    public Schema addField(String name, Type type) {
        return addField(new Field(name, type));
    }

    /**
     * Adds a field.
     *
     * @param field The {@link Field} to add.
     * @return This schema for chaining.
     */
    public Schema addField(Field field) {
        fieldMap.put(field.getName(), field);
        return this;
    }

    /**
     * Changes the type of a field. The field must exist. This will preserve the order of the field in the schema as
     * well as other attributes, if it had any.
     *
     * @param name The name of the field.
     * @param type The non-null {@link Type} of the field.
     * @return This schema for chaining.
     */
    public Schema changeFieldType(String name, Type type) {
        Objects.requireNonNull(type);
        Field field = getField(name);
        field.type = type;
        return this;
    }

    /**
     * Changes the description of a field. The field must exist. This will preserve the order of the field in the
     * schema as well as other attributes, if it had any.
     *
     * @param <T> A {@link DetailedField} type.
     * @param klazz The class of the detailed field.
     * @param name The name of the field.
     * @param description The description to change.
     * @return This schema for chaining.
     */
    public <T extends DetailedField> Schema changeFieldDescription(Class<T> klazz, String name, String description) {
        DetailedField field = getAsDetailedField(DetailedField.class, name);
        field.description = description;
        return this;
    }

    /**
     * Changes the sub-field of a map field. This will preserve the order of the field in the schema as well as
     * other attributes, if it had any.
     *
     * @param <T> A {@link DetailedMapField} type.
     * @param klazz The class of the detailed field.
     * @param name The name of the field.
     * @param subFields The {@link List} of {@link SubField} for the field.
     * @return This schema for chaining.
     */
    public <T extends DetailedMapField> Schema changeSubFields(Class<T> klazz, String name, List<SubField> subFields) {
        DetailedMapField field = getAsDetailedField(DetailedMapField.class, name);
        field.subFields = subFields;
        return this;
    }

    /**
     * Changes the sub-fields of a map of map field. This will preserve the order of the field in the schema as well as
     * other attributes, if it had any.
     *
     * @param <T> A {@link DetailedMapMapField} type.
     * @param klazz The class of the detailed field.
     * @param name The name of the field.
     * @param subFields The {@link List} of {@link SubField} for the field.
     * @return This schema for chaining.
     */
    public <T extends DetailedMapMapField> Schema changeSubSubFields(Class<T> klazz, String name, List<SubField> subFields) {
        DetailedMapMapField field = getAsDetailedField(DetailedMapMapField.class, name);
        field.subSubFields = subFields;
        return this;
    }

    /**
     * Returns a deep copy of all the fields in schema.
     *
     * @return The copied {@link Schema}.
     */
    public Schema copy() {
        return new Schema(fieldMap.values().stream().map(Field::copy).collect(Collectors.toList()));
    }

    /**
     * Gets the list of fields in this schema. The order will be order provided initially to create the schema as well
     * as future additions to the schema.
     *
     * @return The {@link List} of {@link Field} stored in this schema.
     */
    public List<Field> getFields() {
        return new ArrayList<>(fieldMap.values());
    }

    /**
     * Gets all the {@link Type} stored in the schema.
     *
     * @return A {@link Set} of the various {@link Type} in the schema.
     */
    public Set<Type> getTypes() {
        return fieldMap.values().stream().map(Field::getType).collect(Collectors.toSet());
    }

    /**
     * Gets the list of detailed fields in this schema. The order will be order provided initially to create the schema
     * as well as future additions to the schema.
     *
     * @return The {@link List} of {@link DetailedField} stored in this schema.
     */
    public List<DetailedField> getDetailedFields() {
        return getFields(DetailedField.class);
    }

    /**
     * Gets the list of detailed map fields in this schema. The order will be order provided initially to create the
     * schema as well as future additions to the schema.
     *
     * @return The {@link List} of {@link DetailedMapField} stored in this schema.
     */
    public List<DetailedMapField> getDetailedMapFields() {
        return getFields(DetailedMapField.class);
    }

    /**
     * Gets the list of detailed map of map fields in this schema. The order will be order provided initially to create
     * the schema as well as future additions to the schema.
     *
     * @return The {@link List} of {@link DetailedMapMapField} stored in this schema.
     */
    public List<DetailedMapMapField> getDetailedMapMapFields() {
        return getFields(DetailedMapMapField.class);
    }

    private <T extends DetailedField> T getAsDetailedField(Class<T> klazz, String name) {
        Field field = getField(name);
        if (!klazz.isInstance(field)) {
            throw new UnsupportedOperationException(name + " is not a sub-type of " + klazz);
        }
        return klazz.cast(field);
    }

    private <T extends DetailedField> List<T> getFields(Class<T> klazz) {
        return fieldMap.values().stream().filter(klazz::isInstance).map(f -> (T) f).collect(Collectors.toList());
    }
}
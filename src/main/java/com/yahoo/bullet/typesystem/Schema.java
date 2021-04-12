/*
 *  Copyright 2020, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.typesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Schema implements Serializable {
    private static final long serialVersionUID = 4384778745518403997L;
    private static final Field MISSING = new PlainField("", Type.NULL);

    private final LinkedHashMap<String, Field> fieldMap;

    /**
     * Empty constructor.
     */
    public Schema() {
        this((List<Field>) null);
    }

    /**
     * Creates a schema using fields loaded from a JSON file.
     *
     * @param fileName The name of the resource or the path to the file containing the JSON fields to load.
     * @throws ValidationException if the fields are not valid.
     * @throws RuntimeException if the fields are not valid JSON.
     */
    public Schema(String fileName) throws ValidationException {
        this(Parser.parse(readFile(fileName)));
    }

    /**
     * Creates a schema from the provided fields.
     *
     * @param fields The {@link List} of {@link Field} instances. The order is preserved.
     * @throws ValidationException if the fields are not valid.
     */
    public Schema(List<? extends Field> fields) throws ValidationException {
        fieldMap = new LinkedHashMap<>();
        if (fields != null) {
            int i = 0;
            for (Field field : fields) {
                if (field == null) {
                    throw new ValidationException("Found a null field at position " + i + " in the list of fields. " +
                                                  "If parsed from JSON, this might have been an invalid field");
                }
                field.validate();
                fieldMap.put(field.getName(), field);
                i++;
            }
        }
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
        return addField(new PlainField(name, type));
    }

    /**
     * Adds a field after validation.
     *
     * @param field The {@link Field} to add. It will be validated.
     * @return This schema for chaining.
     */
    public Schema addField(Field field) {
        field.validate();
        fieldMap.put(field.getName(), field);
        return this;
    }

    /**
     * Changes the type of a field. The field must exist. This will preserve the order of the field in the schema as
     * well as other attributes, if it had any. It will also validate the field after.
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
     * schema as well as other attributes, if it had any. It will also validate the field after.
     *
     * @param <T> A {@link DetailedField} type.
     * @param name The name of the field.
     * @param description The description to change.
     * @return This schema for chaining.
     */
    public <T extends DetailedField> Schema changeFieldDescription(String name, String description) {
        DetailedField field = getAsDetailedField(DetailedField.class, name);
        field.description = description;
        field.validate();
        return this;
    }

    /**
     * Changes the sub-field of a map field. This will preserve the order of the field in the schema as well as
     * other attributes, if it had any.
     *
     * @param <T> A {@link DetailedMapField} type.
     * @param name The name of the field.
     * @param subFields The {@link List} of {@link SubField} for the field.
     * @return This schema for chaining.
     */
    public <T extends DetailedMapField> Schema changeSubFields(String name, List<SubField> subFields) {
        DetailedMapField field = getAsDetailedField(DetailedMapField.class, name);
        field.subFields = subFields;
        field.validate();
        return this;
    }

    /**
     * Changes the sub-fields of a map of map field. This will preserve the order of the field in the schema as well as
     * other attributes, if it had any.
     *
     * @param <T> A {@link DetailedMapMapField} type.
     * @param name The name of the field.
     * @param subFields The {@link List} of {@link SubField} for the field.
     * @return This schema for chaining.
     */
    public <T extends DetailedMapMapField> Schema changeSubSubFields(String name, List<SubField> subFields) {
        DetailedMapMapField field = getAsDetailedField(DetailedMapMapField.class, name);
        field.subSubFields = subFields;
        field.validate();
        return this;
    }

    /**
     * Changes the sub-fields of a list of map field. This will preserve the order of the field in the schema as well as
     * other attributes, if it had any.
     *
     * @param <T> A {@link DetailedMapListField} type.
     * @param name The name of the field.
     * @param subFields The {@link List} of {@link SubField} for the field.
     * @return This schema for chaining.
     */
    public <T extends DetailedMapListField> Schema changeSubListFields(String name, List<SubField> subFields) {
        DetailedMapListField field = getAsDetailedField(DetailedMapListField.class, name);
        field.subListFields = subFields;
        field.validate();
        return this;
    }

    /**
     * Returns the number of fields in this schema.
     *
     * @return An integer count of the number of fields defined in this schema.
     */
    public int size() {
        return fieldMap.size();
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

    /**
     * Gets the list of detailed list of map fields in this schema. The order will be order provided initially to create
     * the schema as well as future additions to the schema.
     *
     * @return The {@link List} of {@link DetailedMapMapField} stored in this schema.
     */
    public List<DetailedMapListField> getDetailedMapListFields() {
        return getFields(DetailedMapListField.class);
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

    private static Reader readFile(String file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            InputStream is = Schema.class.getResourceAsStream("/" + file);
            return is != null ? new InputStreamReader(is) : new FileReader(file);
        } catch (IOException ioe) {
            log.error("Unable to load schema file {}", file);
            log.error("Error: ", ioe);
            return null;
        }
    }

    /* ============================================================================================================== */
    /*                                          Static helper classes                                                 */
    /* ============================================================================================================== */

    public static class ValidationException extends RuntimeException {
        private static final long serialVersionUID = 1652975997878467083L;

        /**
         * Constructor.
         *
         * @param cause The String cause for the exception.
         */
        public ValidationException(String cause) {
            super(cause);
        }
    }

    @Getter @Setter
    public abstract static class Field {
        protected String name;
        protected Type type;

        private static final String NAME_FIELD = "name";
        private static final String TYPE_FIELD = "type";

        /**
         * Copies this field.
         *
         * @return The copied {@link Field}.
         */
        public abstract Field copy();

        /**
         * Checks to see if the field is valid.
         *
         * @throws ValidationException if the field is not valid.
         */
        public abstract void validate() throws ValidationException;

        /**
         * Checks if the given the String is null or empty.
         *
         * @param field The String to check.
         * @return A boolean denoting if the field is missing.
         */
        public static boolean isNotPresent(String field) {
            return field == null || field.isEmpty();
        }
    }

    @NoArgsConstructor
    public static class PlainField extends Field {
        /**
         * Constructor.
         *
         * @param name The String name of this field.
         * @param type The {@link Type} of this field.
         */
        public PlainField(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public PlainField copy() {
            return new PlainField(name, type);
        }

        /**
         * Checks to see if the field is valid.
         *
         * @throws ValidationException if the field is not valid.
         */
        @Override
        public void validate() throws ValidationException {
            checkFieldMembers();
        }

        /**
         * Checks to see if this field has valid members.
         *
         * @throws ValidationException if the field is not valid.
         */
        protected void checkFieldMembers() throws ValidationException {
            if (isNotPresent(name) || type == null) {
                throw new ValidationException("The name or the type must be provided for a field");
            }
        }
    }

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class SubField {
        private String name;
        private String description;

        /**
         * Copies this sub-field.
         *
         * @return The copied {@link SubField}.
         */
        public SubField copy() {
            return new SubField(name, description);
        }

        /**
         * Copies a {@link List} of sub-fields.
         *
         * @param subFields The {@link List} of {@link SubField} to copy.
         * @return The copied sub-fields.
         */
        static List<SubField> copy(List<SubField> subFields) {
            return subFields == null ? null : subFields.stream().map(SubField::copy).collect(Collectors.toList());
        }

        /**
         * Checks if the given the {@link List} of {@link SubField} is null or empty.
         *
         * @param fields The sub-fields to check.
         * @return A boolean denoting if the sub-fields are missing.
         */
        public static boolean isNotPresent(List<SubField> fields) {
            return fields == null || fields.isEmpty();
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class DetailedField extends PlainField {
        private String description;

        private static final String DESCRIPTION_FIELD = "description";

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

        @Override
        public DetailedField copy() {
            return new DetailedField(getName(), getType(), description);
        }

        @Override
        protected void checkFieldMembers() throws ValidationException {
            super.checkFieldMembers();
            if (isNotPresent(description)) {
                throw new ValidationException("The description must be provided for a detailed field. Use a regular field otherwise");
            }
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class DetailedMapField extends DetailedField {
        private List<SubField> subFields;

        private static final String SUBFIELDS_FIELD = "subFields";

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

        @Override
        public DetailedMapField copy() {
            return new DetailedMapField(getName(), getType(), getDescription(), SubField.copy(subFields));
        }

        @Override
        public void validate() throws ValidationException {
            super.validate();
            if (!Type.isMap(type)) {
                throw new ValidationException("A detailed map should have a type that is a map");
            }
            if (SubField.isNotPresent(subFields)) {
                throw new ValidationException("The subFields are not provided. It is not optional for detailed map field");
            }
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class DetailedMapMapField extends DetailedMapField {
        private List<SubField> subSubFields;

        private static final String SUBSUBFIELDS_FIELD = "subSubFields";

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

        @Override
        public DetailedMapMapField copy() {
            return new DetailedMapMapField(getName(), getType(), getDescription(), SubField.copy(getSubFields()), SubField.copy(subSubFields));
        }

        @Override
        public void validate() throws ValidationException {
            // Do not call parent's validate since that will check subFields. That can be null.
            // Instead call checkFieldMembers, which is not overridden in parent.
            checkFieldMembers();
            if (!Type.isComplexMap(type)) {
                throw new ValidationException("A detailed map of maps should have a type that is a map of map of primitives");
            }
            if (SubField.isNotPresent(subSubFields)) {
                throw new ValidationException("The subSubFields are not provided. It is not optional for a detailed map of map of primitives field");
            }
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class DetailedMapListField extends DetailedField {
        private List<SubField> subListFields;

        private static final String SUBLIST_FIELD = "subListFields";

        /**
         * Constructor.
         *
         * @param name The String name of the field.
         * @param type The {@link Type} of the field.
         * @param description A String description for this field.
         * @param subListFields A {@link List} of {@link SubField} enumerations for each map in this list of maps.
         */
        public DetailedMapListField(String name, Type type, String description, List<SubField> subListFields) {
            super(name, type, description);
            this.subListFields = subListFields;
        }

        @Override
        public DetailedMapListField copy() {
            return new DetailedMapListField(getName(), getType(), getDescription(), SubField.copy(subListFields));
        }

        @Override
        public void validate() throws ValidationException {
            checkFieldMembers();
            if (!Type.isComplexList(type)) {
                throw new ValidationException("A detailed list of map should have a type that is a list of maps of primitives");
            }
            if (SubField.isNotPresent(subListFields)) {
                throw new ValidationException("The subListFields are not provided. It is not optional for a detailed list of map of primitives field");
            }
        }
    }


    public static class Parser {
        private static final GenericTypeAdapterFactory<Field> FIELD_FACTORY =
                // Order matters! This is a line hierarchy so the bottom up
                GenericTypeAdapterFactory.of(Field.class)
                                         .registerSubType(DetailedMapListField.class, Parser::isDetailedMapListField)
                                         .registerSubType(DetailedMapMapField.class, Parser::isDetailedMapMapField)
                                         .registerSubType(DetailedMapField.class, Parser::isDetailedMapField)
                                         .registerSubType(DetailedField.class, Parser::isDetailedField)
                                         .registerSubType(PlainField.class, Parser::isPlainField);

        private static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(FIELD_FACTORY)
                                                          .setPrettyPrinting().serializeNulls().create();

        private static final Set<String> TYPES = Arrays.stream(Type.values()).map(Type::name).collect(Collectors.toSet());

        private static boolean isPlainField(JsonElement jsonElement) {
            if (!jsonElement.isJsonObject()) {
                return false;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement name = jsonObject.get(Field.NAME_FIELD);
            JsonElement type = jsonObject.get(Field.TYPE_FIELD);
            return name != null && !name.isJsonNull() && type != null && !type.isJsonNull() && TYPES.contains(type.getAsString());
        }

        private static boolean isDetailedField(JsonElement jsonElement) {
            return isPlainField(jsonElement) && jsonElement.getAsJsonObject().has(DetailedField.DESCRIPTION_FIELD);
        }

        private static boolean isDetailedMapField(JsonElement jsonElement) {
            // The SUBSUBFIELDS_FIELD check isn't needed since this is invoked in order.
            return isDetailedField(jsonElement) && jsonElement.getAsJsonObject().has(DetailedMapField.SUBFIELDS_FIELD);
        }

        private static boolean isDetailedMapMapField(JsonElement jsonElement) {
            return isDetailedField(jsonElement) && jsonElement.getAsJsonObject().has(DetailedMapMapField.SUBSUBFIELDS_FIELD);
        }

        private static boolean isDetailedMapListField(JsonElement jsonElement) {
            return isDetailedField(jsonElement) && jsonElement.getAsJsonObject().has(DetailedMapListField.SUBLIST_FIELD);
        }

        /**
         * Parses a JSON String into a {@link List} of {@link Field}.
         *
         * @param data The data to parse.
         * @return A {@link List} of {@link Field} instances.
         */
        public static List<Field> parse(String data) {
            return parse(new StringReader(data));
        }

        /**
         * Reads a {@link List} of {@link Field} from a {@link Reader}.
         *
         * @param reader The non-null reader to use.
         * @return A {@link List} of {@link Field} instances.
         */
        public static List<Field> parse(Reader reader) {
            Objects.requireNonNull(reader);
            return GSON.fromJson(reader, new TypeToken<List<Field>>() { }.getType());
        }
    }
}

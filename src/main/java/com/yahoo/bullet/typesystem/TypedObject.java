/*
 *  Copyright 2020, Yahoo Inc.
 *  Licensed under the terms of the Apache License, Version 2.0.
 *  See the LICENSE file associated with the project for terms.
 */
package com.yahoo.bullet.typesystem;

import lombok.Getter;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an object with its {@link Type}. Note that these objects must be {@link Serializable}.
 */
@Getter
public class TypedObject implements Comparable<TypedObject>, Serializable {
    private static final long serialVersionUID = -2162794063118775558L;

    private final Type type;
    private final Serializable value;

    /**
     * Represents the {@link Type#UNKNOWN} object. The value is null.
     */
    public static final TypedObject UNKNOWN = new TypedObject(Type.UNKNOWN, null);
    /**
     * Represents the {@link Type#NULL} object. The value is null.
     */
    public static final TypedObject NULL = new TypedObject(Type.NULL, null);
    /**
     * Represents the {@link Type#BOOLEAN} object for the true value.
     */
    public static final TypedObject TRUE = new TypedObject(Type.BOOLEAN, true);
    /**
     * Represents the {@link Type#BOOLEAN} object for the false value.
     */
    public static final TypedObject FALSE = new TypedObject(Type.BOOLEAN, false);

    /**
     * Constructor that wraps a {@link Serializable} object into a type. See {@link Type#getType(Object)} to see how the
     * type of the corresponding object will be determined.
     *
     * @param value The {@link Serializable} value that is being wrapped.
     */
    public TypedObject(Serializable value) {
        this(Type.getType(value), value);
    }

    /**
     * Create a TypedObject with the given non-null type. Note that the value is not validated to be of that type. If
     * it is not, all operation results are undefined. You should use {@link Type#cast(Serializable)} or
     * {@link Type#forceCast(Type, Serializable)} to force the value to the desired type if it is not.
     *
     * @param type The type of the value.
     * @param value The value being wrapped.
     */
    public TypedObject(Type type, Serializable value) {
        Objects.requireNonNull(type);
        this.type = type;
        this.value = value;
    }

    /**
     * Returns true if this does not have an actual type, i.e. type is {@link Type#UNKNOWN}.
     *
     * @return A boolean denoting if the type is {@link Type#UNKNOWN}.
     */
    public boolean isUnknown() {
        return Type.isUnknown(type);
    }

    /**
     * Returns true if the given type is the {@link Type#NULL} type.
     *
     * @return A boolean denoting if the type is {@link Type#NULL}.
     */
    public boolean isNull() {
        return Type.isNull(type);
    }

    /**
     * Returns true if the given type is in {@link Type#PRIMITIVES}.
     *
     * @return A boolean denoting if the type is primitive.
     */
    public boolean isPrimitive() {
        return Type.isPrimitive(type);
    }

    /**
     * Returns true if the type is in {@link Type#PRIMITIVE_MAPS}.
     *
     * @return A boolean denoting if the type is a primitive map.
     */
    public boolean isPrimitiveMap() {
        return Type.isPrimitiveMap(type);
    }

    /**
     * Returns true if the type is in {@link Type#COMPLEX_MAPS}.
     *
     * @return A boolean denoting if the type is a complex map.
     */
    public boolean isComplexMap() {
        return Type.isComplexMap(type);
    }

    /**
     * Returns true if the type is in {@link Type#MAPS}.
     *
     * @return A boolean denoting if the type is a map.
     */
    public boolean isMap() {
        return Type.isMap(type);
    }

    /**
     * Returns true if the type is in {@link Type#PRIMITIVE_LISTS}.
     *
     * @return A boolean denoting if the type is a primitive list.
     */
    public boolean isPrimitiveList() {
        return Type.isPrimitiveList(type);
    }

    /**
     * Returns true if the type is in {@link Type#COMPLEX_LISTS}.
     *
     * @return A boolean denoting if the type is a complex list.
     */
    public boolean isComplexList() {
        return Type.isComplexList(type);
    }

    /**
     * Returns true if the type is in {@link Type#LISTS}.
     *
     * @return A boolean denoting if the type is a list.
     */
    public boolean isList() {
        return Type.isList(type);
    }

    /**
     * Get the size of the value. Currently only {@link Type#LISTS}, {@link Type#MAPS} and {@link Type#STRING} support
     * getting a size.
     *
     * @return The size of the value.
     * @throws UnsupportedOperationException if not supported.
     */
    public int size() {
        if (isList()) {
            return ((List) value).size();
        } else if (isMap()) {
            return ((Map) value).size();
        } else if (type == Type.STRING) {
            return ((String) value).length();
        }
        throw new UnsupportedOperationException("This type does not support getting a size: " + type);
    }

    /**
     * Returns true if the value or its underlying values contain a mapping for the specified key. Only
     * {@link Type#COMPLEX_LISTS} and {@link Type#MAPS} support getting a mapping.
     *
     * @param key The key to be tested.
     * @return A Boolean to indicate if the value or its underlying values contain a mapping for the specified key.
     * @throws UnsupportedOperationException if not supported.
     */
    @SuppressWarnings("unchecked")
    public boolean containsKey(String key) {
        if (isComplexList()) {
            return ((List) value).stream().anyMatch(e -> ((Map) e).containsKey(key));
        } else if (isComplexMap()) {
            Map map = (Map) value;
            return map.containsKey(key) || map.values().stream().anyMatch(e -> ((Map) e).containsKey(key));
        } else if (isPrimitiveMap()) {
            Map map = (Map) value;
            return map.containsKey(key);
        }
        throw new UnsupportedOperationException("This type does not support mappings: " + type);
    }

    /**
     * Returns true if the value or its underlying values contain the specified value. Only LIST and MAP are supported.
     *
     * @param target The target {@link TypedObject} to be tested.
     * @return A Boolean to indicate if the value or its underlying values contain the specified value.
     * @throws UnsupportedOperationException if not supported.
     */
    @SuppressWarnings("unchecked")
    public boolean containsValue(TypedObject target) {
        if (isPrimitiveList()) {
            Type subType = type.getSubType();
            return ((List) value).stream().filter(Objects::nonNull).anyMatch(o -> target.equalTo(subType, o));
        } else if (isComplexList()) {
            Type subType = type.getSubType().getSubType();
            return ((List) value).stream().filter(Objects::nonNull).anyMatch(e -> containsValueInPrimitiveMap(subType, (Map) e, target));
        } else if (isPrimitiveMap()) {
            Type subType = type.getSubType();
            return ((Map) value).values().stream().filter(Objects::nonNull).anyMatch(o -> target.equalTo(subType, o));
        } else if (isComplexMap()) {
            Type subType = type.getSubType().getSubType();
            return ((Map) value).values().stream().filter(Objects::nonNull).anyMatch(e -> containsValueInPrimitiveMap(subType, (Map) e, target));

        }
        throw new UnsupportedOperationException("This type of field does not support contains value: " + type);
    }

    /**
     * Returns {@link TypedObject#TRUE} if the value or its underlying values contain a mapping for the specified key.
     * Returns {@link TypedObject#NULL} if the value or its underlying values does not contain a mapping for the
     * specified key but contains a null key(s). Returns {@link TypedObject#FALSE} otherwise. Only
     * {@link Type#COMPLEX_LISTS} and {@link Type#MAPS} support getting a mapping.
     *
     * @param key The key to be tested.
     * @return A {@link TypedObject} Boolean or Null to indicate if the value or its underlying values contain a mapping for the specified key.
     * @throws UnsupportedOperationException if not supported.
     */
    @SuppressWarnings("unchecked")
    public Boolean ternaryContainsKey(String key) {
        if (value == null || key == null) {
            return null;
        }
        if (isComplexList()) {
            List list = (List) value;
            boolean containsNull = false;
            for (Object o : list) {
                Map map = (Map) o;
                if (map == null) {
                    containsNull = true;
                } else if (map.containsKey(key)) {
                    return true;
                } else if (map.containsKey(null)) {
                    containsNull = true;
                }
            }
            return containsNull ? null : false;
        } else if (isComplexMap()) {
            Map map = (Map) value;
            if (map.containsKey(key) || map.values().stream().anyMatch(e -> ((Map) e).containsKey(key))) {
                return true;
            } else if (map.containsKey(null) || map.values().stream().anyMatch(e -> ((Map) e).containsKey(null))) {
                return null;
            } else {
                return false;
            }
        } else if (isPrimitiveMap()) {
            Map map = (Map) value;
            if (map.containsKey(key)) {
                return true;
            } else if (map.containsKey(null)) {
                return null;
            } else {
                return false;
            }
        }
        throw new UnsupportedOperationException("This type does not support mappings: " + type);
    }

    /**
     * Returns {@link TypedObject#TRUE} if the value or its underlying values contain the specified value. Returns
     * {@link TypedObject#NULL} if the value or its underlying values does not contain the specified value but contains
     * a null value(s). Returns {@link TypedObject#FALSE} otherwise. Only LIST and MAP are supported.
     *
     * @param target The target {@link TypedObject} to be tested.
     * @return A {@link TypedObject} Boolean or Null to indicate if the value or its underlying values contain the specified value.
     * @throws UnsupportedOperationException if not supported.
     */
    @SuppressWarnings("unchecked")
    public Boolean ternaryContainsValue(TypedObject target) {
        if (isNull() || target.isNull()) {
            return null;
        }
        if (isPrimitiveList()) {
            Type subType = type.getSubType();
            boolean containsNull = false;
            for (Object o : ((List) value)) {
                if (o == null) {
                    containsNull = true;
                } else if (target.equalTo(subType, o)) {
                    return true;
                }
            }
            return containsNull ? null : false;
        } else if (isComplexList()) {
            Type subType = type.getSubType().getSubType();
            boolean containsNull = false;
            for (Object o : ((List) value)) {
                if (o == null) {
                    containsNull = true;
                } else {
                    for (Object p : ((Map) o).values()) {
                        if (p == null) {
                            containsNull = true;
                        } else if (target.equalTo(subType, p)) {
                            return true;
                        }
                    }
                }
            }
            return containsNull ? null : false;
        } else if (isPrimitiveMap()) {
            Type subType = type.getSubType();
            boolean containsNull = false;
            for (Object o : ((Map) value).values()) {
                if (o == null) {
                    containsNull = true;
                } else if (target.equalTo(subType, o)) {
                    return true;
                }
            }
            return containsNull ? null : false;
        } else if (isComplexMap()) {
            Type subType = type.getSubType().getSubType();
            boolean containsNull = false;
            for (Object o : ((Map) value).values()) {
                if (o == null) {
                    containsNull = true;
                } else {
                    for (Object p : ((Map) o).values()) {
                        if (p == null) {
                            containsNull = true;
                        } else if (target.equalTo(subType, p)) {
                            return true;
                        }
                    }
                }
            }
            return containsNull ? null : false;

        }
        throw new UnsupportedOperationException("This type of field does not support contains value: " + type);
    }

    /**
     * Force cast this object to the given {@link Type} castedType. Will return a new {@link TypedObject}.
     *
     * @param castedType The {@link Type} to be casted to.
     * @return The casted {@link TypedObject}.
     */
    public TypedObject forceCast(Type castedType) {
        return new TypedObject(castedType, type.forceCast(castedType, value));
    }

    /**
     * Returns true if this equals the specified object. The object can be a {@link TypedObject} or be constructed as
     * a {@link TypedObject}. Note that this is not the same as {@link #equals(Object)}. This will do a safe cast to
     * unify {@link Type#NUMERICS} and compare. This will basically use {@link #compareTo(TypedObject)} and check if the
     * result is 0.
     *
     * @param target The object to compare to.
     * @return A boolean to indicate if this equals the specified object.
     */
    public boolean equalTo(TypedObject target) {
        return compareTo(target) == 0;
    }

    /**
     * Compares this TypedObject to another. Only works on objects that have a type in {@link Type#PRIMITIVES}.
     * An exception will be thrown when comparing objects whose types are not in {@link Type#PRIMITIVES}, excepting
     * {@link Type#NULL}, unless both are of {@link Type#NULL}. This will force {@link Type#NUMERICS} that are not of
     * the same {@link Type} to {@link Double} instances to compare them.
     * {@inheritDoc}
     *
     * @param other The other non-null TypedObject.
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException if the other object could not compared to this.
     */
    @Override
    public int compareTo(TypedObject other) {
        if (!Type.canCompare(type, other.type)) {
            throw new UnsupportedOperationException("Types are not comparable for " + this + " with " + other);
        }
        // Both are NULL
        if (type == Type.NULL) {
            return 0;
        }
        // If the types are not the same, not NULL and they can be compared, they must be numeric
        if (type != other.type) {
            return Double.compare(((Number) value).doubleValue(), ((Number) other.value).doubleValue());
        }
        // Types are the same and are primitive, but they aren't NULL or UNKNOWN
        switch (type) {
            case BOOLEAN:
                return ((Boolean) value).compareTo((Boolean) other.value);
            case INTEGER:
                return ((Integer) value).compareTo((Integer) other.value);
            case LONG:
                return ((Long) value).compareTo((Long) other.value);
            case FLOAT:
                return ((Float) value).compareTo((Float) other.value);
            case DOUBLE:
                return ((Double) value).compareTo((Double) other.value);
            default:
                return value.toString().compareTo((String) other.value);
        }
    }

    /**
     * Returns a {@link Comparator} for {@link TypedObject} that, unlike {@link TypedObject#compareTo(TypedObject)},
     * will work on any one TypedObject being {@link TypedObject#NULL}. In particular, this comparator will place nulls
     * first when sorting by it. If the first object is {@link TypedObject#NULL}, the comparator will return
     * {@link Integer#MIN_VALUE}. If the second object is {@link TypedObject#NULL}, the comparator will return
     * {@link Integer#MIN_VALUE}. Otherwise, the standard {@link TypedObject#compareTo(TypedObject)} will be invoked.
     *
     * Note this comparator only works on non-null objects, i.e. the {@link TypedObject} arguments must not be null.
     *
     * @return A {@link Comparator} that can places {@link TypedObject#NULL} objects first.
     */
    public static Comparator<TypedObject> nullsFirst() {
        return (o1, o2) -> {
            boolean firstNull = o1.isNull();
            // One is null
            if (firstNull ^ o2.isNull()) {
                return firstNull ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            }
            return o1.compareTo(o2);
        };
    }

    /**
     * This is simply the {@link Comparator#reversed()} on the {@link #nullsFirst()} {@link Comparator}. See docs for
     * that method. This method places {@link TypedObject#NULL} objects last.
     *
     * @return A {@link Comparator} that can places {@link TypedObject#NULL} objects last.
     */
    public static Comparator<TypedObject> nullsLast() {
        return (o1, o2) -> {
            boolean firstNull = o1.isNull();
            // One is null
            if (firstNull ^ o2.isNull()) {
                return firstNull ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
            return o1.compareTo(o2);
        };
    }

    @Override
    public String toString() {
        return value + "::" + type;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TypedObject)) {
            return false;
        }
        TypedObject o = (TypedObject) other;
        return o.type == type && (value == null && o.value == null || value != null && value.equals(o.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    /**
     * Takes an object and returns a casted TypedObject according to the given type. Note that this only casts safely.
     * It can widen numeric types if loss of precision does not occur. See {@link Type#cast(Serializable)}.
     *
     * @param type The {@link Type} to cast the values to.
     * @param object The Object that is being cast.
     * @return The casted TypedObject with the {@link Type} or {@link TypedObject#UNKNOWN} if the cast failed.
     */
    public static TypedObject safeCastFromObject(Type type, Serializable object) {
        // No longer makes a UNKNOWN object if object was null
        try {
            return new TypedObject(type, type.cast(object));
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    /**
     * Takes a non-null value and returns a numeric TypedObject - it has a type in {@link Type#NUMERICS}. The value
     * is then a {@link Number}. It uses the String representation of the object to cast it.
     *
     * @param value The Object value that is being cast to a numeric.
     * @return The casted TypedObject with the type set to numeric or {@link TypedObject#UNKNOWN} if not.
     */
    public static TypedObject forceCastStringToNumber(Serializable value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return new TypedObject(Type.DOUBLE, Type.STRING.forceCast(Type.DOUBLE, value.toString()));
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    /**
     * Returns a {@link TypedObject} representation of the {@link String} argument.
     *
     * @param s A non-null {@link String}.
     * @return A {@link TypedObject} representation of the {@link String} argument.
     */
    public static TypedObject valueOf(String s) {
        Objects.requireNonNull(s);
        return new TypedObject(Type.STRING, s);
    }

    /**
     * Returns a {@link TypedObject} representation of the boolean argument.
     *
     * @param b A boolean.
     * @return A {@link TypedObject} representation of the boolean argument.
     */
    public static TypedObject valueOf(boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Returns a {@link TypedObject} representation of the int argument.
     *
     * @param i An int.
     * @return A {@link TypedObject} representation of the int argument.
     */
    public static TypedObject valueOf(int i) {
        return new TypedObject(Type.INTEGER, i);
    }

    /**
     * Returns a {@link TypedObject} representation of the long argument.
     *
     * @param l A long.
     * @return A {@link TypedObject} representation of the long argument.
     */
    public static TypedObject valueOf(long l) {
        return new TypedObject(Type.LONG, l);
    }

    /**
     * Returns a {@link TypedObject} representation of the float argument.
     *
     * @param f A float.
     * @return A {@link TypedObject} representation of the float argument.
     */
    public static TypedObject valueOf(float f) {
        return new TypedObject(Type.FLOAT, f);
    }

    /**
     * Returns a {@link TypedObject} representation of the double argument.
     *
     * @param d A double.
     * @return A {@link TypedObject} representation of the double argument.
     */
    public static TypedObject valueOf(double d) {
        return new TypedObject(Type.DOUBLE, d);
    }

    private boolean equalTo(Type type, Object object) {
        // These are our objects. They should be serializable
        return equalTo(new TypedObject(type, (Serializable) object));
    }

    private static boolean containsValueInPrimitiveMap(Type mapValueType, Map<?, ?> map, TypedObject target) {
        return map.values().stream().filter(Objects::nonNull).anyMatch(o -> target.equalTo(mapValueType, o));
    }
}

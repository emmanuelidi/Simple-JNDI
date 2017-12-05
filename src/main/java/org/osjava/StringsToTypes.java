package org.osjava;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Jarrad Waterloo, Holger Thurow (thurow.h@gmail.com)
 * @since 05.12.17
 */
public class StringsToTypes {

    private static final Set<String> trueValues;
    private static final Set<String> falseValues;
    private static final int OLD_ENUM_STYLE = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
    private static final List<SimpleDateFormat> dateTimeFormats;
    private static final List<SimpleDateFormat> dateFormats;
    private static final List<SimpleDateFormat> timeFormats;

    static {
        trueValues = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        falseValues = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Collections.addAll(trueValues, "true", "t", "yes", "y", "on", "1", "x", "-1");
        Collections.addAll(falseValues, "false", "f", "no", "n", "off", "0", "");
        dateTimeFormats = new ArrayList<SimpleDateFormat>();
        Collections.addAll(dateTimeFormats, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
                new SimpleDateFormat("yyyy-MM-dd"));
        for (SimpleDateFormat sdf : dateTimeFormats) {
            sdf.setLenient(false);
        }
        dateFormats = new ArrayList<SimpleDateFormat>();
        Collections.addAll(dateFormats, new SimpleDateFormat("yyyy-MM-dd"));
        for (SimpleDateFormat sdf : dateFormats) {
            sdf.setLenient(false);
        }
        timeFormats = new ArrayList<SimpleDateFormat>();
        Collections.addAll(timeFormats, new SimpleDateFormat("HH:mm:ss.SSSZ"),
                new SimpleDateFormat("HH:mm:ss.SSSXXX"),
                new SimpleDateFormat("HH:mm:ss"),
                new SimpleDateFormat("HH:mm"));
        for (SimpleDateFormat sdf : timeFormats) {
            sdf.setLenient(false);
        }
    }

    public static boolean toBoolean(String value) {
        if (trueValues.contains(value)) {
            return true;
        }
        if (falseValues.contains(value)) {
            return false;
        }
        throw getRuntimeException(value, boolean.class);
    }

    /**
     *
     * @return public static final field with this name.
     */
    private static Field findOldStyleEnumField(Class<?> clazz, String name) {
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (clazz.equals(field.getType()) && field.getName().equalsIgnoreCase(name.trim()) && ((field.getModifiers() & OLD_ENUM_STYLE) == OLD_ENUM_STYLE)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Support for int/string enum pattern. That means classes declaring constants with public static final fields to mimic enums.
     */
    public static Object toOldStyleEnumField(Class<?> toWhat, String value) {
        Object obj;
        try {
            Field field = findOldStyleEnumField(toWhat, value);
            if (field == null) {
                Constructor constructor = toWhat.getConstructor(String.class);
                obj = constructor.newInstance(value);
            }
            else {
                obj = field.get(null);
            }
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find (String) constructor on class: " + toWhat, e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate class: " + toWhat, e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to access class: " + toWhat, e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to invoke (String) constructor on class: " + toWhat, e);
        }
        return obj;
    }

    @NotNull
    public static Object toEnum(Class<?> toWhat, String value) {
        Object obj = null;
        //obj = Enum.valueOf(toWhat, value.trim());
        boolean valid = false;
        Object[] enumConstants = toWhat.getEnumConstants();
        for (Object oe : enumConstants) {
            Enum e = (Enum) oe;
            if (e.name().equals(value)) {
                obj = e;
                valid = true;
            }
        }
        for (Object oe : enumConstants) {
            Enum e = (Enum) oe;
            if (e.name().equalsIgnoreCase(value)) {
                obj = e;
                valid = true;
            }
        }
        if (!valid) {
            throw getRuntimeException(value, toWhat);
        }
        return obj;
    }

    @NotNull
    public static Object toTimestamp(String value) {
        Object obj = null;
        boolean valid = false;
        for (SimpleDateFormat sdf : dateTimeFormats) {
            try {
                obj = new java.sql.Timestamp(sdf.parse(value).getTime());
                valid = true;
                break;
            }
            catch (ParseException ignored) { }
        }
        if (!valid) {
            throw getRuntimeException(value, Timestamp.class);
        }
        return obj;
    }

    @NotNull
    public static Object toTime(String value) {
        Object obj = null;
        boolean valid = false;
        for (SimpleDateFormat sdf : timeFormats) {
            try {
                obj = new java.sql.Time(sdf.parse(value).getTime());
                valid = true;
                break;
            }
            catch (ParseException ignored) { }
        }
        if (!valid) {
            throw getRuntimeException(value, Time.class);
        }
        return obj;
    }

    @NotNull
    public static Object toSqlDate(String value) {
        Object obj = null;
        boolean valid = false;
        for (SimpleDateFormat sdf : dateFormats) {
            try {
                obj = new java.sql.Date(sdf.parse(value).getTime());
                valid = true;
                break;
            }
            catch (ParseException ignored) { }
        }
        if (!valid) {
            throw getRuntimeException(value, java.sql.Date.class);
        }
        return obj;
    }

    public static Object toDate(String value) {
        Object obj = null;
        boolean valid = false;
        for (SimpleDateFormat sdf : dateTimeFormats) {
            try {
                obj = sdf.parse(value);
                valid = true;
                break;
            }
            catch (ParseException ignored) { }
        }
        if (!valid) {
            throw getRuntimeException(value, Date.class);
        }
        return obj;
    }

    @NotNull
    private static RuntimeException getRuntimeException(String value, Class<?> toWhat) {
        return new RuntimeException(
                String.format(
                    "The value, '%s' could not be converted to a '%s'",
                    value,
                    toWhat.getName()
                )
        );
    }

    public static char toCharacter(@NotNull String value) {
        return !value.isEmpty() ? value.charAt(0) : (char)Character.UNASSIGNED;
    }
}

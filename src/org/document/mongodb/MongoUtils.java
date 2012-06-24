/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.document.mongodb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.document.schema.DefaultSchema;
import org.document.schema.DocumentSchema;
import org.document.schema.ReflectAccessor;
import org.document.schema.SchemaField;

/**
 *
 * @author Valery
 */
public class MongoUtils {
    
    public static boolean isMongoValueType(Class type) {
        return type.isPrimitive()
                || type.equals(java.util.Date.class)
                || type.equals(String.class)
                || type.equals(Boolean.class)
                || type.equals(Integer.class)
                || type.equals(Short.class)
                || type.equals(Byte.class)
                || type.equals(Float.class)
                || type.equals(Double.class)
                || type.equals(Character.class)
                || type.equals(Long.class);
    }
    
    public static boolean isMongoValueType(Object object) {
        Class type = object.getClass();
        return isMongoValueType(type);
    }
    
    public static boolean isValueType(Class type) {
        return type.isPrimitive()
                || type.equals(java.lang.Object.class)
                || type.equals(java.util.Date.class)
                || type.equals(String.class)
                || type.equals(Boolean.class)
                || type.equals(Integer.class)
                || type.equals(Short.class)
                || type.equals(Byte.class)
                || type.equals(Float.class)
                || type.equals(Double.class)
                || type.equals(Character.class)
                || type.equals(Long.class)
                || type.equals(java.sql.Time.class)
                || type.equals(java.sql.Timestamp.class)
                || type.equals(java.sql.Date.class)
                || type.equals(BigInteger.class)
                || type.equals(BigDecimal.class);
    }

    public static boolean isValueType(Object object) {
        Class type = object.getClass();
        return type.isPrimitive()
                || type.equals(java.lang.Object.class)
                || type.equals(java.util.Date.class)
                || type.equals(String.class)
                || type.equals(Boolean.class)
                || type.equals(Integer.class)
                || type.equals(Short.class)
                || type.equals(Byte.class)
                || type.equals(Float.class)
                || type.equals(Double.class)
                || type.equals(Character.class)
                || type.equals(Long.class)
                || type.equals(java.sql.Time.class)
                || type.equals(java.sql.Timestamp.class)
                || type.equals(java.sql.Date.class)
                || type.equals(BigInteger.class)
                || type.equals(BigDecimal.class);
    }
    
    public static DocumentSchema createSchema(Class clazz, Class superBoundary) {
        DocumentSchema schema = new DefaultSchema(clazz);
        Map<String, Boolean> transientFields = new HashMap<String, Boolean>();
        Map<String, Field> allFields = new HashMap<String, Field>();
        Class c = clazz;
        try {
            while (c != superBoundary) {
                java.lang.reflect.Field[] dfs = c.getDeclaredFields();
                for (java.lang.reflect.Field f : dfs) {

                    String nm = f.getName();
                    allFields.put(nm, f);
                    if (Modifier.isTransient(f.getModifiers())) {
                        transientFields.put(f.getName(), Boolean.TRUE);
                    }
                }
                c = c.getSuperclass();
            }

        } catch (Exception e) {
        }
        
        c = clazz;
        for (Method m : c.getMethods()) {

            if (!(m.getName().startsWith("is") || m.getName().startsWith("get") || m.getName().startsWith("set"))) {
                continue;
            }
            int idx = 3;
            boolean isGetter = false;
            if (m.getName().startsWith("get")) {
                isGetter = true;
            } else if (m.getName().startsWith("is")) {
                idx = 2;
                isGetter = true;
            }

            String name = m.getName().substring(idx);
            if (name.length() == 0) {
                continue;
            }
            String pname = name.substring(0, 1).toLowerCase() + name.substring(1);
            if ( ! pname.equals("bd_className_")) {
                if (transientFields.containsKey(pname) || !allFields.containsKey(pname)) {
                    continue;
                }
            }
            SchemaField f;
            if (!schema.contains(pname)) {
                Class ptype = isGetter ? m.getReturnType() : m.getParameterTypes()[0];
                f = new SchemaField(pname, ptype);
                schema.getFields().add(f);
                f.setField(allFields.get(pname));
                f.setAccessors(new ReflectAccessor(null, null));
            } else {
                f = schema.getField(pname);
            }
            
            if ( isGetter ) {
                ((ReflectAccessor)f.getAccessors()).setGetAccessor(m);
            } else {
                ((ReflectAccessor)f.getAccessors()).setSetAccessor(m);
            }
        }
        return schema;
    }
    
}

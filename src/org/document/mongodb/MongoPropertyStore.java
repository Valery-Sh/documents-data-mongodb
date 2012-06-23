package org.document.mongodb;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import org.document.Document;
import org.document.DocumentPropertyStore;
import org.document.KeyValueMap;
import org.document.schema.DocumentSchema;
import org.document.schema.SchemaField;
import org.document.schema.SchemaUtils;

/**
 *
 * @author V. Shyshkin
 */
public class MongoPropertyStore extends DocumentPropertyStore {

    public MongoPropertyStore(Document source) {
        super(source);
    }

    @Override
    public Object getValue(String key) {
        return getValue(key, owner);
    }

    @Override
    public Object putSilent(String key, Object value) {
        return super.putSilent(key, value);
    }

    /**
     * The method is defined in order to easy override in a subclass without a
     * need to override the
     * <code>putValue</code> method.
     *
     * @param name string property name
     * @param value a value to be bind
     */
    @Override
    protected void setPropertyValue(String key, Object value) {
        setValue(key, owner, value);
    }

    public static Object getValue(String key, Object obj) {
        String error = "";
        if (obj instanceof KeyValueMap) {
            return ((KeyValueMap) obj).getMap().get(key);
        }
        try {
            BeanInfo binfo = Introspector.getBeanInfo(obj.getClass(), Object.class);
            PropertyDescriptor[] props = binfo.getPropertyDescriptors();

            for (int i = 0; i < props.length; i++) {
                String pname = props[i].getName();
                if (key.equals(pname)) {
                    Method m = props[i].getReadMethod();
                    Object v = m.invoke(obj, null);
                    return v;
                }
            }//for

        } catch (IntrospectionException ex) {
            error = ex.getMessage();
        } catch (IllegalAccessException ex) {
            error = ex.getMessage();
        } catch (java.lang.reflect.InvocationTargetException ex) {
            error = ex.getMessage();
        }

        throw new NullPointerException("An object of type " + obj.getClass() + " doesn't contain a field with a name " + key + "(" + error + ")");
    }

    public static void setValue(String key, Object obj, Object newValue) {
        String error;// = "";
        if (obj instanceof KeyValueMap) {
            ((KeyValueMap) obj).getMap().put(key, newValue);
            return;
        }
        try {
            BeanInfo binfo = Introspector.getBeanInfo(obj.getClass(), Object.class);
            PropertyDescriptor[] props = binfo.getPropertyDescriptors();

            for (int i = 0; i < props.length; i++) {
                String pname = props[i].getName();

                if (key.equals(pname)) {
                    Method m = props[i].getWriteMethod();
                    m.invoke(obj, newValue);
                    break;
                }

            }//for

        } catch (IntrospectionException ex) {
            error = ex.getMessage();
        } catch (IllegalAccessException ex) {
            error = ex.getMessage();
        } catch (java.lang.reflect.InvocationTargetException ex) {
            error = ex.getMessage();
        }
    }

    @Override
    protected DocumentSchema createSchema(Class sourceClass, Class superBoundary) {
        DocumentSchema sc = SchemaUtils.createSchema(sourceClass,superBoundary);        
        SchemaField f = new SchemaField("bd_className_",String.class);
        sc.getFields().add(f);
        return sc;

    }
}

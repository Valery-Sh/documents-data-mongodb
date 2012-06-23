/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.document.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.BSONObject;
import org.document.Document;
import org.document.PropertyStore;
import org.document.schema.DocumentSchema;
import org.document.schema.HasSchema;
import org.document.schema.SchemaField;

/**
 *
 * @author Valery
 */
public class BoundMongoObject extends BasicDBObject implements Document {

    protected transient MongoPropertyStore propertyStore;
    protected String bd_className_;
    //protected transient Map<String, Object> mongoProperties;
    //protected BasicDBObject dbObject;
    //protected transient DBObject source;

    public BoundMongoObject() {
        super();
//        source = new BasicDBObject();
        propertyStore = new MongoPropertyStore(this);
        bd_className_ = getClass().getName();
        putInternal("bd_className_", bd_className_);
        //mongoProperties = new HashMap<String, Object>();
        //dbObject = super;

    }

    public String getBd_className_() {
        return bd_className_;
    }

    public void setBd_className_(String bd_className_) {
        this.bd_className_ = bd_className_;
        bind("bd_className_", bd_className_);
    }

    public void objectDone(DBObject o) {
        System.out.println("NEW DECODER IN");
        if (isEmbedded(o)) {
            ((BoundMongoObject) o).applyChanges();
        }
    }

    protected void applyChanges() {

        List<SchemaField> fields = getSchemaFields();
        //Set<String> keys = keySet();
        for (String key : keySet()) {
            Object o = get(key);
            if (o == null) {
                continue;
            }
            SchemaField f = getSchemaField(key);
            if (f == null) {
                continue;
            }
            if (Document.class.isAssignableFrom(f.getPropertyType())) {
                changeBean(key, o);
            } else if (Map.class.isAssignableFrom(f.getPropertyType())) {
                changeMap(key, o);
            }
        }
    }

    protected void changeMap(String key, Object value) {

        SchemaField field = getSchemaField(key);

        if (field == null) { // ? tail
            return;
        }
        Map bean = (Map) propertyStore.getValue(key);
        try {
            DBObject dbObject = (DBObject) value;
            for (String dboKey : dbObject.keySet()) {
                Object o = dbObject.get(dboKey);

                if (isEmbeddedElement(o)) {
                    BoundMongoObject elem = getEmbeddedElement(dboKey, bean, (DBObject)o);
                    bean.put(dboKey, elem);
                    continue;
                }
                if (o instanceof DBObject) {
                    // List or Map 
                    //TODO for List
                    Map target = (Map) bean.get(dboKey);
                    if (target == null) {
                        target = new HashMap();
                    }
                    changeMapElements((DBObject) o, target);
                    bean.put(dboKey, target);
                } else {
                    bean.put(dboKey, o);
                }
                //SchemaField f = getSchemaField(dboKey);
            }

        } catch (Exception e) {
        }

    }

    protected void changeMapElements(DBObject dbObject, Map target) {
        if (dbObject == null) {
            return;
        }
        try {

            for (String dboKey : dbObject.keySet()) {
                Object o = dbObject.get(dboKey);
                if (isEmbeddedElement(o)) {
                    BoundMongoObject elem = getEmbeddedElement(dboKey, target, (DBObject)o);
                    //((BoundMongoObject) o).applyChanges();
                    target.put(dboKey, elem);
                    continue;
                }
                if (o instanceof DBObject) {
                    // List or Map 
                    //TODO for List
                    Map t = new HashMap();
                    changeMapElements((DBObject) o, t);
                    target.put(dboKey, t);
                } else {
                    target.put(dboKey, o);
                }
                //SchemaField f = getSchemaField(dboKey);
            }
        } catch (Exception e) {
        }

    }

    protected BoundMongoObject getEmbeddedElement(String key, Map map, DBObject o) {
        BoundMongoObject elem = (BoundMongoObject)map.get(key);
        if (elem == null) {
            String cname = null;
            try {
                cname = (String) ((DBObject) o).get("bd_className_");
                elem = (BoundMongoObject) Class.forName(cname).newInstance();
                List<SchemaField> sc = getSchemaFields(elem);
                for ( SchemaField f : sc) {
                    String nm = (String)f.getPropertyName();
                    
                    elem.put(nm, ((DBObject) o).get(nm));
                }
                elem.applyChanges();
            } catch (Exception e) {
                System.out.println("Cannot create an instance of " + cname);
            }
        } else {
            elem.applyChanges();
        }
        return elem;
    }

    protected void changeBean(String key, Object value) {

        SchemaField field = getSchemaField(key);

        if (field == null) { // ? tail
            return;
        }
        BoundMongoObject bean = (BoundMongoObject) propertyStore.getValue(key);
        try {
            DBObject dbObject = (DBObject) value;
            for (SchemaField f : bean.getSchemaFields()) {
                Object v = dbObject.get(f.getPropertyName().toString());
                if (v == null) {
                    continue;
                }
                if (isEmbedded(v)) {
                    ((BoundMongoObject) v).applyChanges();
                    continue;
                }
                //bean.put(f.getPropertyName().toString(), v);
                bean.propertyStore.putSilent(f.getPropertyName().toString(), v);
            }

        } catch (Exception e) {
        }
    }

    private boolean isEmbedded(Object o) {
        return Document.class.isAssignableFrom(o.getClass());
    }

    private boolean isEmbeddedElement(Object o) {
        if (!(o instanceof DBObject)) {
            return false;
        }
        Object cn = ((DBObject) o).get("bd_className_");
        if (cn == null) {
            return false;
        }
        return true;

    }

    private boolean isMap(Object o) {
        return Map.class.isAssignableFrom(o.getClass());
    }

    @Override
    public PropertyStore propertyStore() {
        return propertyStore;
    }

    @Override
    public Object get(String key) {

        return super.get(key);
    }
    boolean lockPut;

    @Override
    public Object put(String key, Object value) {
        Object result = null;
        if (lockPut) {
            return result;
        }
        try {
            lockPut = true;
            result = super.put(key, value);
            putBean(key, value);
        } finally {
            lockPut = false;
        }
        return result;
    }

    public Object putInternal(String key, Object value) {
        if (lockPut) {
            return null;
        }
        return super.put(key, value);
        //return dbObject.put(key, value);
    }

    public Object putBean(String key, Object value) {

        SchemaField field = getSchemaField(key);

        if (field == null) { // ? tail
            return null;
        }
        Object result = null;
        try {
            result = propertyStore.getValue(key);
            if (Document.class.isAssignableFrom(field.getPropertyType())) {
                try {
                    BoundMongoObject o = (BoundMongoObject) field.getPropertyType().newInstance();
                    DBObject dbv = (DBObject) value;

                    for (SchemaField f : o.getSchemaFields()) {
                        Object v = dbv.get(f.getPropertyName().toString());
                        o.put(f.getPropertyName().toString(), v);
                    }
                    propertyStore.putSilent(key, o);
                } catch (Exception e) {
                }
            } else if (Map.class.isAssignableFrom(field.getPropertyType())) {
                Map o;
                if (field.getPropertyType().isInterface()) {
                    o = new HashMap();
                } else {
                    o = (Map) field.getPropertyType().newInstance();
                }
                DBObject dbv = (DBObject) value;
                if (dbv != null) {
                    o.putAll(dbv.toMap());
                }
                propertyStore.putSilent(key, o);
            } else {
                propertyStore.putSilent(key, value);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return result;

    }

    @Override
    public void markAsPartialObject() {
        super.markAsPartialObject();
    }

    @Override
    public boolean isPartialObject() {
        return super.isPartialObject();
    }

    @Override
    public void putAll(BSONObject o) {
        super.putAll(o);

        //putAllBean(o);
    }

    @Override
    public void putAll(Map m) {
        super.putAll(m);
        //putAllBean(m);
    }

    protected void putAllBean(BSONObject o) {
        if (o == null) {
            return;
        }
        DocumentSchema sc = propertyStore.getSchema();
        for (String key : o.keySet()) {
            if (sc.getField(key) != null) {
                propertyStore.putSilent(key, o.get(key));
            }
            //Map.Entry me = ((Map.Entry) eo);
            //put(me.getKey().toString(), me.getValue());
        }
    }

    protected void putAllBean(Map m) {
        if (m == null) {
            return;
        }
        DocumentSchema sc = propertyStore.getSchema();
        for (Object eo : m.entrySet()) {
            Map.Entry me = ((Map.Entry) eo);
            if (sc.getField(me.getKey().toString()) != null) {
                propertyStore.putSilent(me.getKey().toString(), me.getValue());
            }
        }
    }

    @Override
    public Map toMap() {
        return super.toMap();
        /*        Map<String, Object> m = new HashMap<String, Object>();
         m.putAll(mongoProperties);
         DocumentSchema sc = propertyStore.getSchema();
         if (sc == null) {
         return m;
         }
         try {
         for (SchemaField sf : sc.getFields()) {
         m.put(sf.getPropertyName().toString(), get(sf.getPropertyName().toString()));
         }
         } catch (Exception e) {
         }
         return m;
         */
    }

    @Override
    public Object removeField(String key) {
        Object result = super.removeField(key);
        propertyStore.putSilent(key, null);
        return result;

    }

    @Override
    public boolean containsKey(String s) {
        return containsField(s);
    }

    @Override
    public boolean containsField(String s) {
        if ("address".equals(s)) {
            System.out.println(s);
        }
        return super.containsField(s);
        /*        if (mongoProperties.containsKey(s)) {
         return true;
         }
         return getSchemaField(s) == null ? false : true;
         */
    }

    @Override
    public Set<String> keySet() {
        return super.keySet();

//        Map m = toMap();
//        return m.keySet();
    }

    /*    protected Field getField(String propertyName) {
     DocumentSchema sc = propertyStore.getSchema();
     SchemaField sf = sc.getField(propertyName);
     if (sf == null) {
     return null;
     }
     return sc.getField(propertyName).getField();
     }
     */
    protected List<SchemaField> getSchemaFields() {
        DocumentSchema sc = propertyStore.getSchema();
        if (sc != null) {
            return sc.getFields();
        }
        return null;
    }
    protected List<SchemaField> getSchemaFields(Document d) {
        DocumentSchema sc = ((HasSchema)d.propertyStore()).getSchema();
        if (sc != null) {
            return sc.getFields();
        }
        return null;
    }

    protected SchemaField getSchemaField(String propertyName) {
        DocumentSchema sc = propertyStore.getSchema();
        return sc.getField(propertyName);
    }

    protected void bind(String propertyName, Object value) {
        putInternal(propertyName, value);
        propertyStore.bind(propertyName, value);
    }
    /*    public Map<String,Field> createFields(Class clazz) {
     Map<String,Field> fields = new HashMap<String,Field>();
        
     DocumentSchema sc = propertyStore.getSchema();
     for ( SchemaField sf : sc.getFields() ) {
     fields.put(null, sf.getField());
     }
     * 
     return fields;
     }
     */
}

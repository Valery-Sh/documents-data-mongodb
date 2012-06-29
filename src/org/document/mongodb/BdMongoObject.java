/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.document.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.BSONObject;
import org.document.DataUtils;
import org.document.Document;
import org.document.PropertyStore;
import org.document.schema.DocumentSchema;
import org.document.schema.HasSchema;
import org.document.schema.SchemaField;

/**
 *
 * @author Valery
 */
public class BdMongoObject extends BasicDBObject implements Document {
//public class BdMongoObject implements DBObject,Document {
    boolean lockPut;
    public static final String CLASS_NAME_FIELD = "bd_className_";
    protected transient MongoPropertyStore propertyStore;
    protected String bd_className_;
    //protected transient Map<String, Object> mongoProperties;
    //protected BasicDBObject dbObject;
    //protected transient DBObject source;

    public BdMongoObject() {
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
        //if (isEmbedded(o)) {
        if (this.getClass().equals(o.getClass())) {
            System.out.println("objectDone before apply: " + (new Date()).getTime() );
            ((BdMongoObject) o).applyChanges();
            System.out.println("objectDone after apply: " + (new Date()).getTime());
        } else {
             System.out.println("objectDone NOT BOUND apply: " + (new Date()).getTime() );
        }
    }

    protected void applyChanges() {

        for (String key : keySet()) {
            Object o = get(key);
            if (o == null) {
                continue;
            }
            SchemaField f = getSchemaField(key);
            if (f == null) {
                continue;
            }
            if (BdMongoObject.class.isAssignableFrom(f.getPropertyType())) {
                BdMongoObject boundObject = boundObjectOf(this, key, (DBObject) o);
                propertyStore.putSilent(key, boundObject);
            } else if (Map.class.isAssignableFrom(f.getPropertyType())) {
                try {
                    Map target = null;
                    Map m = mapOf((DBObject) o);
                    if (m != null) {
                        target = (Map) f.getPropertyType().newInstance();
                        target.putAll(m);
                    }
                    propertyStore.putSilent(key, target);
                } catch (Exception e) {
                }
            } else if (List.class.isAssignableFrom(f.getPropertyType())) {
                try {
                    List target = null;
                    List m = listOf((BasicDBList) o);
                    if (m != null) {
                        target = (List) f.getPropertyType().newInstance();
                        target.addAll(m);
                    }
                    propertyStore.putSilent(key, target);
                } catch (Exception e) {
                }
            }

        }
    }

    //protected void changeMapElements(DBObject dbObject, Map target) {
    protected Map mapOf(DBObject dbObject) {
        if (dbObject == null) {
            return null;
        }
        Map target = new HashMap();
        for (String dboKey : dbObject.keySet()) {
            Object o = dbObject.get(dboKey);
            if (isEmbeddedObject(o)) {
                BdMongoObject elem = boundObjectOf((DBObject) o);
                target.put(dboKey, elem);
            } else if (o instanceof DBObject) {
                Map t = mapOf((DBObject) o);
                target.put(dboKey, t);
            } else if (o instanceof BasicDBList) {
                List l = listOf((BasicDBList) o);
                target.put(dboKey, l);
            } else {
                target.put(dboKey, o);
            }
        }
        return target;
    }

    protected List listOf(BasicDBList dbList) {
        if (dbList == null) {
            return null;
        }
        List target = new ArrayList();
        for (Object o : dbList) {
            if (isEmbeddedObject(o)) {
                BdMongoObject elem = boundObjectOf((BdMongoObject) o);
                target.add(elem);
            }
            if (o instanceof DBObject) {
                Map t = new HashMap();
                target.add(t);
            } else if (o instanceof BasicDBList) {
                List l = listOf((BasicDBList) o);
                target.add(l);
            } else {
                target.add(o);
            }
        }
        return target;
    }
    //protected BdMongoObject boundObjectOf(String key, Map map, DBObject o) {

    protected BdMongoObject boundObjectOf(DBObject dbObject) {
        BdMongoObject boundObject = null;
        String cname = null;
        try {
            cname = (String) ((DBObject) dbObject).get("bd_className_");
            boundObject = (BdMongoObject) Class.forName(cname).newInstance();
            for (String key : dbObject.keySet()) {
                boundObject.put(key, dbObject.get(key));
            }
            boundObject.applyChanges();
        } catch (Exception e) {
            System.out.println("Cannot create an instance of " + cname);
        }
        return boundObject;
    }

    protected BdMongoObject boundObjectOf(BdMongoObject owner, String key, DBObject dbObject) {

        SchemaField field = owner.getSchemaField(key);

        if (field == null) { // ? tail
            return null;
        }
        //BoundMongoObject bean = (BdMongoObject) owner.propertyStore.getValue(key);
        BdMongoObject boundObject = null;
        try {
            boundObject = (BdMongoObject) field.getPropertyType().newInstance();
            for (String k : dbObject.keySet()) {
                boundObject.put(k, dbObject.get(k));
            }
            boundObject.applyChanges();
        } catch (Exception e) {
            System.out.println("Cannot create an instance of " + field.getPropertyType());
        }
        return boundObject;
    }

    private boolean isEmbeddedObject(Object o) {
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
    

    @Override
    public Object put(String key, Object value) {
        Object result = null;
        if (lockPut) {
            return result;
        }
        try {
            lockPut = true;
            result = super.put(key, value);
            if ( value instanceof DBObject) {
                if ( ((DBObject)value).get("bd_decoder") != null ) {
                    return result;
                }
            }
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

    protected Object putBean(String key, Object value) {
        return putBean(this, key, value);
    }

    protected Object putBean(BdMongoObject boundObj, String key, Object value) {

        SchemaField field = boundObj.getSchemaField(key);
        if (field == null) {
            return null;
        }
        /*      
         TODO may be check  _id and other system properties     
         if (field == null) { // ? tail
         throw new MongoException("The object of type " + boundObj.getClass() +
         " doesn't contain a property with a name '" + key + "'");
         //return null;
         }
         */
        //boolean error = false;
        String msg = "";
        if ( value != null ) {       
            msg =   "Incompatible type for property '" + key + "'"
                    + ". Required " + field.getPropertyType()
                    + " but found " + value.getClass();
        }

        Object result = null;
        try {
            result = boundObj.propertyStore.getValue(key);
            Class propertyType = field.getPropertyType();
            if (propertyType.isPrimitive()) {
                propertyType = DataUtils.getWrapper(propertyType);
            }
            if (value == null) {
                boundObj.propertyStore.putSilent(key, value);
            } else if (MongoUtils.isMongoValueType(propertyType)) {
                if (propertyType.isAssignableFrom(value.getClass())) {
                    boundObj.propertyStore.putSilent(key, value);
                } else {
                    throw new MongoException(msg);
                }
            } else if (BdMongoObject.class.isAssignableFrom(value.getClass())) {
                if (propertyType.isAssignableFrom(value.getClass())) {
                    boundObj.propertyStore.putSilent(key, value);
                } else {
                    throw new MongoException(msg);
                }
                // else error
            } else if (value instanceof DBObject) {
                if (propertyType.isAssignableFrom(value.getClass())) {
                    //
                    // when property is Map or DBObject (but not BdMongoObject)
                    //
                    boundObj.propertyStore.putSilent(key, value);
                } else if (BdMongoObject.class.isAssignableFrom(propertyType)) {
                        //
                        // The property is instance of BdMongoObject. We'll try
                        // to copy properies from the value.
                        //
                        Object property = result; // current value
                        if (property == null) {
                            property = propertyType.newInstance();
                        }
                        BdMongoObject bobj = (BdMongoObject) property;
                        List<SchemaField> fields = bobj.getSchemaFields(bobj);
                        for (SchemaField f : fields) {
                            String nm = f.getPropertyName().toString();
                            //bobj.propertyStore.putSilent(nm, ((DBObject) value).get(nm));
                            if ( ! CLASS_NAME_FIELD.equals(nm) ) {
                                putBean(bobj, nm, ((DBObject) value).get(nm));
                            }    
                        }
                        propertyStore.putSilent(key, property);
                    } else {
                        throw new MongoException(msg);
                    }

                
                // else error
            } else if (List.class.isAssignableFrom(propertyType)) {
                if (propertyType.isAssignableFrom(value.getClass())) {
                    boundObj.propertyStore.putSilent(key, value);
                } else {
                    throw new MongoException(msg);
                }
                // else error
            } else if (Map.class.isAssignableFrom(propertyType)) {
                if (propertyType.isAssignableFrom(value.getClass())) {
                    boundObj.propertyStore.putSilent(key, value);
                } else {
                    throw new MongoException(msg);
                }
                // else error
            } else if (DBObject.class.isAssignableFrom(value.getClass())) {
                //
                // Here we must convert if possible the DBObject to property type
                // Property type may be only of BoundMongoObjectType: 
                //
                if (BdMongoObject.class.isAssignableFrom(propertyType)) {
                    Object property = result; // current value
                    if (property == null) {
                        property = propertyType.newInstance();
                    }
                    BdMongoObject bobj = (BdMongoObject) property;
                    List<SchemaField> fields = boundObj.getSchemaFields(bobj);
                    for (SchemaField f : fields) {
                        String nm = f.getPropertyName().toString();
                        //bobj.propertyStore.putSilent(nm, ((DBObject) value).get(nm));
                        putBean(bobj, nm, ((DBObject) value).get(nm));
                    }
                    propertyStore.putSilent(key, value);
                } else {
                    throw new MongoException(msg);
                }
            } else {
                throw new MongoException(msg);
                //propertyStore.putSilent(key, value);
            }
        } catch (Exception e) {
            //System.out.println(e.getMessage());
            throw new MongoException(e.getMessage());
        }
        return result;

    }

    public Object putBeanOLD(String key, Object value) {

        SchemaField field = getSchemaField(key);

        if (field == null) { // ? tail
            return null;
        }
        Object result = null;
        try {
            result = propertyStore.getValue(key);
            if (Document.class.isAssignableFrom(field.getPropertyType())) {
                try {
                    BdMongoObject o = (BdMongoObject) field.getPropertyType().newInstance();
                    DBObject dbv = (DBObject) value;

                    for (SchemaField f : o.getSchemaFields()) {
                        Object v = dbv.get(f.getPropertyName().toString());
                        o.put(f.getPropertyName().toString(), v);
                    }
                    propertyStore.putSilent(key, o);
                } catch (Exception e) {
                }
            } else if (Map.class.isAssignableFrom(field.getPropertyType())) {
                Map map;
                if (field.getPropertyType().isInterface()) {
                    map = new HashMap();
                } else {
                    map = (Map) field.getPropertyType().newInstance();
                }
                //changeMapElements((DBObject) value, map);
                DBObject dbv = (DBObject) value;
                if (dbv != null) {
                    map.putAll(dbv.toMap());
                }

                propertyStore.putSilent(key, map);
            } else if (List.class.isAssignableFrom(field.getPropertyType())) {
                List list;
                if (field.getPropertyType().isInterface()) {
                    list = new ArrayList();
                } else {
                    list = (List) field.getPropertyType().newInstance();
                }
                //changeListElements((DBObject) value, list);
                /*                DBObject dbv = (DBObject) value;
                 if (dbv != null) {
                 map.putAll(dbv.toMap());
                 }
                 */
                propertyStore.putSilent(key, list);
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
        DocumentSchema sc = ((HasSchema) d.propertyStore()).getSchema();
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

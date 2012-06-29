/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.document.mongodb;

import org.bson.BSON;
import org.bson.Transformer;

/**
 *
 * @author Valery
 */
public class MongoCollectionObject {
    
    private Class objectClass;
    private Transformer transformer;
    
    public MongoCollectionObject(Class objectClass) {
        super();
        this.objectClass = objectClass;
        this.transformer = new DecodeTransformer();
        BSON.addDecodingHook(getClass(), transformer);
    }

    public Class getObjectClass() {
        return objectClass;
    }
    
    public class DecodeTransformer implements Transformer {

        @Override
        public Object transform(Object o) {
            Object t = o;
            
            return t;
        }
        
    }
}

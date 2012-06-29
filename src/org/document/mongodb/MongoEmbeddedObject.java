package org.document.mongodb;

/**
 *
 * @author V. Shyshkin
 */
public class MongoEmbeddedObject {
    private Class objectClass;
    public MongoEmbeddedObject(Class objectClass) {
        super();
        this.objectClass = objectClass;
    }

    public Class getObjectClass() {
        return objectClass;
    }
    
}

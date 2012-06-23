package org.document.mongodb;

import com.mongodb.DBCollection;
import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBDecoder;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author V. Shyshkin
 */
public class BdDefaultDBDecoder extends DefaultDBDecoder{
    
    static class DefaultFactory implements DBDecoderFactory {
        @Override
        public DBDecoder create( ){
            return new BdDefaultDBDecoder( );
        }
    }

    public static DBDecoderFactory BDFACTORY = new DefaultFactory();

    public BdDefaultDBDecoder(){
        super();
    }
        
    @Override
    public DBObject decode(byte[] b, DBCollection collection) {
        DBObject o = super.decode(b, collection);
        if ( o != null && (o instanceof BoundMongoObject)) {
            ((BoundMongoObject)o).objectDone(o);
        }
        return o;
    }

    @Override
    public DBObject decode(InputStream in,  DBCollection collection) throws IOException {
        DBObject o = super.decode(in, collection);
        if ( o != null && (o instanceof BoundMongoObject)) {
            ((BoundMongoObject)o).objectDone(o);
        }
        return o;
    }
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.document.mongodb;

import com.mongodb.DBDecoderFactory;
import com.mongodb.MongoOptions;

/**
 *
 * @author Valery
 */
public class BdMongoOptions extends MongoOptions {
    
     public BdMongoOptions(){
        super();
        dbDecoderFactory = BdDefaultDBDecoder.BDFACTORY;
    }

}

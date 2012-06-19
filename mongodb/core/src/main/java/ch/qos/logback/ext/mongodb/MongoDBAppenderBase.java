/*
 * Copyright (C) 2012, QOS.ch. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.ext.mongodb;

import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

/**
 * An abstract appender handling connection to MongoDB. Subclasses should
 * implement {@link #toMongoDocument(Object)}.
 * 
 * @author Tomasz Nurkiewicz
 * @author Christian Trutz
 * @since 0.1
 */
public abstract class MongoDBAppenderBase<E> extends UnsynchronizedAppenderBase<E> {

    // MongoDB instance
    private Mongo mongo = null;

    // MongoDB collection containing logging events
    private DBCollection eventsCollection = null;

    // see also http://www.mongodb.org/display/DOCS/Connections
    private String uri = null;

    /**
     * If appender starts, create a new MongoDB connection and authenticate
     * user. A MongoDB database and collection in {@link #setUri(String)} is
     * mandatory, username and password are optional.
     */
    @Override
    public void start() {
        try {
            if (uri == null) {
                addError("Please set a non-null MongoDB URI.");
                return;
            }
            MongoURI mongoURI = new MongoURI(uri);
            String database = mongoURI.getDatabase();
            String collection = mongoURI.getCollection();
            if (database == null || collection == null) {
                addError("Error connecting to MongoDB URI: " + uri + " must contain a database and a collection."
                        + " E.g. mongodb://localhost/database.collection");
                return;
            }
            mongo = new Mongo(mongoURI);
            DB db = mongo.getDB(database);
            String username = mongoURI.getUsername();
            char[] password = mongoURI.getPassword();
            if (username != null && password != null)
                db.authenticate(username, password);
            eventsCollection = db.getCollection(collection);
            super.start();
        } catch (Exception exception) {
            addError("Error connecting to MongoDB URI: " + uri, exception);
        }
    }

    /**
     * Inserts a new MongoDB document representing {@code eventObject} into
     * MongoDB database.
     * 
     * @param event
     *            a logging event, containing all log data
     */
    @Override
    protected void append(E event) {
        eventsCollection.insert(toMongoDocument(event));
    }

    /**
     * Creates a new MongoDB document {@link BasicDBObject} from a logging
     * event, containing all log data.
     * 
     * @param event
     *            a logging event, containing all log data
     * @return a {@link BasicDBObject} to be inserted into MongoDB
     */
    protected abstract BasicDBObject toMongoDocument(E event);

    /**
     * If appender stops, close also the MongoDB connection.
     */
    @Override
    public void stop() {
        if (mongo != null)
            mongo.close();
        super.stop();
    }

    /**
     * A uri contains all MongoDB connection data.
     * 
     * @param uri
     *            <a href="http://www.mongodb.org/display/DOCS/Connections">a
     *            MongoDB URI</a>
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

}

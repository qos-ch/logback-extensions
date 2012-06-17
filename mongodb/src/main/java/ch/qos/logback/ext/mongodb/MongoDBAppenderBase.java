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

    @Override
    protected void append(E eventObject) {
        eventsCollection.insert(toMongoDocument(eventObject));
    }

    protected abstract BasicDBObject toMongoDocument(E event);

    @Override
    public void stop() {
        if (mongo != null)
            mongo.close();
        super.stop();
    }

    /**
     * @param uri
     *            a MongoDB URI, see also
     *            http://www.mongodb.org/display/DOCS/Connections
     */
    public void setURI(String uri) {
        this.uri = uri;
    }

}

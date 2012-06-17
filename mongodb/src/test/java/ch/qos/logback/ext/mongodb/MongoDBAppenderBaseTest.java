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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

/**
 * @author Christian Trutz
 * @since 0.1
 */
public class MongoDBAppenderBaseTest {

    // to be tested
    private MongoDBAppenderBase<ILoggingEvent> appender;

    @Test
    public void testNullURI() {
        // when uri == null
        appender.setURI(null);
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testNotValidURI() {
        // when uri is not valid
        appender.setURI("notvalid");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testNullDatabase() {
        // when uri does not contain a database
        appender.setURI("mongodb://server");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testNullCollection() {
        // when uri does not contain a collection
        appender.setURI("mongodb://server/database");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testDatabaseAndCollectionOK() {
        // when uri is valid and complete
        appender.setURI("mongodb://server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
    }

    @Test
    public void testPasswordNull() {
        // when uri does not contain password but username
        appender.setURI("mongodb://username@server/database.collection");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    // TODO is empty username in MongoDB allowed?
    @Test
    public void testEmptyUsername() {
        // when uri contains empty username
        appender.setURI("mongodb://:password@server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
        new Verifications() {
            {
                db.authenticate("", "password".toCharArray());
            }
        };
    }

    // TODO is empty password in MongoDB allowed?
    @Test
    public void testEmptyPassword() {
        // when uri contains empty password
        appender.setURI("mongodb://username:@server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
        new Verifications() {
            {
                db.authenticate("username", "".toCharArray());
            }
        };
    }

    @Test
    public void testUsernameAndPasswordOK() {
        // when uri contains username and password
        appender.setURI("mongodb://username:password@server/database.collection");
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
        new Verifications() {
            {
                db.authenticate("username", "password".toCharArray());
            }
        };
    }

    @Test
    public void testAppendOK() {
        // when calling doAppend()
        appender.setURI("mongodb://username:password@server/database.collection");
        appender.start();
        appender.doAppend(event);
        // then invoke collection.insert(...)
        new Verifications() {
            {
                collection.insert(dbObject);
            }
        };
    }

    @Test
    public void testStop() {
        // when calling stop()
        appender.setURI("mongodb://username:password@server/database.collection");
        appender.start();
        appender.doAppend(event);
        appender.stop();
        // then close MongoDB connection and stop appender
        new Verifications() {
            {
                mongo.close();
            }
        };
        assertFalse(appender.isStarted());
    }

    //
    //
    // MOCKING
    //

    @Mocked
    private Mongo mongo;
    @Mocked
    private DB db;
    @Mocked
    private DBCollection collection;
    @Mocked
    private ILoggingEvent event;

    // this object will be inserted in MongoDB and represents an logging event
    private BasicDBObject dbObject = new BasicDBObject();

    @BeforeMethod
    public void before() {
        appender = new MongoDBAppenderBase<ILoggingEvent>() {
            @Override
            protected BasicDBObject toMongoDocument(ILoggingEvent event) {
                return dbObject;
            }
        };
        new NonStrictExpectations() {
            {
                mongo.getDB("database");
                result = db;
                db.getCollection("collection");
                result = collection;
            }
        };
    }
}

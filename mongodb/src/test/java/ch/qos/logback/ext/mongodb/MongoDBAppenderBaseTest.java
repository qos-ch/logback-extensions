/*
 * Copyright 2012 Christian Trutz, et. al.
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

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.mongodb.BasicDBObject;
import static org.testng.Assert.*;

/**
 * @author Christian Trutz
 * @since 0.1
 */
public class MongoDBAppenderBaseTest {

    private MongoDBAppenderBase<ILoggingEvent> appender = null;

    @BeforeTest
    public void before() {
        appender = new MongoDBAppenderBase<ILoggingEvent>() {
            @Override
            protected BasicDBObject toMongoDocument(ILoggingEvent event) {
                return null;
            }
        };
    }

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

}

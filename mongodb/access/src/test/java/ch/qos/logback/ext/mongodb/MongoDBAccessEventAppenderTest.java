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

import static org.junit.Assert.assertEquals;

import java.util.Date;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.qos.logback.access.spi.IAccessEvent;

import com.mongodb.BasicDBObject;

/**
 * Tests for {@link MongoDBAccessEventAppender}.
 * 
 * @author Tomasz Nurkiewicz
 * @author Christian Trutz
 * @since 0.1
 */
@RunWith(JMockit.class)
public class MongoDBAccessEventAppenderTest {

    // to be tested
    private MongoDBAccessEventAppender appender = null;

    @Test
    public void testTimeStamp() {
        // given
        new NonStrictExpectations() {
            {
                event.getTimeStamp();
                result = 1000L;
            }
        };
        // when
        final BasicDBObject dbObject = appender.toMongoDocument(event);
        // then
        assertEquals(new Date(1000L), dbObject.get("timeStamp"));
    }

    @Test
    public void testServerName() {
        // given
        new NonStrictExpectations() {
            {
                event.getServerName();
                result = "servername";
            }
        };
        // when
        final BasicDBObject dbObject = appender.toMongoDocument(event);
        // then
        assertEquals("servername", dbObject.get("serverName"));
    }

    //
    //
    // MOCKING
    //

    @Mocked
    private IAccessEvent event;

    @Before
    public void before() {
        appender = new MongoDBAccessEventAppender();
    }

}

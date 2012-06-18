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

import junit.framework.Assert;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import ch.qos.logback.access.spi.AccessContext;

/**
 * @author Tomasz Nurkiewicz
 * @author Christian Trutz
 * @since 0.1
 */
public class MongoDBAccessEventAppenderTest {

    private final MongoDBAccessEventAppender appender = new MongoDBAccessEventAppender();
    private final AccessContext ac = new AccessContext();

    @BeforeTest
    public void setUp() {
        appender.setContext(ac);
        appender.start();
    }

    @AfterTest
    public void tearDown() {
        appender.stop();
    }

    @Test
    public void smokeTest() throws Exception {
    }

}

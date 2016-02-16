/**
 * Copyright (C) 2016 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.ext.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.ext.spring.web.WebLogbackConfigurer;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.mock.web.MockServletContext;

/**
 * Tests the behavior of the logging configuration if the configuration files are missing.
 * @author John Gibson
 */
public class MissingConfigurationTest {
    /**
     * Parameter specifying the location of the logback config file
     */
    private static final String CONFIG_LOCATION_PARAM = "logbackConfigLocation";

    private static Random rng = new Random();

    @ClassRule
    public static final TemporaryFolder PLAYGROUND = new TemporaryFolder();

    @Before
    public void clearFakeLogs() {
        FakeAppender.logs.clear();
    }

    /**
     * Aside from printing the test name this also ensures that the logging system is in the known default state.
     */
    @Rule
    public TestRule ensureLoggingInitialized = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            Logger log = LoggerFactory.getLogger(MissingConfigurationTest.class);
            log.info("Starting test: " + description.getMethodName());
        }
    };

    @After
    public void cleanupLogging() {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        loggerContext.reset();
    }

    @Test(timeout = 1000L)
    public void testMissingFileConfiguration() throws Exception {
        MockServletContext context = new MockServletContext();
        File missingConfig = null;
        do {
            missingConfig = new File("missingConfigTest" + rng.nextInt() + ".xml");
        } while (missingConfig.exists());

        context.addInitParameter(CONFIG_LOCATION_PARAM, missingConfig.toURI().toURL().toString());

        WebLogbackConfigurer.initLogging(context);
    }

    @Test
    public void testMissingClasspathConfiguration() throws Exception {
        MockServletContext context = new MockServletContext();
        context.addInitParameter(CONFIG_LOCATION_PARAM, "classpath:ch/qos/logback/ext/spring/does/not/exist/" + rng.nextInt() + ".xml");
        WebLogbackConfigurer.initLogging(context);
    }

    @Test
    public void testInvalidConfiguration() throws Exception {
        MockServletContext context = new MockServletContext();
        File empty = PLAYGROUND.newFile("empty.xml");
        context.addInitParameter(CONFIG_LOCATION_PARAM, empty.toURI().toURL().toString());
        try {
            WebLogbackConfigurer.initLogging(context);
            fail("Error expected.");
        } catch (RuntimeException e) {
            assertEquals("Unexpected error while configuring logback", e.getMessage());
            assertTrue("Expected a RuntimeException wrapping a JoranException, but it was a different cause: " + e.getCause(), e.getCause() instanceof JoranException);
        }
    }

    @Test(timeout = 1000L)
    public void testMissingFollowedByNormalConfiguration() throws Exception {
        MockServletContext context = new MockServletContext();
        File missingConfig = null;
        do {
            missingConfig = new File("missingConfigTest" + rng.nextInt() + ".xml");
        } while (missingConfig.exists());

        context.addInitParameter(CONFIG_LOCATION_PARAM, missingConfig.toURI().toURL().toString() + "," + getClass().getResource("fakeLogger.xml"));
        WebLogbackConfigurer.initLogging(context);
        Logger log = LoggerFactory.getLogger(MissingConfigurationTest.class);
        String key = "missingConfig" + rng.nextInt();
        log.error(key);
        boolean found = false;
        ArrayList<ILoggingEvent> logs;
        synchronized (FakeAppender.class) {
            logs = new ArrayList<ILoggingEvent>(FakeAppender.logs);
        }
        for (ILoggingEvent evt : logs) {
            if (key.equals(evt.getMessage())) {
                found = true;
            }
        }
        assertTrue("Logging event was not found: " + logs, found);
    }

    @Test(timeout = 1000L)
    public void testMissingFollowedByNormalClasspathConfiguration() throws Exception {
        MockServletContext context = new MockServletContext();
        File missingConfig = null;
        do {
            missingConfig = new File("missingConfigTest" + rng.nextInt() + ".xml");
        } while (missingConfig.exists());

        context.addInitParameter(CONFIG_LOCATION_PARAM, missingConfig.toURI().toURL().toString() + "," +
                "classpath:ch/qos/logback/ext/spring/fakeLogger.xml");
        WebLogbackConfigurer.initLogging(context);
        Logger log = LoggerFactory.getLogger(MissingConfigurationTest.class);
        String key = "missingConfig" + rng.nextInt();
        log.error(key);
        boolean found = false;
        ArrayList<ILoggingEvent> logs;
        synchronized (FakeAppender.class) {
            logs = new ArrayList<ILoggingEvent>(FakeAppender.logs);
        }
        for (ILoggingEvent evt : logs) {
            if (key.equals(evt.getMessage())) {
                found = true;
            }
        }
        assertTrue("Logging event was not found: " + logs, found);
    }

    public static class FakeAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        private static final ArrayList<ILoggingEvent> logs = new ArrayList<ILoggingEvent>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            synchronized(FakeAppender.class) {
                logs.add(eventObject);
            }
        }
    }
}

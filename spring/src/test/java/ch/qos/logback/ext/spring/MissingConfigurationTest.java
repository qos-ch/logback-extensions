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

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.ext.spring.web.WebLogbackConfigurer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Matchers;
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
import static org.junit.Assert.*;

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

    private LogSensitiveMockServletContext context = new LogSensitiveMockServletContext();

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
            LoggerContext loggerContext = doCleanup();
            BasicConfigurator.configure(loggerContext);
            Logger log = LoggerFactory.getLogger(MissingConfigurationTest.class);
            log.info("Starting test: " + description.getMethodName());
        }
    };

    private LoggerContext doCleanup() {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        loggerContext.reset();
        return loggerContext;
    }

    @After
    public void cleanupLogging() {
        doCleanup();
    }

    @Test(timeout = 3000L)
    public void testMissingFileConfiguration() throws Exception {
        File missingConfig = null;
        do {
            missingConfig = new File("missingConfigTest" + rng.nextInt() + ".xml");
        } while (missingConfig.exists());

        context.addInitParameter(CONFIG_LOCATION_PARAM, missingConfig.toURI().toURL().toString());

        WebLogbackConfigurer.initLogging(context);
        assertThat("Missing configuration file wasn't seen.", context.messages,
                Matchers.hasItem((Matchers.containsString(missingConfig.getName()))));
    }

    @Test
    public void testMissingClasspathConfiguration() throws Exception {
        final String fakeEntry = "classpath:ch/qos/logback/ext/spring/does/not/exist/" + rng.nextInt() + ".xml";
        context.addInitParameter(CONFIG_LOCATION_PARAM, fakeEntry);
        WebLogbackConfigurer.initLogging(context);
        assertThat("Missing configuration classpath wasn't seen.", context.messages,
                Matchers.hasItem((Matchers.containsString(fakeEntry))));
    }

    @Test
    public void testInvalidConfiguration() throws Exception {
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

    @Test(timeout = 3000L)
    public void testMissingFollowedByNormalConfiguration() throws Exception {
        File missingConfig = null;
        do {
            missingConfig = new File("missingConfigTest" + rng.nextInt() + ".xml");
        } while (missingConfig.exists());

        File fileConfig = PLAYGROUND.newFile("fakeLogger.xml");
        URL u = getClass().getResource("fakeLogger.xml");
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = u.openStream();
            os = new FileOutputStream(fileConfig);
            byte[] buff = new byte[2048];
            for (int read = is.read(buff); read != -1; read = is.read(buff)) {
                os.write(buff, 0, read);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }

        context.addInitParameter(CONFIG_LOCATION_PARAM, missingConfig.toURI().toURL().toString() + "," +
                fileConfig.toURI().toURL().toString());
        WebLogbackConfigurer.initLogging(context);
        assertThat("Missing configuration file wasn't seen.", context.messages,
                Matchers.hasItem((Matchers.containsString(missingConfig.getName()))));
        assertThat("Actual configuration file wasn't seen.", context.messages,
                Matchers.hasItem((Matchers.containsString(fileConfig.toURI().toURL().toString()))));

        Logger log = LoggerFactory.getLogger(MissingConfigurationTest.class);
        String key = "missingConfig" + rng.nextInt();
        log.error(key);
        ArrayList<ILoggingEvent> logs;
        synchronized (FakeAppender.class) {
            logs = new ArrayList<ILoggingEvent>(FakeAppender.logs);
        }
        assertThat(logs, Matchers.hasItem(new LoggingEventMatcher(key)));
    }

    @Test(timeout = 3000L)
    public void testMissingFollowedByNormalClasspathConfiguration() throws Exception {
        File missingConfig = null;
        do {
            missingConfig = new File("missingConfigTest" + rng.nextInt() + ".xml");
        } while (missingConfig.exists());

        String cpConfig = "classpath:ch/qos/logback/ext/spring/fakeLogger.xml";
        context.addInitParameter(CONFIG_LOCATION_PARAM, missingConfig.toURI().toURL().toString() + "," +
                cpConfig);
        WebLogbackConfigurer.initLogging(context);
        assertThat("Missing configuration file wasn't seen.", context.messages,
                Matchers.hasItem((Matchers.containsString(missingConfig.getName()))));
        assertThat("Actual configuration file wasn't seen.", context.messages,
                Matchers.hasItem((Matchers.containsString(cpConfig))));

        Logger log = LoggerFactory.getLogger(MissingConfigurationTest.class);
        String key = "missingConfig" + rng.nextInt();
        log.error(key);
        ArrayList<ILoggingEvent> logs;
        synchronized (FakeAppender.class) {
            logs = new ArrayList<ILoggingEvent>(FakeAppender.logs);
        }
        assertThat(logs, Matchers.hasItem(new LoggingEventMatcher(key)));
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

    private static class LoggingEventMatcher extends BaseMatcher<ILoggingEvent> {
        private final String message;

        public LoggingEventMatcher(String message) {
            this.message = message;
        }

        @Override
        public boolean matches(Object item) {
            if (!(item instanceof ILoggingEvent)) {
                return false;
            }
            return message.equals(((ILoggingEvent) item).getMessage());
        }

        @Override
        public void describeTo(org.hamcrest.Description description) {
            description.appendText("Logging event with message").appendValue(message);
        }
    }

    /**
     * Spring's MockServletContext class uses a logger to write messages. This interferes with testing the actual
     * logging system. In addition it makes it difficult to capture messages that we should expect to be logged to the
     * servlet.
     */
    private static class LogSensitiveMockServletContext extends MockServletContext {
        public final ArrayList<String> messages = new ArrayList<String>();
        private static final String PREFIX = "MockServlet: ";

        @Override
        public void log(String message) {
            System.out.println(PREFIX + message);
            messages.add(message);
        }

        @Override
        public void log(Exception ex, String message) {
            log(message, ex);
        }

        @Override
        public void log(String message, Throwable ex) {
            System.err.println(PREFIX + message);
            messages.add(message);
            System.err.println(PREFIX + ex.toString());
            messages.add(ex.toString());
            ex.printStackTrace();
        }
    }
}

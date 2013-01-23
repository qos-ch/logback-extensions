/*
 * Copyright 2012 Ceki Gulcu, Les Hazlewood, et. al.
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
package ch.qos.logback.ext.spring;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Convenience class that features simple methods for custom Log4J configuration.
 * <p/>
 * Only needed for non-default Logback initialization with a custom
 * config location. By default, Logback will simply read its
 * configuration from a "logback.xml" or "logback_test.xml" file in the root of the classpath.
 * <p/>
 * For web environments, the analogous LogbackWebConfigurer class can be found
 * in the web package, reading in its configuration from context-params in web.xml.
 * In a JEE web application, Logback is usually set up via LogbackConfigListener or
 * LogbackConfigServlet, delegating to LogbackWebConfigurer underneath.
 *
 * @author Juergen Hoeller
 * @author Bryan Turner
 * @author Les Hazlewood
 * @author Knute Axelson
 * @see ch.qos.logback.ext.spring.web.WebLogbackConfigurer WebLogbackConfigurer
 * @see ch.qos.logback.ext.spring.web.LogbackConfigListener LogbackConfigListener
 * @see ch.qos.logback.ext.spring.web.LogbackConfigServlet LogbackConfigServlet
 * @since 0.1
 */
public class LogbackConfigurer {

    private LogbackConfigurer() {
    }

    /**
     * Initialize logback from the given file.
     *
     * @param location the location of the config file: either a "classpath:" location
     *                 (e.g. "classpath:logback.xml"), an absolute file URL
     *                 (e.g. "file:C:/logback.xml), or a plain absolute path in the file system
     *                 (e.g. "C:/logback.xml")
     * @throws java.io.FileNotFoundException if the location specifies an invalid file path
     * @throws ch.qos.logback.core.joran.spi.JoranException
     *                                       Thrown
     */
    public static void initLogging(String location) throws FileNotFoundException, JoranException {
        String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
        URL url = ResourceUtils.getURL(resolvedLocation);
        LoggerContext loggerContext = (LoggerContext)StaticLoggerBinder.getSingleton().getLoggerFactory();

        // in the current version logback automatically configures at startup the context, so we have to reset it
        loggerContext.reset();

        // reinitialize the logger context.  calling this method allows configuration through groovy or xml
        new ContextInitializer(loggerContext).configureByResource(url);
    }

    /**
     * Set the specified system property to the current working directory.
     * <p/>
     * This can be used e.g. for test environments, for applications that leverage
     * LogbackWebConfigurer's "webAppRootKey" support in a web environment.
     *
     * @param key system property key to use, as expected in Logback configuration
     *            (for example: "demo.root", used as "${demo.root}/WEB-INF/demo.log")
     * @see ch.qos.logback.ext.spring.web.WebLogbackConfigurer WebLogbackConfigurer
     */
    public static void setWorkingDirSystemProperty(String key) {
        System.setProperty(key, new File("").getAbsolutePath());
    }

    /**
     * Shut down Logback.
     * <p/>
     * This isn't strictly necessary, but recommended for shutting down
     * logback in a scenario where the host VM stays alive (for example, when
     * shutting down an application in a J2EE environment).
     */
    public static void shutdownLogging() {
        ContextSelector selector = ContextSelectorStaticBinder.getSingleton().getContextSelector();
        LoggerContext loggerContext = selector.getLoggerContext();
        String loggerContextName = loggerContext.getName();
        LoggerContext context = selector.detachLoggerContext(loggerContextName);
        context.reset();
    }
}

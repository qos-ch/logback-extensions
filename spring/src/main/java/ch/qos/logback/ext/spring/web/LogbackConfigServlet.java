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
package ch.qos.logback.ext.spring.web;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Bootstrap servlet for custom Logback initialization in a web environment.
 * Delegates to LogbackWebConfigurer (see its javadoc for configuration details).
 * <p/>
 * <b>WARNING: Assumes an expanded WAR file</b>, both for loading the configuration
 * file and for writing the log files. If you want to keep your WAR unexpanded or
 * don't need application-specific log files within the WAR directory, don't use
 * Logback setup within the application (thus, don't use LogbackConfigListener or
 * LogbackConfigServlet). Instead, use a global, VM-wide Logback setup (for example,
 * in JBoss) or JDK 1.4's <code>java.util.logging</code> (which is global too).
 * <p/>
 * Note: This servlet should have a lower <code>load-on-startup</code> value
 * in <code>web.xml</code> than ContextLoaderServlet, when using custom Logback
 * initialization.
 * <p/>
 * <i>Note that this class has been deprecated for containers implementing
 * Servlet API 2.4 or higher, in favor of LogbackConfigListener.</i><br>
 * According to Servlet 2.4, listeners must be initialized before load-on-startup
 * servlets. Many Servlet 2.3 containers already enforce this behavior
 * (see ContextLoaderServlet javadocs for details). If you use such a container,
 * this servlet can be replaced with LogbackConfigListener. Else or if working
 * with a Servlet 2.2 container, stick with this servlet.
 *
 * @author Juergen Hoeller
 * @author Les Hazlewood
 * @see WebLogbackConfigurer
 * @see LogbackConfigListener
 * @since 0.1
 */
public class LogbackConfigServlet extends HttpServlet {

    @Override
    public void init() {
        WebLogbackConfigurer.initLogging(getServletContext());
    }

    @Override
    public void destroy() {
        WebLogbackConfigurer.shutdownLogging(getServletContext());
    }

    @Override
    public String getServletInfo() {
        return "LogbackConfigServlet for Servlet API 2.2/2.3 " +
                "(deprecated in favor of LogbackConfigListener for Servlet API 2.4+)";
    }

    /**
     * This should never even be called since no mapping to this servlet should
     * ever be created in web.xml. That's why a correctly invoked Servlet 2.3
     * listener is much more appropriate for initialization work ;-)
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        getServletContext().log(
                "Attempt to call service method on LogbackConfigServlet as [" +
                        request.getRequestURI() + "] was ignored");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}

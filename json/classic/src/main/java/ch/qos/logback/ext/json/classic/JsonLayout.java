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

package ch.qos.logback.ext.json.classic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.ext.json.JsonLayoutBase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A JsonLayout builds its {@link #toJsonMap(ch.qos.logback.classic.spi.ILoggingEvent) jsonMap} from a
 * source {@link ch.qos.logback.classic.spi.ILoggingEvent ILoggingEvent} with the following keys/value pairs:
 * <p/>
 * <table>
 *     <tr>
 *         <th nowrap="nowrap">Key</th>
 *         <th nowrap="nowrap">Value</th>
 *         <th nowrap="nowrap">Notes</th>
 *         <th nowrap="nowrap">Enabled by default?</th>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code timestamp}</td>
 *         <td nowrap="nowrap">String value of <code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getTimeStamp() getTimeStamp()}</code></td>
 *         <td>By default, the value is not formatted; it is simply {@code String.valueOf(timestamp)}.  To format
 *         the string using a SimpleDateFormat, set the {@link #setTimestampFormat(String) timestampFormat}
 *         property with the corresponding SimpleDateFormat string, for example, {@code yyyy-MM-dd HH:mm:ss.SSS}</td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code level}</td>
 *         <td nowrap="nowrap">String value of <code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getLevel() getLevel()}</code></td>
 *         <td><code>String.valueOf(event.getLevel());</code></td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code thread}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getThreadName() getThreadName()}</code></td>
 *         <td></td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code mdc}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getMDCPropertyMap() getMDCPropertyMap()}</code></td>
 *         <td>Unlike the other values which are all Strings, this value is a {@code Map&lt;String,String&gt;}.  If there is no
 *         MDC, this property will not be added to the JSON map.</td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code thread}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getLoggerName() getLoggerName()}</code></td>
 *         <td></td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code message}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getFormattedMessage() getFormattedMessage()}</code></td>
 *         <td>This is the <em>formatted</em> message.  The raw (unformatted) message is available as {@code raw-message}.
 *         Most people will want the formatted message as the raw message does not reflect any log message arguments.</td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code raw-message}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getMessage() getMessage()}</code></td>
 *         <td></td>
 *         <td>false</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code exception}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getThrowableProxy() getThrowableProxy()}</code></td>
 *         <td>If there is no exception, this property will not be added to the JSON map.  If there is an exception, it
 *             will be formatted to a String first via a {@link ch.qos.logback.classic.pattern.ThrowableProxyConverter ThrowableProxyConverter}.</td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td nowrap="nowrap">{@code context}</td>
 *         <td nowrap="nowrap"><code>ILoggingEvent.{@link ch.qos.logback.classic.spi.ILoggingEvent#getLoggerContextVO() getLoggerContextVO()}</code></td>
 *         <td>The name of the logger context. Defaults to <em>default</em>.</td>
 *         <td>true</td>
 *     </tr>
 * </table>
 * <p/>
 * The constructed Map will be serialized to JSON via the parent class's {@link #getJsonFormatter() jsonFormatter}.
 *
 * @author Les Hazlewood
 * @author Pierre Queinnec
 * @since 0.1
 */
public class JsonLayout extends JsonLayoutBase<ILoggingEvent> {

    public static final String TIMESTAMP_ATTR_NAME = "timestamp";
    public static final String LEVEL_ATTR_NAME = "level";
    public static final String THREAD_ATTR_NAME = "thread";
    public static final String MDC_ATTR_NAME = "mdc";
    public static final String LOGGER_ATTR_NAME = "logger";
    public static final String FORMATTED_MESSAGE_ATTR_NAME = "message";
    public static final String MESSAGE_ATTR_NAME = "raw-message";
    public static final String EXCEPTION_ATTR_NAME = "exception";
    public static final String CONTEXT_ATTR_NAME = "context";

    protected boolean includeLevel;
    protected boolean includeThreadName;
    protected boolean includeMDC;
    protected boolean includeLoggerName;
    protected boolean includeFormattedMessage;
    protected boolean includeMessage;
    protected boolean includeException;
    protected boolean includeContextName;

    private final ThrowableProxyConverter throwableProxyConverter;

    public JsonLayout() {
        super();
        this.includeLevel = true;
        this.includeThreadName = true;
        this.includeMDC = true;
        this.includeLoggerName = true;
        this.includeFormattedMessage = true;
        this.includeException = true;
        this.includeContextName = true;
        this.throwableProxyConverter = new ThrowableProxyConverter();
    }

    @Override
    public void start() {
        this.throwableProxyConverter.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        this.throwableProxyConverter.stop();
    }

    @Override
    protected Map toJsonMap(ILoggingEvent event) {

        Map<String, Object> map = new LinkedHashMap<String, Object>();

        if (this.includeTimestamp) {
            long timestamp = event.getTimeStamp();
            String formatted = formatTimestamp(timestamp);
            if (formatted != null) {
                map.put(TIMESTAMP_ATTR_NAME, formatted);
            }
        }

        if (this.includeLevel) {
            Level level = event.getLevel();
            if (level != null) {
                String lvlString = String.valueOf(level);
                map.put(LEVEL_ATTR_NAME, lvlString);
            }
        }

        if (this.includeThreadName) {
            String threadName = event.getThreadName();
            if (threadName != null) {
                map.put(THREAD_ATTR_NAME, threadName);
            }
        }

        if (this.includeMDC) {
            Map<String, String> mdc = event.getMDCPropertyMap();
            if ((mdc != null) && !mdc.isEmpty()) {
                map.put(MDC_ATTR_NAME, mdc);
            }
        }

        if (this.includeLoggerName) {
            String loggerName = event.getLoggerName();
            if (loggerName != null) {
                map.put(LOGGER_ATTR_NAME, loggerName);
            }
        }

        if (this.includeFormattedMessage) {
            String msg = event.getFormattedMessage();
            if (msg != null) {
                map.put(FORMATTED_MESSAGE_ATTR_NAME, msg);
            }
        }

        if (this.includeMessage) {
            String msg = event.getMessage();
            if (msg != null) {
                map.put(MESSAGE_ATTR_NAME, msg);
            }
        }

        if (this.includeContextName) {
            String msg = event.getLoggerContextVO().getName();
            if (msg != null) {
                map.put(CONTEXT_ATTR_NAME, msg);
            }
        }

        if (this.includeException) {
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                String ex = throwableProxyConverter.convert(event);
                if (ex != null && !ex.equals("")) {
                    map.put(EXCEPTION_ATTR_NAME, ex);
                }
            }
        }

        return map;
    }

    public boolean isIncludeLevel() {
        return includeLevel;
    }

    public void setIncludeLevel(boolean includeLevel) {
        this.includeLevel = includeLevel;
    }

    public boolean isIncludeLoggerName() {
        return includeLoggerName;
    }

    public void setIncludeLoggerName(boolean includeLoggerName) {
        this.includeLoggerName = includeLoggerName;
    }

    public boolean isIncludeFormattedMessage() {
        return includeFormattedMessage;
    }

    public void setIncludeFormattedMessage(boolean includeFormattedMessage) {
        this.includeFormattedMessage = includeFormattedMessage;
    }

    public boolean isIncludeMessage() {
        return includeMessage;
    }

    public void setIncludeMessage(boolean includeMessage) {
        this.includeMessage = includeMessage;
    }

    public boolean isIncludeMDC() {
        return includeMDC;
    }

    public void setIncludeMDC(boolean includeMDC) {
        this.includeMDC = includeMDC;
    }

    public boolean isIncludeThreadName() {
        return includeThreadName;
    }

    public void setIncludeThreadName(boolean includeThreadName) {
        this.includeThreadName = includeThreadName;
    }

    public boolean isIncludeException() {
        return includeException;
    }

    public void setIncludeException(boolean includeException) {
        this.includeException = includeException;
    }

    public boolean isIncludeContextName() {
        return includeContextName;
    }

    public void setIncludeContextName(boolean includeContextName) {
        this.includeContextName = includeContextName;
    }
}

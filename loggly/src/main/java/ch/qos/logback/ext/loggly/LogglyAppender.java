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
package ch.qos.logback.ext.loggly;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An Appender that posts logging messages to <a href="http://www.loggly.com">Loggly</a>, a cloud logging service.
 *
 * @author Mårten Gustafson
 * @author Les Hazlewood
 * @since 0.1
 */
public class LogglyAppender<E> extends UnsynchronizedAppenderBase<E> {

    public static final String DEFAULT_ENDPOINT_PREFIX = "https://logs.loggly.com/inputs/";

    public static final String DEFAULT_LAYOUT_PATTERN = "%d{\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\",UTC} %-5level [%thread] %logger: %m%n";

    private String endpointUrl;
    private String inputKey;

    private Layout<E> layout;
    private boolean layoutCreatedImplicitly = false;
    private String pattern;

    public LogglyAppender() {
    }

    @Override
    protected void append(E eventObject) {
        String msg = this.layout.doLayout(eventObject);
        postToLoggly(msg);
    }

    private void postToLoggly(final String event) {
        try {
            assert endpointUrl != null;
            URL endpoint = new URL(endpointUrl);
            final HttpURLConnection connection = (HttpURLConnection)endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", this.layout.getContentType());
            connection.connect();
            sendAndClose(event, connection.getOutputStream());
            connection.disconnect();
            final int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                final String message = readResponseBody(connection.getInputStream());
                addError("Loggly post failed (HTTP " + responseCode + ").  Response body:\n" + message);
            }
        } catch (final IOException e) {
            addError("IOException while attempting to communicate with Loggly", e);
        }
    }

    private String readResponseBody(final InputStream input) throws IOException {
        try {
            final byte[] bytes = toBytes(input);
            return new String(bytes, "UTF-8");
        } finally {
            input.close();
        }
    }

    private byte[] toBytes(final InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        byte[] buf = new byte[512];

        while((count = is.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, count);
        }
        baos.flush();

        return baos.toByteArray();
    }

    private void sendAndClose(final String event, final OutputStream output) throws IOException {
        try {
            output.write(event.getBytes("UTF-8"));
        } finally {
            output.close();
        }
    }

    protected final void ensureLayout() {
        if (this.layout == null) {
            this.layout = createLayout();
            this.layoutCreatedImplicitly = true;
        }
        if (this.layout != null) {
            Context context = this.layout.getContext();
            if (context == null) {
                this.layout.setContext(getContext());
            }
        }
    }

    protected Layout<E> createLayout() {
        PatternLayout layout = new PatternLayout();
        String pattern = getPattern();
        if (pattern == null) {
            pattern = DEFAULT_LAYOUT_PATTERN;
        }
        layout.setPattern(pattern);
        return (Layout<E>) layout;
    }

    protected String buildEndpointUrl(String inputKey) {
        return new StringBuilder(DEFAULT_ENDPOINT_PREFIX).append(inputKey).toString();
    }

    @Override
    public void start() {
        ensureLayout();
        if (!this.layout.isStarted()) {
            this.layout.start();
        }
        if (this.endpointUrl == null) {
            if (this.inputKey == null) {
                addError("inputKey (or alternatively, endpointUrl) must be configured");
            } else {
                this.endpointUrl = buildEndpointUrl(this.inputKey);
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.layoutCreatedImplicitly) {
            try {
                this.layout.stop();
            } finally {
                this.layout = null;
                this.layoutCreatedImplicitly = false;
            }
        }
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getInputKey() {
        return inputKey;
    }

    public void setInputKey(String inputKey) {
        String cleaned = inputKey;
        if (cleaned != null) {
            cleaned = cleaned.trim();
        }
        if ("".equals(cleaned)) {
            cleaned = null;
        }
        this.inputKey = cleaned;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Layout<E> getLayout() {
        return layout;
    }

    public void setLayout(Layout<E> layout) {
        this.layout = layout;
    }
}


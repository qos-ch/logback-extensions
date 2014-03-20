/**
 * Copyright (C) 2014 The logback-extensions developers (logback-user@qos.ch)
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
package ch.qos.logback.ext.loggly;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;

/**
 * Common base for Loggly appenders.
 *
 * @author Mårten Gustafson
 * @author Les Hazlewood
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public abstract class AbstractLogglyAppender<E> extends UnsynchronizedAppenderBase<E> {
    public static final String DEFAULT_ENDPOINT_PREFIX = "https://logs-01.loggly.com/";
    public static final String DEFAULT_LAYOUT_PATTERN = "%d{\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\",UTC} %-5level [%thread] %logger: %m%n";
    protected static final Charset UTF_8 = Charset.forName("UTF-8");
    protected String endpointUrl;
    protected String inputKey;
    protected Layout<E> layout;
    protected boolean layoutCreatedImplicitly = false;
    private String pattern;

    private int proxyPort;
    private String proxyHost;
    protected Proxy proxy;
    private int httpReadTimeoutInMillis = 1000;

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

        if (this.proxyHost == null || this.proxyHost.isEmpty()) {
            // don't set it to Proxy.NO_PROXY (i.e. Proxy.Type.DIRECT) as the meaning is different (user-jvm-proxy-config vs. don't use proxy)
            this.proxy = null;
        } else {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
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

    protected byte[] toBytes(final InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        byte[] buf = new byte[512];

        while((count = is.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, count);
        }
        baos.flush();

        return baos.toByteArray();
    }

    protected String readResponseBody(final InputStream input) throws IOException {
        try {
            final byte[] bytes = toBytes(input);
            return new String(bytes, UTF_8);
        } finally {
            input.close();
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
        return new StringBuilder(DEFAULT_ENDPOINT_PREFIX).append(getEndpointPath())
        		.append(inputKey).toString();
    }
    
    /**
     * The standard HTTP endpoint uses a different URL path than the bulk HTTP endpoint
     */
    protected abstract String getEndpointPath();

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

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    public void setProxyPort(String proxyPort) {
        if(proxyPort == null || proxyPort.trim().isEmpty()) {
            // handle logback configuration default value like "<proxyPort>${logback.loggly.proxy.port:-}</proxyPort>"
            proxyPort = "0";
        }
        this.proxyPort = Integer.parseInt(proxyPort);
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        if(proxyHost == null || proxyHost.trim().isEmpty()) {
            // handle logback configuration default value like "<proxyHost>${logback.loggly.proxy.host:-}</proxyHost>"
            proxyHost = null;
        }
        this.proxyHost = proxyHost;
    }

    public int getHttpReadTimeoutInMillis() {
        return httpReadTimeoutInMillis;
    }

    public void setHttpReadTimeoutInMillis(int httpReadTimeoutInMillis) {
        this.httpReadTimeoutInMillis = httpReadTimeoutInMillis;
    }
}

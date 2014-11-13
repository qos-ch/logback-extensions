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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

/**
 * An Appender that posts logging messages to <a href="http://www.loggly.com">Loggly</a>, a cloud logging service.
 *
 * @author Mårten Gustafson
 * @author Les Hazlewood
 * @since 0.1
 */
public class LogglyAppender<E> extends AbstractLogglyAppender<E> {

    public static final String ENDPOINT_URL_PATH = "inputs/";
    
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
            final HttpURLConnection connection;
            if (proxy == null) {
                connection = (HttpURLConnection) endpoint.openConnection();
            } else {
                connection = (HttpURLConnection) endpoint.openConnection(proxy);
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", this.layout.getContentType());
			connection.addRequestProperty("X-Forwarded-For", InetAddress.getLocalHost().getHostAddress());
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

    private void sendAndClose(final String event, final OutputStream output) throws IOException {
        try {
            output.write(event.getBytes("UTF-8"));
        } finally {
            output.close();
        }
    }

    @Override
    protected String getEndpointPrefix() {
        return ENDPOINT_URL_PATH;
    }
}


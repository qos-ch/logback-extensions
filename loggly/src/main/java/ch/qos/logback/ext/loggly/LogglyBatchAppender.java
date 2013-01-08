/*
 * Copyright 2012-2013 Ceki Gulcu, Les Hazlewood and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.ext.loggly;

import ch.qos.logback.ext.loggly.io.DiscardingRollingOutputStream;
import ch.qos.logback.ext.loggly.io.IoUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * <a href="http://logback.qos.ch/">Logback</a> batch appender for <a href="http://www.loggly.com/">Loggly</a> HTTP API</a>.
 * </p>
 * <p><strong>Note:</strong>Loggly's Syslog API is much more scalable then the HTTP API which should mostly used
 * to low volume or non-production systems. The HTTP API can be very convenient to workaround firewalls.</p>
 * <p>If the {@link LogglyBatchAppender} saturates and discards log messages, the following warning message is
 * appended to both Loggly and {@link System#err}: <br/>
 * "<code>$date - OutputStream is full, discard previous logs</code>"</p>
 * <h2>Configuration settings</h2>
 * <table>
 * <tr>
 * <th>Property Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>inputKey</td>
 * <td>String</td>
 * <td>Loggly input key. "<code>inputKey</code>" or <code>endpointUrl</code> is required. Sample
 * "<code>12345678-90ab-cdef-1234-567890abcdef</code>"</td>
 * </tr>
 * <tr>
 * <td>endpointUrl</td>
 * <td>String</td>
 * <td>Loggly HTTP API endpoint URL. "<code>inputKey</code>" or <code>endpointUrl</code> is required. Sample:
 * "<code>https://logs.loggly.com/inputs/12345678-90ab-cdef-1234-567890abcdef</code>"</td>
 * </tr>
 * <tr>
 * <td>pattern</td>
 * <td>String</td>
 * <td>Pattern used for Loggly log messages. Default value is:
 * <code>%d{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",UTC} %-5level [%thread] %logger: %m%n</code>.</td>
 * </tr>
 * <tr>
 * <td>proxyHost</td>
 * <td>String</td>
 * <td>hostname of a proxy server. If blank, no proxy is used (See {@link URL#openConnection(java.net.Proxy)}.</td>
 * </tr>
 * <tr>
 * <td>proxyPort</td>
 * <td>int</td>
 * <td>port a proxy server. blank string defaults to 0 to ease
 * <a href="http://logback.qos.ch/manual/configuration.html#variableSubstitution">property substitution</a> in <code>logback.xml</code> (See {@link URL#openConnection(java.net.Proxy)}.</td>
 * </tr>
 * <tr>
 * <td>jmxMonitoring</td>
 * <td>boolean</td>
 * <td>Enable registration of a monitoring MBean named
 * "<code>ch.qos.logback:type=LogglyBatchAppender,name=LogglyBatchAppender@#hashcode#</code>". Default: <code>true</code>.</td>
 * </tr>
 * <tr>
 * <td>maxNumberOfBuckets</td>
 * <td>int</td>
 * <td>Max number of buckets of in the bytes buffer. Default value: <code>8</code>.</td>
 * </tr>
 * <tr>
 * <td>maxBucketSizeInKoBytes</td>
 * <td>int</td>
 * <td>Max size of each bucket. Default value: <code>1024</code>.</td>
 * </tr>
 * <tr>
 * <td>flushIntervalInSeconds</td>
 * <td>int</td>
 * <td>Interval of the buffer flush to Loggly API. Default value: <code>3</code>.</td>
 * </tr>
 * </table>
 * Default configuration consumes up to 8 buffers of 1Mo each which seemed very reasonable even for small JVMs.
 * If logs are discarded, try first to shorten the <code>flushIntervalInSeconds</code> parameter to "2s" or event "1s".
 * <p/>
 * <h2>Configuration Sample</h2>
 * <pre><code>
 * &lt;configuration scan="true" scanPeriod="30 seconds" debug="true"&gt;
 *   &lt;if condition='isDefined("logback.loggly.inputKey")'&gt;
 *     &lt;then&gt;
 *       &lt;appender name="loggly" class="ch.qos.logback.ext.loggly.LogglyBatchAppender"&gt;
 *         &lt;inputKey&gt;${logback.loggly.inputKey}&lt;/inputKey&gt;
 *         &lt;pattern&gt;%d{yyyy/MM/dd HH:mm:ss,SSS} [${HOSTNAME}] [%thread] %-5level %logger{36} - %m %throwable{5}%n&lt;/pattern&gt;
 *         &lt;proxyHost&gt;${logback.loggly.proxy.host:-}&lt;/proxyHost&gt;
 *         &lt;proxyPort&gt;${logback.loggly.proxy.port:-}&lt;/proxyPort&gt;
 *         &lt;debug&gt;${logback.loggly.debug:-false}&lt;/debug&gt;
 *       &lt;/appender&gt;
 *       &lt;root level="WARN"&gt;
 *         &lt;appender-ref ref="loggly"/&gt;
 *       &lt;/root&gt;
 *     &lt;/then&gt;
 *   &lt;/if&gt;
 * &lt;/configuration&gt;
 * </code></pre>
 * </p>
 * <p/>
 * <h2>Implementation decisions</h2>
 * <ul>
 * <li>Why buffering the generated log messages as bytes instead of using the
 * {@link ch.qos.logback.core.read.CyclicBufferAppender} and buffering the {@link ch.qos.logback.classic.spi.ILoggingEvent} ?
 * Because it is much more easy to control the size in memory</li>
 * <li>
 * Why buffering in a byte array instead of directly writing in a {@link BufferedOutputStream} on the {@link HttpURLConnection} ?
 * Because the Loggly API may not like such kind of streaming approach.
 * </li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class LogglyBatchAppender<E> extends AbstractLogglyAppender<E> implements LogglyBatchAppenderMBean {

    private static boolean debug = false;

    public static void log(String message, Object... args) {
        if (!debug) {
            return;
        }
        try {
            String formattedMessage = String.format(message, args);
            System.err.println(new Timestamp(System.currentTimeMillis()) + " - LogglyBatchAppender [" + Thread.currentThread().getName() + "] " + formattedMessage);
        } catch (Exception e) {
            System.err.println(new Timestamp(System.currentTimeMillis()) + " - LogglyBatchAppender [" + Thread.currentThread().getName() + "] " + message + " - " + Arrays.asList(args));
            e.printStackTrace();
        }
    }

    private int flushIntervalInSeconds = 3;

    private DiscardingRollingOutputStream outputStream;

    protected final AtomicLong sendDurationInNanos = new AtomicLong();

    protected final AtomicLong sentBytes = new AtomicLong();

    protected final AtomicInteger sendSuccessCount = new AtomicInteger();

    protected final AtomicInteger sendExceptionCount = new AtomicInteger();

    private ScheduledExecutorService scheduledExecutor;

    private boolean jmxMonitoring = true;

    private MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    private ObjectName registeredObjectName;

    private int maxNumberOfBuckets = 8;

    private int maxBucketSizeInKoBytes = 1024;

    private Charset charset = Charset.forName("ISO-8859-1");

    @Override
    protected void append(E eventObject) {
        if (!isStarted()) {
            return;
        }
        String msg = this.layout.doLayout(eventObject);
        try {
            outputStream.write(msg.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {

        // OUTPUTSTREAM
        outputStream = new DiscardingRollingOutputStream(
                maxBucketSizeInKoBytes * 1024,
                maxNumberOfBuckets) {
            @Override
            protected void onBucketDiscard(ByteArrayOutputStream discardedBucket) {
                LogglyBatchAppender.log("Discard bucket - %s", this);
                String s = new Timestamp(System.currentTimeMillis()) + " - OutputStream is full, discard previous logs" + LINE_SEPARATOR;
                try {
                    getFilledBuckets().peekLast().write(s.getBytes());
                    addWarn(s);
                } catch (IOException e) {
                    addWarn("Exception appending warning message '" + s + "'", e);
                }
            }

            @Override
            protected void onBucketRoll(ByteArrayOutputStream rolledBucket) {
                LogglyBatchAppender.log("Roll bucket - %s", this);
            }

        };

        // SCHEDULER
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName("logback-loggly-appender");
                thread.setDaemon(true);
                return thread;
            }
        };
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutor.scheduleWithFixedDelay(new LogglyExporter(), flushIntervalInSeconds, flushIntervalInSeconds, TimeUnit.SECONDS);

        // MONITORING
        if (jmxMonitoring) {
            String objectName = "ch.qos.logback:type=LogglyBatchAppender,name=LogglyBatchAppender@" + System.identityHashCode(this);
            try {
                registeredObjectName = mbeanServer.registerMBean(this, new ObjectName(objectName)).getObjectName();
            } catch (Exception e) {
                addWarn("Exception registering mbean '" + objectName + "'", e);
            }
        }

        // super.setOutputStream() must be defined before calling super.start()
        super.start();
    }

    @Override
    public void stop() {
        scheduledExecutor.shutdown();

        processLogEntries();

        if (registeredObjectName != null) {
            try {
                mbeanServer.unregisterMBean(registeredObjectName);
            } catch (Exception e) {
                addWarn("Exception unRegistering mbean " + registeredObjectName, e);
            }
        }

        try {
            scheduledExecutor.awaitTermination(2 * this.flushIntervalInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            addWarn("Exception waiting for termination of LogglyAppender scheduler", e);
        }

        // stop appender (ie close outputStream) after sending it to Loggly
        outputStream.close();

        super.stop();
    }

    /**
     * Send log entries to Loggly
     */
    @Override
    public void processLogEntries() {
        log("processLogEntries() %s", this);

        outputStream.rollCurrentBucketIfNotEmpty();
        BlockingDeque<ByteArrayOutputStream> filledBuckets = outputStream.getFilledBuckets();

        ByteArrayOutputStream bucket;

        while ((bucket = filledBuckets.poll()) != null) {
            try {
                InputStream in = new ByteArrayInputStream(bucket.toByteArray());
                processLogEntries(in);
            } catch (Exception e) {
                addWarn("Internal error", e);
            }
            outputStream.recycleBucket(bucket);
        }
    }


    /**
     * Send log entries to Loggly
     */
    protected void processLogEntries(InputStream in) throws IOException {
        long nanosBefore = System.nanoTime();
        try {

            URL url = new URL(endpointUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",  layout.getContentType() + "; charset=" + charset.name());
            conn.setRequestMethod("POST");
            conn.setReadTimeout((int) TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS));
            BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());

            long len = IoUtils.copy(in, out);
            sentBytes.addAndGet(len);

            out.flush();
            out.close();

            int responseCode = conn.getResponseCode();
            String response = super.readResponseBody(conn.getInputStream());
            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_ACCEPTED:
                    sendSuccessCount.incrementAndGet();
                    break;
                default:
                    sendExceptionCount.incrementAndGet();
                    addError("LogglyAppender server-side exception: " + responseCode + ": " + response);
            }
            // force url connection recycling
            try {
                conn.getInputStream().close();
                conn.disconnect();
            } catch (Exception e) {
                // swallow exception
            }
        } catch (Exception e) {
            sendExceptionCount.incrementAndGet();
            addError("LogglyAppender client-side exception", e);
        } finally {
            sendDurationInNanos.addAndGet(System.nanoTime() - nanosBefore);
        }
    }

    public int getFlushIntervalInSeconds() {
        return flushIntervalInSeconds;
    }

    public void setFlushIntervalInSeconds(int flushIntervalInSeconds) {
        this.flushIntervalInSeconds = flushIntervalInSeconds;
    }

    @Override
    public long getSentBytes() {
        return sentBytes.get();
    }

    @Override
    public long getSendDurationInNanos() {
        return sendDurationInNanos.get();
    }

    @Override
    public int getSendSuccessCount() {
        return sendSuccessCount.get();
    }

    @Override
    public int getSendExceptionCount() {
        return sendExceptionCount.get();
    }

    @Override
    public int getDiscardedBucketsCount() {
        return outputStream.getDiscardedBucketCount();
    }

    @Override
    public long getCurrentLogEntriesBufferSizeInBytes() {
        return outputStream.getCurrentOutputStreamSize();
    }

    public void setDebug(boolean debug) {
        LogglyBatchAppender.debug = debug;
    }

    public boolean isDebug() {
        return LogglyBatchAppender.debug;
    }

    public void setJmxMonitoring(boolean jmxMonitoring) {
        this.jmxMonitoring = jmxMonitoring;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public void setMaxNumberOfBuckets(int maxNumberOfBuckets) {
        this.maxNumberOfBuckets = maxNumberOfBuckets;
    }

    public void setMaxBucketSizeInKoBytes(int maxBucketSizeInKoBytes) {
        this.maxBucketSizeInKoBytes = maxBucketSizeInKoBytes;
    }

    @Override
    public String toString() {
        return "LogglyBatchAppender{" +
                "sendDurationInMillis=" + TimeUnit.MILLISECONDS.convert(sendDurationInNanos.get(), TimeUnit.NANOSECONDS) +
                ", sendSuccessCount=" + sendSuccessCount +
                ", sendExceptionCount=" + sendExceptionCount +
                ", sentBytes=" + sentBytes +
                ", discardedBucketsCount=" + getDiscardedBucketsCount() +
                ", currentLogEntriesBufferSizeInBytes=" + getCurrentLogEntriesBufferSizeInBytes() +
                '}';
    }

    public class LogglyExporter implements Runnable {
        @Override
        public void run() {
            try {
                processLogEntries();
            } catch (Exception e) {
                addWarn("Exception processing log entries", e);
            }
        }
    }
}

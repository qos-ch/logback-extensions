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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.layout.EchoLayout;

@RunWith(MockitoJUnitRunner.class)
public class LogglySendOnlyWhenMaxBucketsFullTest {
  static private final int                   PORT           = 10800;
  static private final int                   MAX_BUCKETS    = 4;
  static private final int                   BUCKET_KB_SIZE = 1;
  static private final int                   MSG_SIZE       = BUCKET_KB_SIZE * 1024;
  static private LogglyBatchAppender<String> appender;
  static private LoggerContext               context;
  private ByteArrayOutputStream              byteOutputStream;
  private ByteArrayInputStream               byteInputStream;

  @Mock
  HttpURLConnection connection;

  /**
   * Creates a context with a status listener
   */
  @BeforeClass
  static public void beforeClass() {
    context = new LoggerContext();
  }

  /**
   * Shuts down the appender's processing thread
   */
  @AfterClass
  static public void afterClass() {
    appender.stop();
  }

  @Before
  public void before() throws UnknownHostException {
    byteOutputStream = new ByteArrayOutputStream();
    byteInputStream = new ByteArrayInputStream(new byte[0]);

    appender = new LogglyBatchAppenderWithMockConnection();
    appender.setContext(context);
    appender.setEndpointUrl("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT + "/");

    appender.setLayout(new EchoLayout<String>());
    appender.setDebug(true);
    appender.setMaxBucketSizeInKilobytes(BUCKET_KB_SIZE);
    appender.setMaxNumberOfBuckets(MAX_BUCKETS);
    appender.start();

    assertNoTrafficYet();

  }

  @After
  public void after() {
    System.out.println("BYTES ------>");
    System.out.println(byteOutputStream.toString());
  }

  @Test
  public void successCountMatchesSentCount() throws Exception {
    sendMessagesAndConfirmRx(MAX_BUCKETS);
    assertEquals(MAX_BUCKETS, appender.getSendSuccessCount());
  }

  @Test
  public void rxCountMatchesSentCount() throws Exception {
    sendMessagesAndConfirmRx(MAX_BUCKETS);
    assertEquals(MAX_BUCKETS, getRxMessageCount());
  }

  /**
   * Mirrors LogglyBatchAppender but uses a mock HTTP connection to
   * feed byte streams that we can control. Also calls notifyAll()
   * after log entries are processed.
   */
  private class LogglyBatchAppenderWithMockConnection extends LogglyBatchAppender<String> {

    @Override
    protected HttpURLConnection getHttpConnection(URL url) throws IOException {
      when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
      when(connection.getOutputStream()).thenReturn(new BufferedOutputStream(byteOutputStream));
      when(connection.getInputStream()).thenReturn(new BufferedInputStream(byteInputStream));
      return connection;
    }

    @Override
    protected void processLogEntries(InputStream in) throws IOException {
      super.processLogEntries(in);
      synchronized(appender) {
        notifyAll();
      }
    }
  }

  /**
   * Verifies that no messages have been sent or received yet
   */
  private void assertNoTrafficYet() {
    assertEquals(0, appender.getSendSuccessCount());
    assertEquals(0, getRxMessageCount());
  }

  /**
   * Sends bucket-full messages to a remote server (which is just a mock connection)
   * and confirms delivery (by checking for method calls in the mock connection)
   *
   * @param count number of full buckets to send
   * @throws IOException
   * @throws InterruptedException
   */
  private void sendMessagesAndConfirmRx(int count) throws IOException, InterruptedException {
    appendFullBuckets(count);

    for (int i = 0; i < count; i++) {
      synchronized(appender) {
        appender.wait(10000);
      }
    }

    verify(connection, atLeast(MAX_BUCKETS)).getOutputStream();
    verify(connection, atLeast(MAX_BUCKETS)).getInputStream();
    verify(connection, atLeast(MAX_BUCKETS)).disconnect();
  }

  /**
   * Gets the number of messages "received"
   * @return the message count
   */
  private int getRxMessageCount() {
    String str = byteOutputStream.toString();
    return str.isEmpty() ? 0 : str.split("\n").length;
  }

  /**
   * Fills the appender's buckets with max-length messages
   * @param count number of buckets to fill
   */
  private void appendFullBuckets(int count) {
    for (int i = 0; i < count; i++) {
      appender.doAppend(i + ")" + new String(new char[MSG_SIZE]).replace("\0", "X"));
    }
  }

}

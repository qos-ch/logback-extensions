/*
 * Copyright 2012-2013 Ceki Gulcu, Les Hazlewood, et. al.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.layout.EchoLayout;
import ch.qos.logback.core.status.OnConsoleStatusListener;

/**
 * Tests the LogglyBatchAppender
 */
@Ignore
public class LogglyBatchAppenderTest {

  static private final int                   PORT           = 10800;
  static private final int                   MAX_BUCKETS    = 4;
  static private final int                   BUCKET_KB_SIZE = 1;
  static private final int                   MSG_SIZE       = BUCKET_KB_SIZE * 1024;
  static private HttpTestServer              httpServer;
  static private LogglyBatchAppender<String> appender;
  static private LoggerContext               context;

  /**
   * Starts the HTTP test server, initializes the appender,
   * and creates a context with a status listener
   * @throws IOException
   */
  @BeforeClass
  static public void beforeClass() throws IOException {
    httpServer = new HttpTestServer(PORT);
    httpServer.start();
    context = new LoggerContext();
    OnConsoleStatusListener.addNewInstanceToContext(context);
  }

  /**
   * Shuts down the HTTP test server
   */
  @AfterClass
  static public void afterClass() {
    httpServer.stop();
    appender.stop();
  }

  @Before
  public void before() throws UnknownHostException {
    httpServer.clearRequests();

    appender = new LogglyBatchAppender<String>();
    appender.setContext(context);
    appender.setEndpointUrl("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT + "/");

    appender.setLayout(new EchoLayout<String>());
    appender.setDebug(true);
    appender.setMaxBucketSizeInKilobytes(BUCKET_KB_SIZE);
    appender.setMaxNumberOfBuckets(MAX_BUCKETS);
    appender.start();
  }

  @Test
  public void starts() {
    assertTrue(appender.isStarted());
  }

  private void appendFullBuckets(int count) {
    for (int i = 0; i < count; i++) {
      appender.doAppend(new String(new char[MSG_SIZE]).replace("\0", "X"));
    }
  }

  @Test(timeout = 180000)
  public void sendsOnlyWhenMaxBucketsFull() {
    // assert nothing yet sent/received
    assertEquals(0, appender.getSendSuccessCount());
    assertEquals(0, httpServer.requestCount());

    // send stuff and wait for it to be received
    appendFullBuckets(MAX_BUCKETS);
    httpServer.waitForRequests(MAX_BUCKETS);

    // assert stuff sent/received
    assertEquals(MAX_BUCKETS, appender.getSendSuccessCount());
    assertEquals(MAX_BUCKETS, httpServer.requestCount());
  }

  @Test(timeout = 180000)
  public void excessBucketsGetDiscarded() {
    // assert nothing yet discarded (because nothing is yet sent)
    assertEquals(0, appender.getDiscardedBucketsCount());

    // send stuff and wait for it to be received
    final int NUM_MSGS = 40;
    appendFullBuckets(NUM_MSGS);
    httpServer.waitForRequests(MAX_BUCKETS);

    // assert excess buckets (those > MAX_BUCKETS) were discarded
    assertEquals(NUM_MSGS - MAX_BUCKETS, appender.getDiscardedBucketsCount());
  }
}

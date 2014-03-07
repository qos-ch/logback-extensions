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
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * HTTP test server that tracks the number of requests received
 */
public class HttpTestServer implements Container {
  static private final int MAX_RXBUF_SIZE = 2048;
  private int              port;
  private Connection       connection;
  private Server           server;
  AtomicInteger            numRequests;

  /**
   * Initializes the HTTP server
   * @param port
   */
  public HttpTestServer(int port) {
    this.port = port;
    numRequests = new AtomicInteger(0);
  }

  /**
   * Opens the HTTP server
   * @throws IOException
   */
  public void start() throws IOException {
    stop();
    this.server = new ContainerServer(this);
    this.connection = new SocketConnection(server);
    SocketAddress address = new InetSocketAddress(this.port);
    this.connection.connect(address);
  }

  /**
   * Closes the HTTP server
   */
  public void stop() {
    if (this.connection != null) {
      try {
        this.connection.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * Gets the number of requests received
   * @return the request count
   */
  public int requestCount() {
    return numRequests.get();
  }

  /**
   * Resets the request count
   */
  public void clearRequests() {
    numRequests.set(0);
  }

  /**
   * Waits indefinitely for the specified number of requests to be received
   * @param count the number of received requests to wait for
   */
  public void waitForRequests(int count) {
    waitForRequests(count, 0, 0);
  }

  /**
   * Waits for the specified number of requests to be received
   * @param count the number of received requests to wait for
   * @param interval the wait time between polls, checking the receive count;
   * use 0 to wait indefinitely
   * @param maxPolls the maximum number of polls; use 0 for no limit
   */
  public void waitForRequests(int count, long interval, int maxPolls) {
    synchronized (this) {
      while (requestCount() < count) {
        System.out.println("requests " + requestCount() + "/" + count);
        if (maxPolls > 0 && maxPolls-- > 0) {
          break;
        }
        try {
          this.wait(interval);
        } catch (InterruptedException e) {
        }
      }
      System.out.println("requests " + requestCount() + "/" + count);
    }
  }

  /**
   * Handles incoming HTTP requests by responding with the
   * message index and size of the received message.
   * @see org.simpleframework.http.core.Container#handle(org.simpleframework.http.Request, org.simpleframework.http.Response)
   */
  @Override
  public void handle(Request request, Response response) {
    try {
      PrintStream body = response.getPrintStream();
      long time = System.currentTimeMillis();

      response.setValue("Content-Type", "text/html");
      response.setValue("Server", "HttpTestServer/1.0 (Simple 4.0)");
      response.setDate("Date", time);
      response.setDate("Last-Modified", time);

      ByteBuffer buf = ByteBuffer.allocate(MAX_RXBUF_SIZE);
      int len = request.getByteChannel().read(buf);
      int count = numRequests.incrementAndGet();

      // warn if RX buffer exceeded (message truncated)
      String warning = "";
      if (len > MAX_RXBUF_SIZE) {
        warning = "(" + (len - MAX_RXBUF_SIZE) + " bytes truncated)";
      }

      body.println("Request #" + count + "\n" + len + " bytes read" + warning);
      body.close();

      synchronized (this) {
        notify();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
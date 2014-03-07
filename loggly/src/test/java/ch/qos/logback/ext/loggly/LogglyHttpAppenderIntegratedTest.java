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

import ch.qos.logback.core.layout.EchoLayout;
import ch.qos.logback.ext.loggly.io.IoUtils;
import de.svenjacobs.loremipsum.LoremIpsum;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class LogglyHttpAppenderIntegratedTest {

    public static void main(String[] args) throws Exception {

        Random random = new Random();
        LoremIpsum loremIpsum = new LoremIpsum();

        String file = "/tmp/loggly-appender-test-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".log";
        System.out.println("Generate " + file);
        final OutputStream out = new FileOutputStream(file);

        LogglyBatchAppender<String> appender = new LogglyBatchAppender<String>() {
            @Override
            protected void processLogEntries(InputStream in) throws IOException {
                // super.processLogEntries(in);
                IoUtils.copy(in, out);
            }
        };
        appender.setInputKey("YOUR LOGGLY INPUT KEY");
        appender.setLayout(new EchoLayout<String>());
        appender.setDebug(true);

        // Start
        appender.start();

        assertTrue("Appender failed to start", appender.isStarted());

        appender.doAppend("# Test " + new Timestamp(System.currentTimeMillis()));

        for (int i = 0; i < 100000; i++) {
            appender.doAppend(i + " -- " + new Timestamp(System.currentTimeMillis()) + " - " + loremIpsum.getWords(random.nextInt(50), random.nextInt(50)));
            TimeUnit.MILLISECONDS.sleep(random.nextInt(30));
            if (i % 100 == 0) {
                System.out.println(i + " - " + appender);
            }
        }
        // stop
        appender.stop();

        out.close();
        System.out.println(appender);
    }

}

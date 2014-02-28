/*
 * Copyright 2012-2014 Ceki Gulcu, Les Hazlewood, Jonathan Fritz, et. al.
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
package ch.qos.logback.ext.loggly.io;

import java.io.IOException;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for the {@link DiscardingRollingOutputStream} size and peek
 * functions.
 * 
 * @author MusikPolice (jonfritz@gmail.com)
 */
public class DiscardingRollingOutputStreamTest {

    private static final int maxBucketSizeInBytes = 90;
    private static final int maxBucketCount = 5;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Ensures that the buffer size calculation works as expected
     * 
     * @throws IOException
     */
    @Test
    public void bufferSizeCalculationTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 300 bytes of random data in 10 byte packets
        // this should fill three buckets and put another 30 bytes into the
        // active bucket
        Random r = new Random();
        for (int i = 0; i < 30; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            outputStream.write(data);
        }

        // make sure that the size of the output stream comes back as 300 bytes
        Assert.assertEquals(300, outputStream.size());
        outputStream.close();
    }

    /**
     * Ensures that an exception is thrown if we try to peek past the end of the
     * stream
     */
    @Test
    public void bufferPeekEmptyThrowsExceptionTest() {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        thrown.expect(IllegalArgumentException.class);
        outputStream.peek(100);
        outputStream.close();
    }

    /**
     * Ensures that an exception is thrown if we try to peek past the start of
     * the stream
     */
    @Test
    public void bufferPeekNegativeThrowsExceptionTest() {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        outputStream.peek(-1);
        outputStream.close();
    }

    /**
     * Ensures that the peek operation succeeds when it looks in the last filled
     * bucket
     */
    @Test
    public void bufferPeekFilledBucketsTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 90 bytes of random data in 10 byte packets
        // this should cause the active bucket to be empty when we perform the
        // peek op
        byte byte90 = 0;
        Random r = new Random();
        for (int i = 0; i < 9; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            if (i == 8) {
                byte90 = data[9];
            }

            outputStream.write(data);
        }

        // peek operation is zero based, so look at the 89th byte
        Assert.assertEquals(byte90, outputStream.peek(89));
        outputStream.close();
    }

    /**
     * Ensures that the peek operation succeeds when it looks in the active
     * bucket
     */
    @Test
    public void bufferPeekActiveBucketTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 110 bytes of random data in 10 byte packets
        // this should cause the active bucket to contain 20 bytes when we do
        // the peek op
        byte byte110 = 0;
        Random r = new Random();
        for (int i = 0; i < 11; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            if (i == 10) {
                byte110 = data[9];
            }

            outputStream.write(data);
        }

        // peek operation is zero based, so look at the 109th byte
        Assert.assertEquals(byte110, outputStream.peek(109));
        outputStream.close();
    }

    /**
     * Ensures that an exception is thrown if we try to peek past the end of the
     * stream
     */
    @Test
    public void bufferPeekFirstWhileEmptyThrowsExceptionTest() {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        outputStream.peekFirst();
        outputStream.close();
    }

    /**
     * Ensures that the peekFirst operation succeeds when it looks in the last
     * filled bucket
     * 
     * @throws IOException
     */
    @Test
    public void bufferPeekFirstSucceedsOnFilledBucketTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 90 bytes of random data in 10 byte packets
        // this should cause the active bucket to be empty when we perform the
        // peek op
        byte byte0 = 0;
        Random r = new Random();
        for (int i = 0; i < 9; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            if (i == 0) {
                byte0 = data[0];
            }

            outputStream.write(data);
        }

        Assert.assertEquals(byte0, outputStream.peekFirst());
        outputStream.close();
    }

    /**
     * Ensures that the peekFirst operation succeeds when it looks in the active
     * bucket
     * 
     * @throws IOException
     */
    @Test
    public void bufferPeekFirstSucceedsOnActiveBucketTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 80 bytes of random data in 10 byte packets
        // this should cause the active bucket to contain 80 bytes when we do
        // the peek op
        byte byte0 = 0;
        Random r = new Random();
        for (int i = 0; i < 8; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            if (i == 0) {
                byte0 = data[0];
            }

            outputStream.write(data);
        }

        Assert.assertEquals(byte0, outputStream.peekFirst());
        outputStream.close();
    }

    /**
     * Ensures that an exception is thrown if we try to peek past the end of the
     * stream
     */
    @Test
    public void bufferPeekLastWhenEmptyThrowsExceptionTest() {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        outputStream.peekLast();
        outputStream.close();
    }

    /**
     * Ensures that the peekLast operation succeeds when it looks in the last
     * filled bucket
     * 
     * @throws IOException
     */
    @Test
    public void bufferPeekLastWhenFilledBucketSucceedsTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 90 bytes of random data in 10 byte packets
        // this should cause the active bucket to be empty when we perform the
        // peek op
        byte byte89 = 0;
        Random r = new Random();
        for (int i = 0; i < 9; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            if (i == 8) {
                byte89 = data[9];
            }

            outputStream.write(data);
        }

        Assert.assertEquals(byte89, outputStream.peekLast());
        outputStream.close();
    }

    /**
     * Ensures that the peekLast operation succeeds when it looks in the active
     * bucket
     * 
     * @throws IOException
     */
    @Test
    public void bufferPeekLastWhenActiveBucketFilledSucceedsTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);

        // fill the buffer with 110 bytes of random data in 10 byte packets
        // this should cause the active bucket to contain 20 bytes when we do
        // the peek op
        byte byte109 = 0;
        Random r = new Random();
        for (int i = 0; i < 11; i++) {
            byte[] data = new byte[10];
            r.nextBytes(data);
            if (i == 10) {
                byte109 = data[9];
            }

            outputStream.write(data);
        }

        Assert.assertEquals(byte109, outputStream.peekLast());
        outputStream.close();
    }

    /**
     * Ensures that isEmpty functions as designed
     * 
     * @throws IOException
     */
    @Test
    public void isEmptyReturnsTrueWhenBufferEmptyTest() throws IOException {
        // an empty stream should return true
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        Assert.assertTrue(outputStream.isEmpty());
        outputStream.close();
    }

    /**
     * Ensures that isEmpty functions as designed
     * 
     * @throws IOException
     */
    @Test
    public void isEmptyReturnsFalseWhenActiveBucketNotEmptyTest() throws IOException {
        // a single byte in the active buffer should return true
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        byte[] data = new byte[] { 1 };
        outputStream.write(data);
        Assert.assertFalse(outputStream.isEmpty());
        outputStream.close();
    }

    /**
     * Ensures that isEmpty functions as designed
     * 
     * @throws IOException
     */
    @Test
    public void isEmptyReturnsFalseWithFilledBucketAndEmptyActiveBucketTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        // add 90 bytes to the buffer so that we have one filled bucket and
        // an empty active bucket
        // should return false since we have some filled bucket
        Random r = new Random();
        byte[] data = new byte[90];
        r.nextBytes(data);
        outputStream.write(data);
        Assert.assertFalse(outputStream.isEmpty());
        outputStream.close();
    }

    /**
     * Ensures that isEmpty functions as designed
     * 
     * @throws IOException
     */
    @Test
    public void isEmptyReturnsFalseWithFilledBucketAndNonEmptyActiveBucketTest() throws IOException {
        DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes,
                maxBucketCount);
        // add another byte so that there's one byte in the active bucket as
        // well as a filled bucket
        // should return false since we have some filled bucket
        byte[] data = new byte[] { 1 };
        outputStream.write(data);
        Assert.assertFalse(outputStream.isEmpty());
        outputStream.close();
    }
}

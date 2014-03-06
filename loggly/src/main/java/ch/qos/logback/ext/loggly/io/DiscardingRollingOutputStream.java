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
package ch.qos.logback.ext.loggly.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Capped in-memory {@linkplain OutputStream} composed of a chain of {@linkplain ByteArrayOutputStream} called 'buckets'.
 * </p>
 * <p>
 * Each 'bucket' is limited in size (see {@link #maxBucketSizeInBytes}) and the total size of the {@linkplain OutputStream}
 * is bounded thanks to a discarding policy. An external component is expected to consume the filled buckets thanks to
 * {@link #getFilledBuckets()}.
 * </p>
 * <p>
 * Implementation decisions:
 * </p>
 * <ul>
 * <li>Why in-memory without offload on disk: offload on disk was possible with Google Guava's
 * <code>FileBackedOutputStream</code> but had the drawback to introduce a dependency. Loggly batch appender use case
 * should be OK with a pure in-memory approach.</li>
 * </ul>
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class DiscardingRollingOutputStream extends OutputStream {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private ByteArrayOutputStream currentBucket;

    private final ReentrantLock currentBucketLock = new ReentrantLock();

    private final BlockingDeque<ByteArrayOutputStream> filledBuckets;

    private final ConcurrentLinkedQueue<ByteArrayOutputStream> recycledBucketPool;

    private long maxBucketSizeInBytes;

    private final AtomicInteger discardedBucketCount = new AtomicInteger();

    /**
     * @param maxBucketSizeInBytes
     * @param maxBucketCount
     */
    public DiscardingRollingOutputStream(int maxBucketSizeInBytes, int maxBucketCount) {
        if (maxBucketCount < 2) {
            throw new IllegalArgumentException("'maxBucketCount' must be >1");
        }

        this.maxBucketSizeInBytes = maxBucketSizeInBytes;
        this.filledBuckets = new LinkedBlockingDeque<ByteArrayOutputStream>(maxBucketCount);

        this.recycledBucketPool = new ConcurrentLinkedQueue<ByteArrayOutputStream>();
        this.currentBucket = newBucket();
    }


    @Override
    public void write(int b) throws IOException {
        currentBucketLock.lock();
        try {
            currentBucket.write(b);
            rollCurrentBucketIfNeeded();
        } finally {
            currentBucketLock.unlock();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        currentBucketLock.lock();
        try {
            currentBucket.write(b);
            rollCurrentBucketIfNeeded();
        } finally {
            currentBucketLock.unlock();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        currentBucketLock.lock();
        try {
            currentBucket.write(b, off, len);
            rollCurrentBucketIfNeeded();
        } finally {
            currentBucketLock.unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        currentBucketLock.lock();
        try {
            currentBucket.flush();
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Close all the underlying buckets (current bucket, filled buckets and buckets from the recycled buckets pool).
     */
    @Override
    public void close() {
        // no-op as ByteArrayOutputStream#close() is no op
    }

    /**
     * Roll current bucket if size threshold has been reached.
     */
    private void rollCurrentBucketIfNeeded() {
        if (currentBucket.size() < maxBucketSizeInBytes) {
            return;
        }
        rollCurrentBucket();
    }

    /**
     * Roll current bucket if size threshold has been reached.
     */
    public void rollCurrentBucketIfNotEmpty() {
        if (currentBucket.size() == 0) {
            return;
        }
        rollCurrentBucket();
    }

    /**
     * Moves the current active bucket to the list of filled buckets and defines a new one.
     * <p/>
     * The new active bucket is reused from the {@link #recycledBucketPool} pool if one is available or recreated.
     */
    public void rollCurrentBucket() {
        currentBucketLock.lock();
        try {
            boolean offered = filledBuckets.offer(currentBucket);
            if (offered) {
                onBucketRoll(currentBucket);
            } else {
                onBucketDiscard(currentBucket);
                discardedBucketCount.incrementAndGet();
            }

            currentBucket = newBucket();
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Returns the current size of the buffer, including the active bucket and
     * all filled buckets.
     *
     * @return the current size of the buffer
     */
    public long size() {
        currentBucketLock.lock();
        try {
            long size = currentBucket.size();
            for (java.io.ByteArrayOutputStream b : filledBuckets) {
                size += b.size();
            }
            return size;
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Returns true if the buffer is currently empty. This implementation is
     * more efficient than size() > 0 if the number of buckets is large.
     *
     * @return true if the buffer is currently empty.
     */
    public boolean isEmpty() {
        currentBucketLock.lock();
        try {
            return currentBucket.size() == 0 && filledBuckets.size() == 0;
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Returns the byte at the specified position in the buffer.
     *
     * @param position
     *            the zero-based position of the byte to look at. The first byte
     *            is at position = 0, and the last byte is at position = size()
     *            -1
     * @return the byte at the specified position of the buffer
     */
    public byte peek(long position) {
        currentBucketLock.lock();
        try {
            // look for the target byte in the filled buckets
            for (java.io.ByteArrayOutputStream b : filledBuckets) {
                if (position >= b.size()) {
                    position -= b.size();
                } else {
                    return b.toByteArray()[(int) position];
                }
            }

            // look for the target byte in the active bucket
            if (position >= currentBucket.size()) {
                throw new IllegalArgumentException("Can't peek past the end of the stream.");
            } else {
                return currentBucket.toByteArray()[(int) position];
            }
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Returns the first byte in the buffer
     *
     * @throws ArrayIndexOutOfBoundsException
     *             if the buffer is empty
     */
    public byte peekFirst() {
        currentBucketLock.lock();
        try {
            if (filledBuckets.size() > 0) {
                return filledBuckets.peekFirst().toByteArray()[0];
            } else {
                return currentBucket.toByteArray()[0];
            }
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Returns the last byte in the buffer
     *
     * @throws ArrayIndexOutOfBoundsException
     *             if the buffer is empty
     */
    public byte peekLast() {
        currentBucketLock.lock();
        try {
            if (currentBucket.size() > 0) {
                return currentBucket.toByteArray()[currentBucket.size() - 1];
            } else if (filledBuckets.size() > 0) {
                return filledBuckets.peekLast().toByteArray()[filledBuckets.peekLast().size() - 1];
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        } finally {
            currentBucketLock.unlock();
        }
    }

    /**
     * Designed for extension.
     *
     * @param discardedBucket the discarded bucket
     */
    protected void onBucketDiscard(ByteArrayOutputStream discardedBucket) {

    }

    /**
     * The rolled bucket. Designed for extension.
     *
     * @param rolledBucket the discarded bucket
     */
    protected void onBucketRoll(ByteArrayOutputStream rolledBucket) {

    }

    /**
     * Get a new bucket from the {@link #recycledBucketPool} or instantiate a new one if none available
     * in the free bucket pool.
     *
     * @return the bucket ready to use
     */
    protected ByteArrayOutputStream newBucket() {
        ByteArrayOutputStream bucket = recycledBucketPool.poll();
        if (bucket == null) {
            bucket = new ByteArrayOutputStream();
        }
        return bucket;
    }

    /**
     * Returns the given bucket to the pool of free buckets.
     *
     * @param bucket the bucket to recycle
     */
    public void recycleBucket(ByteArrayOutputStream bucket) {
        bucket.reset();
        recycledBucketPool.offer(bucket);
    }

    /**
     * Return the filled buckets
     */
    public BlockingDeque<ByteArrayOutputStream> getFilledBuckets() {
        return filledBuckets;
    }

    /**
     * Number of discarded buckets. Monitoring oriented metric.
     */
    public int getDiscardedBucketCount() {
        return discardedBucketCount.get();
    }

    public long getCurrentOutputStreamSize() {
        long sizeInBytes = 0;
        for (ByteArrayOutputStream bucket : filledBuckets) {
            sizeInBytes += bucket.size();
        }
        sizeInBytes += currentBucket.size();
        return sizeInBytes;
    }

    @Override
    public String toString() {
        return "DiscardingRollingOutputStream{" +
                "currentBucket.bytesWritten=" + currentBucket.size() +
                ", filledBuckets.size=" + filledBuckets.size() +
                ", discardedBucketCount=" + discardedBucketCount +
                ", recycledBucketPool.size=" + recycledBucketPool.size() +
                '}';
    }
}

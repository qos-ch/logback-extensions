package ch.qos.logback.ext.loggly.io;

import java.io.IOException;
import java.util.Random;
import junit.framework.Assert;
import org.junit.Test;

public class DiscardingRollingOutputStreamTest {

	private static final int maxBucketSizeInBytes = 90;
	private static final int maxBucketCount = 5;

	/**
	 * Ensures that the buffer size calculation works as expected
	 * @throws IOException
	 */
	@Test
	public void bufferSizeTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 300 bytes of random data in 10 byte packets
		//this should fill three buckets and put another 30 bytes into the active bucket
		Random r = new Random();
		for (int i = 0; i < 30; i++) {
			byte[] data = new byte[10];
			r.nextBytes(data);
			outputStream.write(data);
		}

		//make sure that the size of the output stream comes back as 300 bytes
		Assert.assertEquals(300, outputStream.size());
		outputStream.close();
	}

	/**
	 * Ensures that an exception is thrown if we try to peek past the end of the stream
	 */
	@Test(expected = IllegalArgumentException.class)
	public void bufferPeekExceptionTest() {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);
		outputStream.peek(100);
		outputStream.close();
	}

	/**
	 * Ensures that the peek operation succeeds when it looks in the last filled bucket
	 */
	@Test
	public void bufferPeekFilledBucketsTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 90 bytes of random data in 10 byte packets
		//this should cause the active bucket to be empty when we perform the peek op
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

		//peek operation is zero based, so look at the 89th byte
		Assert.assertEquals(byte90, outputStream.peek(89));
		outputStream.close();
	}

	/**
	 * Ensures that the peek operation succeeds when it looks in the active bucket
	 */
	@Test
	public void bufferPeekActiveBucketTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 110 bytes of random data in 10 byte packets
		//this should cause the active bucket to contain 20 bytes when we do the peek op
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

		//peek operation is zero based, so look at the 109th byte
		Assert.assertEquals(byte110, outputStream.peek(109));
		outputStream.close();
	}

	/**
	 * Ensures that an exception is thrown if we try to peek past the end of the stream
	 */
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void bufferPeekFirstExceptionTest() {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);
		outputStream.peekFirst();
		outputStream.close();
	}

	/**
	 * Ensures that the peekFirst operation succeeds when it looks in the last filled bucket
	 * @throws IOException
	 */
	@Test
	public void bufferPeekFirstFilledBucketTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 90 bytes of random data in 10 byte packets
		//this should cause the active bucket to be empty when we perform the peek op
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
	 * Ensures that the peekFirst operation succeeds when it looks in the active bucket
	 * @throws IOException
	 */
	@Test
	public void bufferPeekFirstActiveBucketTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 80 bytes of random data in 10 byte packets
		//this should cause the active bucket to contain 80 bytes when we do the peek op
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
	 * Ensures that an exception is thrown if we try to peek past the end of the stream
	 */
	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void bufferPeekLastExceptionTest() {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);
		outputStream.peekLast();
		outputStream.close();
	}

	/**
	 * Ensures that the peekLast operation succeeds when it looks in the last filled bucket
	 * @throws IOException
	 */
	@Test
	public void bufferPeekLastFilledBucketTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 90 bytes of random data in 10 byte packets
		//this should cause the active bucket to be empty when we perform the peek op
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
	 * Ensures that the peekLast operation succeeds when it looks in the active bucket
	 * @throws IOException
	 */
	@Test
	public void bufferPeekLastActiveBucketTest() throws IOException {
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);

		//fill the buffer with 110 bytes of random data in 10 byte packets
		//this should cause the active bucket to contain 20 bytes when we do the peek op
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
	 * @throws IOException
	 */
	@Test
	public void isEmptyTest() throws IOException {
		//an empty stream should return true
		DiscardingRollingOutputStream outputStream = new DiscardingRollingOutputStream(maxBucketSizeInBytes, maxBucketCount);
		Assert.assertTrue(outputStream.isEmpty());

		//a single byte in the active buffer should return true
		byte[] data = new byte[] { 1 };
		outputStream.write(data);
		Assert.assertFalse(outputStream.isEmpty());

		//add 89 more bytes to the buffer so that we have one filled bucket and an empty active bucket
		//should return false since we have some filled bucket
		Random r = new Random();
		data = new byte[89];
		r.nextBytes(data);
		outputStream.write(data);
		Assert.assertFalse(outputStream.isEmpty());

		//add another byte so that there's one byte in the active bucket as well as a filled bucket
		//should return false since we have some filled bucket
		data = new byte[] { 1 };
		outputStream.write(data);
		Assert.assertFalse(outputStream.isEmpty());

		outputStream.close();
	}
}

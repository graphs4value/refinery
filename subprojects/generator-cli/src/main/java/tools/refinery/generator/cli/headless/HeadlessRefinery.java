/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.headless;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public abstract class HeadlessRefinery implements AutoCloseable {
	public static final int MAX_FRAME = 16 * 1024 * 1024;
	public static final String ENDPOINT_ENV_VAR = "REFINERY_IPC_ENDPOINT";
	public static final int TIMEOUT_MS = 5_000;

	private static final String WINDOWS_PIPE_PREFIX = "\\\\.\\pipe\\";

	public static HeadlessRefinery connect() throws IOException {
		var endpoint = System.getenv(ENDPOINT_ENV_VAR);
		if (endpoint == null) {
			throw new IllegalStateException(ENDPOINT_ENV_VAR + " is not configures.");
		}
		return connect(endpoint);
	}

	public static HeadlessRefinery connect(String endpoint) throws IOException {
		return connect(endpoint, TIMEOUT_MS);
	}

	public static HeadlessRefinery connect(String endpoint, long timeoutMillis) throws IOException {
		long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
		IOException last = null;
		while (true) {
			try {
				return endpoint.startsWith(WINDOWS_PIPE_PREFIX)
						? new NamedPipeClient(endpoint)
						: new UnixSocketClient(Path.of(endpoint));
			} catch (IOException e) {
				last = e;
			}
			if (System.nanoTime() - deadline >= 0) {
				throw new IOException("timed out connecting to " + endpoint, last);
			}
			try {
				// We're polling for the Refinery IP socket to come up.
				//noinspection BusyWait
				Thread.sleep(50);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new IOException("interrupted while connecting to " + endpoint, ie);
			}
		}
	}

	public byte[] interact(byte[] request) throws IOException {
		send(request);
		return receive();
	}

	private void send(byte[] payload) throws IOException {
		if (payload.length > MAX_FRAME) {
			throw new IOException("frame too large: " + payload.length);
		}
		// Header and body go out in a single write so a reader on the other end
		// never sees a torn frame from a partially flushed handle.
		byte[] frame = new byte[4 + payload.length];
		writeLength(frame, payload.length);
		System.arraycopy(payload, 0, frame, 4, payload.length);
		writeFully(frame);
	}

	static void writeLength(byte[] frame, int length) {
		frame[0] = (byte) (length >>> 24);
		frame[1] = (byte) (length >>> 16);
		frame[2] = (byte) (length >>> 8);
		frame[3] = (byte) length;
	}

	static long readLength(byte[] frame) {
		return ((long) (frame[0] & 0xFF) << 24)
				| ((long) (frame[1] & 0xFF) << 16)
				| ((long) (frame[2] & 0xFF) << 8)
				| (frame[3] & 0xFF);
	}

	/**
	 * @return the next payload, or {@code null} at a clean end of stream.
	 */
	private byte[] receive() throws IOException {
		byte[] header = new byte[4];
		int n = read(header, 0, 4);
		if (n < 0) {
			return null; // peer closed on a frame boundary
		}
		readFully(header, n, 4 - n);
		long length = ((long) (header[0] & 0xFF) << 24)
				| ((long) (header[1] & 0xFF) << 16)
				| ((long) (header[2] & 0xFF) << 8)
				| (header[3] & 0xFF);
		if (length > MAX_FRAME) {
			throw new IOException("frame too large: " + length);
		}
		byte[] payload = new byte[(int) length];
		readFully(payload, 0, payload.length);
		return payload;
	}

	private void readFully(byte[] b, int off, int len) throws IOException {
		int done = 0;
		while (done < len) {
			int n = read(b, off + done, len - done);
			if (n < 0) {
				throw new EOFException("peer closed mid-frame");
			}
			done += n;
		}
	}

	/**
	 * @return bytes read, or -1 at end of stream.
	 */
	protected abstract int read(byte[] b, int off, int len) throws IOException;

	protected abstract void writeFully(byte[] b) throws IOException;

	@Override
	public void close() throws IOException {
		// Narrow exception signature.
	}

	private static final class UnixSocketClient extends HeadlessRefinery {
		private final SocketChannel channel;

		UnixSocketClient(Path path) throws IOException {
			SocketChannel c = SocketChannel.open(StandardProtocolFamily.UNIX);
			try {
				c.connect(UnixDomainSocketAddress.of(path));
			} catch (IOException e) {
				try {
					c.close();
				} catch (IOException suppressed) {
					e.addSuppressed(suppressed);
				}
				throw e;
			}
			this.channel = c;
		}

		@Override
		protected int read(byte[] b, int off, int len) throws IOException {
			return channel.read(ByteBuffer.wrap(b, off, len));
		}

		@Override
		protected void writeFully(byte[] b) throws IOException {
			ByteBuffer buf = ByteBuffer.wrap(b);
			while (buf.hasRemaining()) {
				channel.write(buf);
			}
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}
	}

	private static final class NamedPipeClient extends HeadlessRefinery {
		private final RandomAccessFile pipe;

		NamedPipeClient(String path) throws IOException {
			this.pipe = new RandomAccessFile(path, "rw");
		}

		@Override
		protected int read(byte[] b, int off, int len) throws IOException {
			return pipe.read(b, off, len);
		}

		@Override
		protected void writeFully(byte[] b) throws IOException {
			pipe.write(b); // RandomAccessFile.write is already all-or-throw
		}

		@Override
		public void close() throws IOException {
			pipe.close();
		}
	}
}

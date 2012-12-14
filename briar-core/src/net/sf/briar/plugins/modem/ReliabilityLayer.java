package net.sf.briar.plugins.modem;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

class ReliabilityLayer implements ReadHandler, WriteHandler {

	private static final int TICK_INTERVAL = 500; // Milliseconds

	private static final Logger LOG =
			Logger.getLogger(ReliabilityLayer.class.getName());

	private final WriteHandler writeHandler;
	private final BlockingQueue<byte[]> writes;

	private volatile Receiver receiver = null;
	private volatile SlipDecoder decoder = null;
	private volatile ReceiverInputStream inputStream = null;
	private volatile SenderOutputStream outputStream = null;
	private volatile Thread writer = null;
	private volatile boolean running = false;

	ReliabilityLayer(WriteHandler writeHandler) {
		this.writeHandler = writeHandler;
		writes = new LinkedBlockingQueue<byte[]>();
	}

	void start() {
		SlipEncoder encoder = new SlipEncoder(this);
		final Sender sender = new Sender(encoder);
		receiver = new Receiver(sender);
		decoder = new SlipDecoder(receiver);
		inputStream = new ReceiverInputStream(receiver);
		outputStream = new SenderOutputStream(sender);
		writer = new Thread("ReliabilityLayer") {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				long next = now + TICK_INTERVAL;
				try {
					while(running) {
						byte[] b = null;
						while(now < next && b == null) {
							b = writes.poll(next - now, MILLISECONDS);
							now = System.currentTimeMillis();
						}
						if(b == null) {
							sender.tick();
							while(next <= now) next += TICK_INTERVAL;
						} else {
							if(b.length == 0) return; // Poison pill
							writeHandler.handleWrite(b);
						}
					}
				} catch(InterruptedException e) {
					if(LOG.isLoggable(WARNING))
						LOG.warning("Interrupted while writing");
					Thread.currentThread().interrupt();
					running = false;
				} catch(IOException e) {
					if(LOG.isLoggable(WARNING))
						LOG.log(WARNING, e.toString(), e);
					running = false;
				}
			}
		};
		running = true;
		writer.start();
	}

	InputStream getInputStream() {
		return inputStream;
	}

	OutputStream getOutputStream() {
		return outputStream;
	}

	void stop() {
		running = false;
		receiver.invalidate();
		writes.add(new byte[0]); // Poison pill
	}

	// The modem calls this method to pass data up to the SLIP decoder
	public void handleRead(byte[] b) throws IOException {
		if(!running) throw new IOException("Connection closed");
		decoder.handleRead(b);
	}

	// The SLIP encoder calls this method to pass data down to the modem
	public void handleWrite(byte[] b) throws IOException {
		if(!running) throw new IOException("Connection closed");
		if(b.length > 0) writes.add(b);
	}
}

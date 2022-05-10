package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class OctetsReader implements Reader {
	private enum State {
		DONE, WAITING_INT, WAITING_FILE, ERROR
	};

	private State state = State.WAITING_INT;
	byte[] buffer = null;
	private int size;
	private final IntReader intReader = new IntReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		try {
			switch (state) {
			case WAITING_INT: {
				var processBlockSize = intReader.process(bb);
				switch (processBlockSize) {
				case DONE:
					size = intReader.get();
					intReader.reset();
					state = State.WAITING_FILE;
					break;
				case REFILL:
					return ProcessStatus.REFILL;
				case ERROR:
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
				bb.flip();
			}
			case WAITING_FILE: {
				if(bb.remaining() >= size) {
					buffer = new byte[size];
					bb.get(buffer, 0, size);
					state = State.DONE;
					return ProcessStatus.DONE;
				}else {
					return ProcessStatus.REFILL;
				}
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + state);
			}
		} finally {
			bb.compact();
		}
	}

	@Override
	public byte[] get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return buffer;
	}

	public int getSize() {
		return size;
	}

	@Override
	public void reset() {
		state = State.WAITING_INT;
		buffer = null;
	}

}

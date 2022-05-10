package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class StringReader implements Reader<String> {
	private enum State {DONE, WAITING_FOR_SIZE, WAITING_FOR_CONTENT, ERROR}; 
    private State state = State.WAITING_FOR_SIZE;
    private final ByteBuffer buffer = ByteBuffer.allocate(1_024); // write-mode
    private String val;
    private int size = 0;
    private static final Charset UTF8 = Charset.forName("UTF8");
    private final IntReader intReader = new IntReader();

    @Override
    public ProcessStatus process(ByteBuffer buff) {
        switch(state) {
        	case WAITING_FOR_SIZE:
		        if(intReader.process(buff) == ProcessStatus.REFILL) {
		        	return ProcessStatus.REFILL;
		        }
		        size = intReader.get();
		        if(size < 0 || size > 1024) {
		        	return ProcessStatus.ERROR;
		        }
		        state = State.WAITING_FOR_CONTENT;
		        buffer.limit(size);
        	case WAITING_FOR_CONTENT:
	            buff.flip();
		        try {
		            if (buff.remaining() <= buffer.remaining()){
		            	buffer.put(buff);
		            } else {
		                var limit = buff.limit();
		                buff.limit(buffer.remaining());
		                buffer.put(buff);
		                buff.limit(limit);
		            }
		        } finally {
		            buff.compact();
		        }
		    	if(!buffer.hasRemaining()) {
		    		buffer.flip();
		        	val = UTF8.decode(buffer).toString();
		        	
		        	state = State.DONE;
		        	return ProcessStatus.DONE;
		        }
		    	return ProcessStatus.REFILL;
		    default:
		    	throw new IllegalStateException();
        }
    }

    @Override
    public String get() {
        if (state!= State.DONE) {
            throw new IllegalStateException();
        }
        return val;
    }

    @Override
    public void reset() {
    	intReader.reset();
        state= State.WAITING_FOR_SIZE;
        size = 0;
        val = null;
        buffer.clear();
    }

}
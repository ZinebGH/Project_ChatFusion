package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.entity.Login;
import fr.upem.net.tcp.nonblocking.entity.Message;

public class MessageReader implements Reader<Message>{
	 private enum State {DONE, WAITING_SERVER, WAITING_LOGIN, WAITING_MSG, ERROR};
	    private State state = State.WAITING_SERVER;
	    private Message value;
	    private String nameServer;
	    private Login login;
	    private String text;
	    private final StringReader stringReader = new StringReader();
	    private final LoginReader loginReader = new LoginReader();
	   

	    @Override
	    public ProcessStatus process(ByteBuffer buffer) {
	        if (state == State.DONE || state == State.ERROR) {
	            throw new IllegalStateException();
	        }
			if (state == State.WAITING_SERVER) {
				var processServer= stringReader.process(buffer);
				switch (processServer) {
				case DONE:
					nameServer = stringReader.get();
					stringReader.reset();
					state = State.WAITING_LOGIN;
					break;
				case REFILL:
					return ProcessStatus.REFILL;
				case ERROR:
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
			}
			if (state == State.WAITING_LOGIN) {
				var processLogin = loginReader.process(buffer);
				switch (processLogin) {
				case DONE:
					login = loginReader.get();
					
					loginReader.reset();
					state = State.WAITING_MSG;
					break;
				case REFILL:
					return ProcessStatus.REFILL;
				case ERROR:
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
			}
			if (state == State.WAITING_MSG) {
				var processMsg = stringReader.process(buffer);
				switch (processMsg) {
				case DONE:
					text = stringReader.get();
					stringReader.reset();
					state = State.DONE;
					break;
				case REFILL:
					return ProcessStatus.REFILL;
				case ERROR:
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
			}
	        value = new Message(login, text, nameServer);
			return ProcessStatus.DONE;
	    }

	    @Override
	    public Message get() {
	        if (state!= State.DONE) {
	            throw new IllegalStateException();
	        }
	        return value;
	    }

	    @Override
	    public void reset() {
	    	stringReader.reset();
	        state = State.WAITING_SERVER;
	    }
}
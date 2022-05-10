package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.entity.Login;
import fr.upem.net.tcp.nonblocking.entity.PrivateMessage;

public class PrivateMessageReader implements Reader<PrivateMessage> {

	private enum State {
		DONE, WAITING_SERVER, WAITING_LOGIN, WAITING_MSG, ERROR
	};

	private State state = State.WAITING_SERVER;
	private PrivateMessage value;
	private String nameServer;
	private Login login;
	private String nameServerDst;
	private Login loginDst;
	private String text;
	private final LoginReader loginReader = new LoginReader();
	private final StringReader stringReader = new StringReader();

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		if (state == State.WAITING_SERVER) {
			var processServer = stringReader.process(buffer);
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
				state = State.WAITING_SERVER;
				break;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
		}
		if (state == State.WAITING_SERVER) {
			var processServerDst = stringReader.process(buffer);
			switch (processServerDst) {
			case DONE:
				nameServerDst = stringReader.get();
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
			var processLoginDst = loginReader.process(buffer);
			switch (processLoginDst) {
			case DONE:
				loginDst = loginReader.get();
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
		value = new PrivateMessage(login, text, nameServer, nameServerDst, loginDst);
		return ProcessStatus.DONE;
	}

	@Override
	public PrivateMessage get() {
		if (state != State.DONE) {
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

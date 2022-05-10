package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.entity.Login;

import java.nio.ByteBuffer;

public class LoginReader implements Reader<Login> {
    private StringReader stringReader = new StringReader();
    private Login login;
    private ProcessStatusLogin state = ProcessStatusLogin.WAIT_LOGIN;

    private enum ProcessStatusLogin {
        DONE, WAIT_LOGIN, ERROR
    }

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        var loginProcess = stringReader.process(bb);
        switch (loginProcess){
            case DONE:
                login = new Login(stringReader.get());
                stringReader.reset();
                state = ProcessStatusLogin.DONE;
                break;
            case REFILL:
            	state = ProcessStatusLogin.WAIT_LOGIN;
                return ProcessStatus.REFILL;
            case ERROR:
                state = ProcessStatusLogin.ERROR;
                return ProcessStatus.ERROR;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public Login get() {
        if(state != ProcessStatusLogin.DONE){
            throw new IllegalStateException();
        }
        
        return login;
    }

    @Override
    public void reset() {
        state = ProcessStatusLogin.WAIT_LOGIN;
        login = null;
    }
}

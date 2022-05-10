package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.entity.ConnectedServer;
import fr.upem.net.tcp.nonblocking.entity.Login;

import java.nio.ByteBuffer;

public class ConnectedServerReader implements Reader<ConnectedServer> {
    private StringReader stringReader = new StringReader();
    private ConnectedServer connectedServer;
    private ProcessStatusServer state = ProcessStatusServer.WAIT_SERVER_NAME;

    private enum ProcessStatusServer {
        DONE, WAIT_SERVER_NAME, ERROR
    }

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        var connectedProcess = stringReader.process(bb);
        switch (connectedProcess){
            case DONE:
                connectedServer = new ConnectedServer(stringReader.get());
                stringReader.reset();
                state = ProcessStatusServer.DONE;
                return ProcessStatus.DONE;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                state = ProcessStatusServer.ERROR;
                return ProcessStatus.ERROR;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public ConnectedServer get() {
        if(state != ProcessStatusServer.DONE){
            throw new IllegalStateException();
        }
        return connectedServer;
    }

    @Override
    public void reset() {
        state = ProcessStatusServer.WAIT_SERVER_NAME;
    }
}

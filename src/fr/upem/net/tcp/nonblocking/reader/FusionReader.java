package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.entity.Fusion;
import fr.upem.net.tcp.nonblocking.entity.Login;

import java.nio.ByteBuffer;

public class FusionReader  implements Reader<Fusion> {
    private StringReader stringReader = new StringReader();
    private String nameServer;
    private ProcessStatusFusion state = ProcessStatusFusion.WAIT_SERVER;

    private enum ProcessStatusFusion {
        DONE, WAIT_SERVER, ERROR
    }

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        var nameServerProcess = stringReader.process(bb);
        switch (nameServerProcess){
            case DONE:
                nameServer = stringReader.get();
                stringReader.reset();
                state = ProcessStatusFusion.DONE;
                break;
            case REFILL:
                state = ProcessStatusFusion.WAIT_SERVER;
                return ProcessStatus.REFILL;
            case ERROR:
                state = ProcessStatusFusion.ERROR;
                return ProcessStatus.ERROR;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public Fusion get() {
        if(state != ProcessStatusFusion.DONE){
            throw new IllegalStateException();
        }
        return null;
       //return new Fusion(nameServer, null, );
    }

    @Override
    public void reset() {
        state = ProcessStatusFusion.WAIT_SERVER;
    }
}

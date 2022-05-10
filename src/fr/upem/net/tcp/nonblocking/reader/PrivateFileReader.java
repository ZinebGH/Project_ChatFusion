package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.entity.Login;
import fr.upem.net.tcp.nonblocking.entity.PrivateFile;

import java.nio.ByteBuffer;

public class PrivateFileReader implements Reader<PrivateFile> {
    private enum State {
        DONE, WAITING_SERVER, WAITING_LOGIN, WAITING_FILE, ERROR, WAITING_NUMBER_BLOCKS, WAITING_SIZE_BLOCK, WAITING_OCTETS
    };
    private State state = State.WAITING_SERVER;
    private PrivateFile value;
    private Login loginSrc;
    private Login loginDst;
    private String nameServerSrc;
    private String nameServerDst;
    private int nbBlocks;
    private int sizeBlock;
    private String filename;
    private byte[] block;
    private final LoginReader loginReader = new LoginReader();
    private final StringReader stringReader = new StringReader();
    private final IntReader intReader = new IntReader();
    private final OctetsReader octetsReader = new OctetsReader();


    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_SERVER) {
            var processServer = stringReader.process(buffer);
            switch (processServer) {
                case DONE:
                    nameServerSrc = stringReader.get();
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
                    loginSrc = loginReader.get();
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
            var processServer = stringReader.process(buffer);
            switch (processServer) {
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
            var processLogin = loginReader.process(buffer);
            switch (processLogin) {
                case DONE:
                    loginDst = loginReader.get();
                    loginReader.reset();
                    state = State.WAITING_FILE;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if (state == State.WAITING_FILE) {
            var processMsg = stringReader.process(buffer);
            switch (processMsg) {
                case DONE:
                    filename = stringReader.get();
                    stringReader.reset();
                    state = State.WAITING_NUMBER_BLOCKS;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if (state == State.WAITING_NUMBER_BLOCKS) {
            var processNumBlocks = intReader.process(buffer);
            switch (processNumBlocks) {
                case DONE:
                	nbBlocks = intReader.get();
                    intReader.reset();
                    state = State.WAITING_OCTETS;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if (state == State.WAITING_OCTETS) {
            var processOctets = octetsReader.process(buffer);
            switch (processOctets) {
                case DONE:
                	sizeBlock = octetsReader.getSize();
                    block = octetsReader.get();
                    octetsReader.reset();
                    state = State.DONE;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        value = new PrivateFile(loginSrc, loginDst, filename, nameServerSrc, nameServerDst, sizeBlock, nbBlocks, block);
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateFile get() {
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
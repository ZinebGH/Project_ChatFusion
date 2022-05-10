package fr.upem.net.tcp.nonblocking.reader;


import java.nio.ByteBuffer;

public interface Reader<Entity> {

    enum ProcessStatus { DONE, REFILL, ERROR }

    ProcessStatus process(ByteBuffer bb);

    Entity get();

    void reset();

}

package fr.upem.net.tcp.nonblocking.visitor;

import fr.upem.net.tcp.nonblocking.entity.*;

import java.nio.ByteBuffer;

public interface Visitor {
    void visit(PrivateFile privateFile);
    void visit(PrivateMessage privateMessage);
    void visit(Message message);
    void visit(Login login);
    void visit(ConnectedServer connectedServer);
    void visit(Fusion fusion);
}
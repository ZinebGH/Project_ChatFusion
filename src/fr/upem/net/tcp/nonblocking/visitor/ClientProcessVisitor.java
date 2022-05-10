package fr.upem.net.tcp.nonblocking.visitor;

import fr.upem.net.tcp.nonblocking.entity.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public class ClientProcessVisitor implements Visitor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final ByteBuffer buffer;
    public ClientProcessVisitor(ByteBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer);
    }

    public void visit(PrivateFile privateFile) {
        buffer.clear();
        var encodeServerSrc = UTF8.encode(privateFile.getServerSrc());
        var encodeServerDst = UTF8.encode(privateFile.getServerDst());
        var encodeloginSrc = UTF8.encode(privateFile.getLoginSrc().getValue());
        var encodeloginDst = UTF8.encode(privateFile.getLoginDst().getValue());
        var encodeFile = UTF8.encode(privateFile.getFilename());

        if (buffer.remaining() < encodeServerSrc.remaining() + encodeServerDst.remaining() + encodeloginSrc.remaining() + encodeloginDst.remaining() +
                encodeFile.remaining() + 3 * Integer.BYTES + 1) {
            return;
        }
        buffer.put((byte) 6)
                .putInt(encodeServerSrc.remaining())
                .put(encodeServerSrc)
                .putInt(encodeloginSrc.remaining())
                .put(encodeloginSrc)
                .putInt(encodeServerDst.remaining())
                .put(encodeServerDst)
                .putInt(encodeloginDst.remaining())
                .put(encodeloginDst)
                .putInt(encodeFile.remaining())
                .put(encodeFile)
                .putInt(privateFile.getNbBlocks())
                .putInt(privateFile.getBlockSize())
                .put(privateFile.getBlock());
    }

    public void visit(PrivateMessage privateMessage) {
        var encodelogin = UTF8.encode(privateMessage.getLogin().getValue());
        var encodetext = UTF8.encode(privateMessage.getTexte());
        var encodeServerSrc = UTF8.encode(privateMessage.getNameServerSrc());
        var encodeLoginDst = UTF8.encode(privateMessage.getLoginDst().getValue());
        var encodeServerDst = UTF8.encode(privateMessage.getNameServerDst());

        if (buffer.remaining() < encodelogin.remaining() + encodetext.remaining() + encodeServerSrc.remaining() + encodeLoginDst.remaining() +
                encodeServerDst.remaining()) {
            return;
        }
        buffer.put((byte) 5)
                .putInt(encodeServerSrc.remaining())
                .put(encodeServerSrc)
                .putInt(encodelogin.remaining())
                .put(encodelogin)
                .putInt(encodeServerDst.remaining())
                .put(encodeServerDst).putInt(encodeLoginDst.remaining())
                .put(encodeLoginDst).putInt(encodetext.remaining()).put(encodetext);
    }

    public void visit(Message message) {
        var encodelogin = UTF8.encode(message.getLogin().getValue());
        var encodetext = UTF8.encode(message.getTexte());
        var encodeNameServer  = UTF8.encode(message.getNameServer());

        if (buffer.remaining() < encodelogin.remaining() + encodetext.remaining() + encodeNameServer.remaining()) {
            return;
        }
        buffer.put((byte) 4)
                .putInt(encodeNameServer.remaining())
                .put(encodeNameServer)
                .putInt(encodelogin.remaining())
                .put(encodelogin)
                .putInt(encodetext.remaining())
                .put(encodetext);
    }

    public void visit(Login login) {
        var encodeName = UTF8.encode(login.getValue());
        if (buffer.remaining() < encodeName.remaining()) {
            return;
        }
        buffer.put((byte) 0)
                .putInt(encodeName.remaining())
                .put(encodeName);
    }

    public void visit(ConnectedServer connectedServer) {
    }

    public void visit(Fusion fusion) {

    }


}
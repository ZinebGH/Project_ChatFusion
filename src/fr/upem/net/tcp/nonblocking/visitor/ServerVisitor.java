package fr.upem.net.tcp.nonblocking.visitor;

import fr.upem.net.tcp.nonblocking.entity.*;
import fr.upem.net.tcp.nonblocking.server.ServerChatFusion;

import java.nio.channels.SelectionKey;
import java.util.Objects;

public class ServerVisitor implements Visitor {
    private final ServerChatFusion server;
    private final ServerChatFusion.Context context;

    public ServerVisitor(ServerChatFusion server, ServerChatFusion.Context context) {
        this.server = Objects.requireNonNull(server);
        this.context = Objects.requireNonNull(context);
    }

    public void visit(PrivateFile privateFile) {
        var nameServerDst = server.servers.get(privateFile.getServerDst());
        var logDstContext = nameServerDst.clients.get(privateFile.getLoginDst().getValue());
        if(nameServerDst == null || logDstContext == null) {
            return;
        }
        logDstContext.queueMessage(privateFile, nameServerDst);
    }

    public void visit(PrivateMessage privateMessage) {
        var serverDst = server.getServers().get(privateMessage.getNameServerDst());
        var logDstContext = serverDst.getClients().get(privateMessage.getLoginDst().getValue());
        if(serverDst == null || logDstContext == null) {
            return;
        }
        logDstContext.queueMessage(privateMessage, serverDst);
    }

    public void visit(Message message) {
        for (SelectionKey key : server.getSelector().keys()) {
            var context = (ServerChatFusion.Context) key.attachment();
            if (context == null)
                continue;
            context.queueMessage(message, server);
        }
    }

    public void visit(Login login) {
        context.queueMessage(login, server);
    }

    public void visit(ConnectedServer connectedServer) {
        context.queueMessage(connectedServer, server);
    }

    public void visit(Fusion fusion) {
        System.out.println("Fusion Server vistor");
    }
}
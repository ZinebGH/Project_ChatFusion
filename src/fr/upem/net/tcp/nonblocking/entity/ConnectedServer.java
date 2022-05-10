package fr.upem.net.tcp.nonblocking.entity;

import fr.upem.net.tcp.nonblocking.server.ServerChatFusion;
import fr.upem.net.tcp.nonblocking.server.ServerChatFusion.Context;
import fr.upem.net.tcp.nonblocking.visitor.Visitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public class ConnectedServer implements Entity {
	private final String serverName;
	private static final Charset UTF8 = Charset.forName("UTF8");

	public ConnectedServer(String serverName) {
		this.serverName = Objects.requireNonNull(serverName);
	}

	@Override
	public String getValue() {
		return serverName;
	}


	@Override
	public String toString() {
		return "Connected to the Server '" + serverName + '\'';
	}

	@Override
	public void accept(Visitor visitor){
		visitor.visit(this);
	}

	@Override
	public void process(Visitor visitor) {
		visitor.visit(this);
	}
}

package fr.upem.net.tcp.nonblocking.visitor;

import fr.upem.net.tcp.nonblocking.entity.*;
import fr.upem.net.tcp.nonblocking.server.ServerChatFusion;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerProcessVisitor implements Visitor {
	private final ServerChatFusion server;
	private final ServerChatFusion.Context context;
	private ByteBuffer bufferOut;
	private static final Charset UTF8 = Charset.forName("UTF8");

	
	public ServerProcessVisitor(ServerChatFusion server, ServerChatFusion.Context context, ByteBuffer bufferOut) {

		this.server = Objects.requireNonNull(server);
		this.context = Objects.requireNonNull(context);
		this.bufferOut = Objects.requireNonNull(bufferOut);
		
	}

	public void visit(PrivateFile privateFile) {
		if (!bufferOut.hasRemaining()) {
			return;
		}
		bufferOut.clear();
		var servSrc = UTF8.encode(privateFile.getServerSrc());
		var servDst = UTF8.encode(privateFile.getServerDst());
		var logSrc = UTF8.encode(privateFile.getLoginSrc().getValue());
		var logDst = UTF8.encode(privateFile.getLoginDst().getValue());
		var file = UTF8.encode(privateFile.getFilename());

		if (bufferOut.remaining() < logSrc.remaining() + logDst.remaining() + file.remaining() + 3 * Integer.BYTES
				+ 1) {
			return;
		}
		bufferOut.put((byte) 6);
		bufferOut.putInt(servSrc.remaining());
		bufferOut.put(servSrc);
		bufferOut.putInt(logSrc.remaining());
		bufferOut.put(logSrc);
		bufferOut.putInt(servDst.remaining());
		bufferOut.put(servDst);
		bufferOut.putInt(logDst.remaining());
		bufferOut.put(logDst);
		bufferOut.putInt(file.remaining());
		bufferOut.put(file);
		bufferOut.putInt(privateFile.getNbBlocks());
		bufferOut.putInt(privateFile.getBlockSize());
		bufferOut.put(privateFile.getBlock());
	}

	public void visit(PrivateMessage privateMessage) {
		if (!bufferOut.hasRemaining()) {
			return;
		}
		var log = UTF8.encode(privateMessage.getLogin().getValue());
		var text = UTF8.encode(privateMessage.getTexte());
		var serverSrc = UTF8.encode(privateMessage.getNameServerSrc());
		var logDst = UTF8.encode(privateMessage.getLoginDst().getValue());
		var serverDst = UTF8.encode(privateMessage.getNameServerDst());

		if (serverSrc.remaining() > 100 || log.remaining() > 30 || serverDst.remaining() > 100
				|| logDst.remaining() > 30 || text.remaining() > 1024) {
			System.out.println("Problem size");
			return;
		}
		bufferOut.put((byte) 5);
		bufferOut.putInt(serverSrc.remaining());
		bufferOut.put(serverSrc);
		bufferOut.putInt(log.remaining());
		bufferOut.put(log);
		bufferOut.putInt(serverDst.remaining());
		bufferOut.put(serverDst);
		bufferOut.putInt(logDst.remaining());
		bufferOut.put(logDst);
		bufferOut.putInt(text.remaining());
		bufferOut.put(text);
	}

	public void visit(Message message) {
		if (!bufferOut.hasRemaining()) {
			return;
		}
		var log = UTF8.encode(message.getLogin().getValue());
		var text = UTF8.encode(message.getTexte());
		var nameServ = UTF8.encode(message.getNameServer());
		bufferOut.put((byte) 4);
		bufferOut.putInt(nameServ.remaining());
		bufferOut.put(nameServ);
		bufferOut.putInt(log.remaining());
		bufferOut.put(log);
		bufferOut.putInt(text.remaining());
		bufferOut.put(text);
	}

	public void visit(Login login) {
		if (!bufferOut.hasRemaining()) {
			return;
		}
		if (server.newClient(login, context)) {
			var nameServer = UTF8.encode(server.get());
			bufferOut.put((byte) 2);
			bufferOut.putInt(nameServer.remaining());
			bufferOut.put(nameServer);
		} else {
			bufferOut.put((byte) 3);
		}
	}

	public void visit(ConnectedServer connectedServer) {
		return;
	}

	public void visit(Fusion fusion) {
		if (!bufferOut.hasRemaining()) {
			return;
		}
		var encodeMegaServerName = UTF8.encode(server.get());
		var encodeAddress = UTF8.encode(fusion.getHostName());
		var encodeLstMembers = new ArrayList<ByteBuffer>();
		var encodeLstMembersSize = 0;

		for (var member : fusion.getMembers()) {
			var memberEncode = UTF8.encode(member);
			encodeLstMembersSize += memberEncode.remaining();
			encodeLstMembers.add(memberEncode);
		}
		if (bufferOut.remaining() > encodeMegaServerName.remaining() + encodeAddress.remaining()
				+ (encodeLstMembersSize + 3) * Integer.BYTES + 1) {
			System.out.println("Problem size");
			return;
		}
		bufferOut.put((byte) 8);
		bufferOut.putInt(encodeMegaServerName.remaining());
		bufferOut.put(encodeMegaServerName);
		bufferOut.putInt(encodeAddress.remaining());
		bufferOut.put(encodeAddress);
		bufferOut.putInt(fusion.getNbMembers());
		for (var i = 0; i < encodeLstMembers.size(); i++) {
			bufferOut.putInt(encodeLstMembers.get(i).remaining());
			bufferOut.put(encodeLstMembers.get(i));
		}

	}
}

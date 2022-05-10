package fr.upem.net.tcp.nonblocking.visitor;

import fr.upem.net.tcp.nonblocking.client.ClientChatFusion;
import fr.upem.net.tcp.nonblocking.entity.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ClientVisitor implements Visitor {
	private final ClientChatFusion client;
	private final int cpt;

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public ClientVisitor(ClientChatFusion client, int cpt) {
		this.client = client;
		this.cpt = cpt;
	}

	public void visit(PrivateFile privateFile) {
		try {
			FileWriter fw;
			if (cpt == 1) {
				System.out.println("Private File in : " + privateFile.getServerDst());
			}
			fw = new FileWriter("../"+privateFile.getFilename(), true);
			var b = ByteBuffer.allocateDirect(privateFile.getBlock().length);
			for (var i = 0; i < privateFile.getBlock().length; i++) {
				b.put(privateFile.getBlock()[i]);
			}
			b.flip();
			fw.write(UTF8.decode(b).toString());
			fw.close();
			if (cpt == privateFile.getNbBlocks()) {
				System.out.println(privateFile);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void visit(PrivateMessage privateMessage) {
		System.out.println(privateMessage);
	}

	public void visit(Message message) {
		System.out.println(message);
	}

	public void visit(Login login) {
		System.out.println(login);
	}

	public void visit(ConnectedServer connectedServer) {
		client.connectClient();
		System.out.println(connectedServer);
	}

	public void visit(Fusion fusion) {
		System.out.println("Fusion Client Visitor");
	}

}

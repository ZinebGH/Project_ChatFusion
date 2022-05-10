package fr.upem.net.tcp.nonblocking.server;

import fr.upem.net.tcp.nonblocking.entity.Entity;
import fr.upem.net.tcp.nonblocking.entity.Fusion;
import fr.upem.net.tcp.nonblocking.entity.Login;
import fr.upem.net.tcp.nonblocking.reader.LoginReader;
import fr.upem.net.tcp.nonblocking.reader.MessageReader;
import fr.upem.net.tcp.nonblocking.reader.PrivateFileReader;
import fr.upem.net.tcp.nonblocking.reader.PrivateMessageReader;
import fr.upem.net.tcp.nonblocking.reader.Reader;
import fr.upem.net.tcp.nonblocking.visitor.ServerProcessVisitor;
import fr.upem.net.tcp.nonblocking.visitor.ServerVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ServerChatFusion {
	static public class Context {
		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<Entity> queue = new LinkedList<>();
		final private ServerChatFusion server;
		private Object lock = new Object();
		

		private boolean closed = false;
		private Reader<?> reader = new LoginReader();

		private Context(ServerChatFusion server, SelectionKey key) {
			this.key = Objects.requireNonNull(key);
			this.sc = (SocketChannel) key.channel();
			this.server = Objects.requireNonNull(server);
		}

		/**
		 * Process the content of bufferIn
		 *
		 * The convention is that bufferIn is in write-mode before the call to process
		 * and after the call
		 *
		 */
		private void processIn() {
			bufferIn.flip();
			defineReader(bufferIn.get());
			bufferIn.compact();
			Reader.ProcessStatus status = reader.process(bufferIn);
			switch (status) {
			case DONE:
				var entity = (Entity) reader.get();
				acceptEntity(entity, server, this);
				//entity.broadcast(this, server);
				reader.reset();
				return;
			case REFILL:
				return;
			case ERROR:
				silentlyClose();
				return;
			}

		}

		/**
		 * Executes the code linked to the given entity.
		 *
		 * @param entity The entity to which execute the code.
		 */
		protected void acceptEntity(Entity entity, ServerChatFusion server, Context ctx) {
			var entityVisitor = new ServerVisitor(server, ctx);
			entity.accept(entityVisitor);
		}

		private void defineReader(byte opCode) {

			switch (opCode) {
			case 0:
				System.out.println("Anonymous Connection");
				reader = new LoginReader();
				break;
			case 1:
				System.out.println("PWD Connection");
				break;
			case 4:
				System.out.println("Public Message");
				reader = new MessageReader();
				break;

			case 5:
				System.out.println("Private Message");
				reader = new PrivateMessageReader();
				break;

			case 6:
				System.out.println("Private File");
				reader = new PrivateFileReader();
				break;
			default:
				System.out.println("DEFAULT SERVE");
				break;
			}
		}

		/**
		 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
		 *
		 * @param entity
		 */
		public void queueMessage(Entity entity, ServerChatFusion server) {
			synchronized (lock) {
				queue.add(entity);
				processOut();
				updateInterestOps();
			}
		}

		/**
		 * Try to fill bufferOut from the message queue
		 */
		private void processOut() {
			synchronized (lock) {
				while (bufferOut.remaining() >= Integer.BYTES && !queue.isEmpty()) {
					var entity = queue.peek();
					processEntity(entity, bufferOut);
					queue.remove();
				}
			}
		}

		/**
		 * Executes the code linked to the given entity.
		 *
		 * @param entity The entity to which execute the code.
		 */
		protected void processEntity(Entity entity, ByteBuffer bufferOut) {
			var entityVisitor = new ServerProcessVisitor(server ,this, bufferOut);
			entity.process(entityVisitor);
		}


		/**
		 * Update the interestOps of the key looking only at values of the boolean
		 * closed and of both ByteBuffers.
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * updateInterestOps and after the call. Also it is assumed that process has
		 * been be called just before updateInterestOps.
		 */
		private void updateInterestOps() {
			int intOps = 0;
			if (bufferIn.hasRemaining() && !closed) {
				intOps |= SelectionKey.OP_READ;
			}
			if (bufferOut.position() != 0) {
				intOps |= SelectionKey.OP_WRITE;
			}
			if (intOps == 0) {
				silentlyClose();
				return;
			}
			key.interestOps(intOps);
		}

		private void silentlyClose() {
			try {
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Performs the read action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doRead and after the call
		 *
		 * @throws IOException e
		 */
		private void doRead() throws IOException {
			if (sc.read(bufferIn) == -1) {
				closed = true;
			}
			processIn();
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doWrite and after the call
		 *
		 * @throws IOException e
		 */
		private void doWrite() throws IOException {
			bufferOut.flip();
			sc.write(bufferOut);
			bufferOut.compact();
			processOut();
			updateInterestOps();
		}
	}

	public final HashMap<String, ServerChatFusion> servers = new HashMap<>();
	private String nameServer;
	private static final Charset UTF8 = Charset.forName("UTF8");
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	static private int BUFFER_SIZE = 5_040;
	static private Logger logger = Logger.getLogger(ServerChatFusion.class.getName());
	public final HashMap<String, Context> clients = new HashMap<>(); // Connexion public des clients
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private static final Pattern IP_PATTERN = Pattern
			.compile("^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])(.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])){3}");
	private static final Pattern PORT_PATTERN = Pattern.compile("^([0-9]){1,4}");
	private final Thread console;

	private Context uniqueContext;

	public ServerChatFusion(String nameServer, int port) throws IOException {
		this.nameServer = Objects.requireNonNull(newServer(nameServer));
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		this.console = new Thread(this::consoleRun);
	}


	private void consoleRun() {
		try {
			try (var scanner = new Scanner(System.in)) {
				while (scanner.hasNextLine()) {
					var msg = scanner.nextLine();
					fusionCommand(msg);
				}
			}
			logger.info("Console thread stopping");
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		}
	}

	private void fusionCommand(String msg) throws InterruptedException {
		synchronized (msg) {
			commandQueue.add(msg);
			selector.wakeup();
		}
	}

	private void processCommands() throws IOException {
		while (!commandQueue.isEmpty()) {
			String command;
			synchronized (commandQueue) {
				command = commandQueue.remove();
			}
			var txt = command.split(" ");
			if (txt.length != 3 || !txt[0].matches("FUSION") || !txt[1].matches(IP_PATTERN.pattern()) || !txt[2].matches(PORT_PATTERN.pattern())) {
				return;
			}
			System.out.println("FUSIOOOON " + txt[0] + txt[1] + txt[2]);
			if(0 > Integer.parseInt(txt[2]) ||  Integer.parseInt(txt[2])  > 65535){
	            throw new IllegalArgumentException("Port is incorrect");
	        }
			InetSocketAddress addr = new InetSocketAddress(txt[1], Integer.parseInt(txt[2]));
			//DOIT RECUPERER LE NOM DE L'AUTRE SERVER ???
			var entity = new Fusion(addr, nameServer, clients.keySet());
			uniqueContext.queueMessage(entity, this);
		}
	}

	
	public String newServer(String nameServer) {
		var server = UTF8.encode(nameServer);
		if (server.remaining() <= 96) {
			 servers.putIfAbsent(nameServer, this);
			 
			 return nameServer;
		}
		return null;
	}

	
	public String get() {
		return nameServer;
	}

	public boolean newClient(Login login, Context context) {
		return clients.putIfAbsent(login.getValue(), context) == null;

	}
	public HashMap<String, ServerChatFusion> getServers() {
		return servers;
	}

	public HashMap<String, Context> getClients() {
		return clients;
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(getSelector(), SelectionKey.OP_ACCEPT);

		console.start();

		while (!Thread.interrupted()) {
			printKeys();
			System.out.println("Starting select");
			try {
				getSelector().select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
			System.out.println("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
		printSelectedKey(key);
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection closed with client due to IOException", e);
			silentlyClose(key);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		var sc = serverSocketChannel.accept();
		if (sc == null) {
			logger.warning("The selector was wrong");
			return; // the selector gave a bad hint
		}
		sc.configureBlocking(false);
		var ckey = sc.register(getSelector(), SelectionKey.OP_READ);
		ckey.attach(new Context(this, ckey));
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	/***
	 * Theses methods are here to help understanding the behavior of the selector
	 ***/
	private String interestOpsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
			list.add("OP_ACCEPT");
		if ((interestOps & SelectionKey.OP_READ) != 0)
			list.add("OP_READ");
		if ((interestOps & SelectionKey.OP_WRITE) != 0)
			list.add("OP_WRITE");
		return String.join("|", list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = getSelector().keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e) {
			return "???";
		}
	}

	public void printSelectedKey(SelectionKey key) {
		SelectableChannel channel = key.channel();
		if (channel instanceof ServerSocketChannel) {
			System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
		} else {
			SocketChannel sc = (SocketChannel) channel;
			System.out.println(
					"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable())
			list.add("ACCEPT");
		if (key.isReadable())
			list.add("READ");
		if (key.isWritable())
			list.add("WRITE");
		return String.join(" and ", list);
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 2) {
			usage();
			return;
		}
		if (0 > Integer.parseInt(args[1]) || Integer.parseInt(args[1]) > 65535) {
			System.out.println("Numéro de port doit être compris entre 0 et 65535");
			return;
		}
		new ServerChatFusion(args[0], Integer.parseInt(args[1])).launch();
	}

	private static void usage() {
		System.out.println("Usage : ServerChatFusion hostname port");
	}

	public Selector getSelector() {
		return selector;
	}

}

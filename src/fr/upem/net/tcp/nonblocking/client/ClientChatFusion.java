package fr.upem.net.tcp.nonblocking.client;

import fr.upem.net.tcp.nonblocking.entity.Entity;
import fr.upem.net.tcp.nonblocking.entity.Login;
import fr.upem.net.tcp.nonblocking.entity.Message;
import fr.upem.net.tcp.nonblocking.entity.PrivateFile;
import fr.upem.net.tcp.nonblocking.entity.PrivateMessage;
import fr.upem.net.tcp.nonblocking.reader.*;

import fr.upem.net.tcp.nonblocking.server.ServerChatFusion;
import fr.upem.net.tcp.nonblocking.visitor.ClientProcessVisitor;
import fr.upem.net.tcp.nonblocking.visitor.ClientVisitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class ClientChatFusion {

	static private class Context {
		private final SelectionKey key;
		private final SocketChannel sc;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final ArrayDeque<Entity> queue = new ArrayDeque<>();
		private boolean closed = false;
		private int cpt = 0;
		private Reader<? extends Entity> reader;
		private Object lock = new Object();
		private String nameServer;

		private Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
		}

		/**
		 * Process the content of bufferIn
		 *
		 * The convention is that bufferIn is in write-mode before the call to process
		 * and after the call
		 *
		 */
		private void processIn(ClientChatFusion client) {
			bufferIn.flip();
			defineReader(bufferIn.get());
			bufferIn.compact();
			if (reader == null) {return;}
			Reader.ProcessStatus status = reader.process(bufferIn);
			switch (status) {
			case DONE:
				var entity = reader.get();
				if (!client.isConnected && nameServer == null) {
					nameServer = entity.getValue();
				}
				//entity.execute(client);
				
				acceptEntity(entity, client, cpt);
				reader.reset();
				break;
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
		protected void acceptEntity(Entity entity, ClientChatFusion client, int cpt) {
			
			var entityVisitor = new ClientVisitor(client, cpt);
			entity.accept(entityVisitor);
		}

		private void defineReader(byte opCode) {
			switch (opCode) {
			case 2:
				reader = new ConnectedServerReader();
				System.out.println("Authentification accepted");
				break;
			case 3:
				System.out.println("Authentification refused");
				break;
			case 4:
				System.out.println("Public Message in : " + nameServer);
				reader = new MessageReader();
				break;
			case 5:
				System.out.println("Private Message in : " + nameServer);
				reader = new PrivateMessageReader();
				break;
			case 6:
				reader = new PrivateFileReader();
				cpt++;
				
				break;
			case 8:
				System.out.println("Fusion Server");
				reader = new FusionReader();
				break;
			default:
				break;
			}
		}

		/**
		 * Add a message to the message queue, tries to fill bufferOut and
		 * updateInterestOps
		 *
		 * @param entity
		 */
		public void queueMessage(Entity entity) {
			synchronized (lock) {
				queue.add(entity);
				processOut();
				updateInterestOps();
			}
		}

		/**
		 * Try to fill bufferOut from the message queue
		 *
		 */
		private void processOut() {
			synchronized (lock) {
				while (!queue.isEmpty()) {
					var entity = queue.remove();
					processEntity(entity, bufferOut);
				}
			}
		}


		/**
		 * Executes the code linked to the given entity.
		 *
		 * @param entity The entity to which execute the code.
		 */
		protected void processEntity(Entity entity, ByteBuffer buffer) {
			var entityVisitor = new ClientProcessVisitor(buffer);
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
			var interesOps = 0;
			if (!closed && bufferIn.hasRemaining()) {
				interesOps = interesOps | SelectionKey.OP_READ;
			}
			if ((bufferOut.position() > 0 || !queue.isEmpty()) && !closed) {
				interesOps = interesOps | SelectionKey.OP_WRITE;
			}
			if (interesOps == 0) {
				silentlyClose();
				return;
			}
			key.interestOps(interesOps);
		}

		private Object getLock() {
			return lock;
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
		 * @throws IOException
		 */
		private void doRead(ClientChatFusion client) throws IOException {
			if (sc.read(bufferIn) == -1) {
				closed = true;
			}
			processIn(client);
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doWrite and after the call
		 *
		 * @throws IOException
		 */
		private void doWrite() throws IOException {
			bufferOut.flip();
			sc.write(bufferOut);
			bufferOut.compact();
			processOut();
			updateInterestOps();
		}

		public void doConnect() throws IOException {
			if (!sc.finishConnect())
				return; // the selector gave a bad hint
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	static private int BUFFER_SIZE = 5_040;
	static private Logger logger = Logger.getLogger(ClientChatFusion.class.getName());
	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAddress;
	private boolean isConnected;
	private final Thread console;
	private Context uniqueContext;
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private Login login;
	private static final Charset UTF8 = Charset.forName("UTF8");

	public final HashMap<String, ServerChatFusion> servers = new HashMap<>();


	public ClientChatFusion(InetSocketAddress serverAddress) throws IOException {
		this.serverAddress = serverAddress;
		this.isConnected = false;
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
	}

	public void connectClient() {
		isConnected = true;
	}

	private void consoleRun() {
		try {
			try (var scanner = new Scanner(System.in)) {
				while (scanner.hasNextLine()) {
					var msg = scanner.nextLine();
					sendCommand(msg);
				}
			}
			logger.info("Console thread stopping");
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		}
	}

	/**
	 * Send instructions to the selector via a BlockingQueue and wake it up
	 *
	 * @param msg
	 * @throws InterruptedException
	 */
	private void sendCommand(String msg) throws InterruptedException {
		synchronized (msg) {
			commandQueue.add(msg);
			selector.wakeup();
		}
	}

	/**
	 * Processes the command from the BlockingQueue
	 * 
	 * @throws IOException
	 */
	private void processCommands() throws IOException {
		while (!commandQueue.isEmpty()) {
			String command;
			synchronized (commandQueue) {
				command = commandQueue.remove();
			}
			var entity = parseText(command);
			if (entity.isEmpty()) {
				return;
			}
			uniqueContext.queueMessage(entity.get());
		}
	}

	private Optional<Entity> parseText(String command) throws IOException {
		Optional<Entity> entity = Optional.empty();
		if (command.isEmpty() || command.startsWith(" ")) {
			System.out.println("Usage : no empty messages");
			return Optional.empty();
		}

		if (!isConnected) {
			login = new Login(command);
			entity = Optional.of(login);

		} else {
			var consigne = command.charAt(0);
			switch (consigne) {
			case '@': // message privé //
				var elements = command.substring(1).split(" ", 2);
				var texte = elements.length == 1 ? "" : elements[1];
				var infoLogServer = elements[0].split(":");
				var logDst = infoLogServer[0];
				var serverDst = infoLogServer[1];
				entity = Optional
						.of(new PrivateMessage(login, texte, uniqueContext.nameServer, serverDst, new Login(logDst)));
				break;
			case '/': // message fichier
				var elmt = command.substring(1).split(" ", 2);
				var file = elmt.length == 1 ? "" : elmt[1];
				var info = elmt[0].split(":");
				var loginDst = info[0];
				var servDst = info[1];

				BufferedReader br = Files.newBufferedReader(Paths.get(file));
				String line;

				var encodeFile = ByteBuffer.allocateDirect(32000);
				while ((line = br.readLine()) != null) {
					encodeFile.put(UTF8.encode(line));
				}
				encodeFile.flip();
				br.close();

				var nbBlocks = (encodeFile.remaining() / 5000) + 1;
				byte[] block;
				for (var i = 0; i < nbBlocks; i++) {
					block = new byte[5000 < encodeFile.remaining() ? 5000 : encodeFile.remaining()];
					encodeFile.get(block, 0, 5000 < encodeFile.remaining() ? 5000 : encodeFile.remaining());
					var blockSize = block.length;
					uniqueContext.queueMessage(new PrivateFile(login, new Login(loginDst), file, uniqueContext.nameServer,
							servDst, blockSize, nbBlocks, block));
					selector.select(this::treatKey);
				}
				break;
			default: // message global
				entity = Optional.of(new Message(login, command, uniqueContext.nameServer));
				break;
			}
		}
		return entity;
	}

	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		uniqueContext = new Context(key);
		key.attach(uniqueContext);
		sc.connect(serverAddress);

		console.start();

		while (!Thread.interrupted()) {
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException ioe) {
				throw ioe.getCause();
			}
		}
	}

	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				uniqueContext.doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				uniqueContext.doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				uniqueContext.doRead(this);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
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
		new ClientChatFusion(new InetSocketAddress(args[0], Integer.parseInt(args[1]))).launch();
	}

	private static void usage() {
		System.out.println("Usage : ClientChatFusion hostname port");
	}
}

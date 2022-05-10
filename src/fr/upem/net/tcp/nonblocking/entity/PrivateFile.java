package fr.upem.net.tcp.nonblocking.entity;

import fr.upem.net.tcp.nonblocking.visitor.Visitor;

import java.nio.charset.Charset;
import java.util.Objects;

public class PrivateFile implements Entity {
	private final Login loginSrc;
	private final Login loginDst;
	private final String filename;
	private final String serverSrc;
	private final String serverDst;
	private final int nbBlocks;
	private final int blockSize;
	private byte block[];


	public PrivateFile(Login loginSrc, Login loginDst, String filename, String serverSrc, String serverDst,
			int blockSize, int nbBlocks, byte[] block) {
		this.loginSrc = Objects.requireNonNull(loginSrc);
		this.loginDst = Objects.requireNonNull(loginDst);
		this.filename = Objects.requireNonNull(filename);
		this.serverSrc = Objects.requireNonNull(serverSrc);
		this.serverDst = Objects.requireNonNull(serverDst);
		this.nbBlocks = Objects.requireNonNull(nbBlocks);
		this.blockSize = Objects.requireNonNull(blockSize);
		this.block = block;
	}

	@Override
	public String toString() {
		return "\n\t\t\t FILE " + getFilename() + " RECEIVED FROM : " + getLoginSrc().getValue() + " SERVER : " + getServerSrc();
	}
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void process(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String getValue() {
		return filename;
	}

	public Login getLoginSrc() {
		return loginSrc;
	}

	public Login getLoginDst() {
		return loginDst;
	}

	public String getFilename() {
		return filename;
	}

	public String getServerSrc() {
		return serverSrc;
	}

	public String getServerDst() {
		return serverDst;
	}

	public int getNbBlocks() {
		return nbBlocks;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public byte[] getBlock() {
		return block;
	}
}
package fr.upem.net.tcp.nonblocking.entity;

import fr.upem.net.tcp.nonblocking.visitor.Visitor;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;

public class Fusion implements Entity {

	
	private final InetSocketAddress addr;
	private final String megaServerName;
	private final int nbMembers;
	private final Set<String> members;
	private static final Charset UTF8 = Charset.forName("UTF8");

	public Fusion(InetSocketAddress addr, String megaServerName, Set<String> members) {
		this.addr = Objects.requireNonNull(addr);
		this.megaServerName = Objects.requireNonNull(megaServerName);
		this.members = Objects.requireNonNull(members);
		this.nbMembers = Objects.requireNonNull(members.size());
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);

	}

	@Override
	public void process(Visitor visitor) {
		visitor.visit(this);

	}

	public String getHostName() {
		return addr.getHostName();
	}

	public Integer getPort() {
		return addr.getPort();
	}

	public String getMegaServerName() {
		return megaServerName;
	}

	

	public int getNbMembers() {
		return nbMembers;
	}

	public Set<String> getMembers() {
		return members;
	}
}
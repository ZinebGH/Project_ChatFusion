package fr.upem.net.tcp.nonblocking.entity;

import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.visitor.Visitor;

public class PrivateMessage implements Entity {
	private final Login login;
	private final String texte;
	private final String nameServerSrc;
	private final Login loginDst;
	private final String nameServerDst;

	public PrivateMessage(Login login, String text, String nameServerSrc, String nameServerDst, Login loginDst) {
		this.login = Objects.requireNonNull(login);
		this.texte = Objects.requireNonNull(text);
		this.nameServerSrc = Objects.requireNonNull(nameServerSrc);
		this.loginDst = Objects.requireNonNull(loginDst);
		this.nameServerDst = Objects.requireNonNull(nameServerDst);
	}

	@Override
	public String toString() {
		return "In server : " + nameServerDst + "\n        " + login.getValue() + " : \"" + texte + "\"";
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

	public Login getLogin() {
		return login;
	}

	public String getTexte() {
		return texte;
	}

	public String getNameServerSrc() {
		return nameServerSrc;
	}

	public Login getLoginDst() {
		return loginDst;
	}

	public String getNameServerDst() {
		return nameServerDst;
	}
}
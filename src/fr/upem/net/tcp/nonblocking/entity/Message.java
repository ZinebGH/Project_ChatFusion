package fr.upem.net.tcp.nonblocking.entity;

import fr.upem.net.tcp.nonblocking.server.ServerChatFusion;
import fr.upem.net.tcp.nonblocking.visitor.Visitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public class Message implements Entity {
    public final Login login;
    public final String texte;
    public final String nameServer;
    private static final Charset UTF8 = Charset.forName("UTF8");

    public Message(Login login, String text, String nameServer) {
        this.login = Objects.requireNonNull(login);
        this.texte = Objects.requireNonNull(text);
        this.nameServer = Objects.requireNonNull(nameServer);
    }

    @Override
    public String toString() {
        return "Public Message \"" + texte + "\" from " + login;
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

    public String getNameServer() {
        return nameServer;
    }

    @Override
    public String getValue() {
        return null;
    }


}
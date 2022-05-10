package fr.upem.net.tcp.nonblocking.entity;

import fr.upem.net.tcp.nonblocking.visitor.Visitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public class Login implements Entity {
    private final String name;
    private static final Charset UTF8 = Charset.forName("UTF8");

    public Login(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
        return "login = '" + name + "\' ";
    }

    @Override
    public String getValue() {
        return name;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void process(Visitor visitor) {
        visitor.visit(this);
    }
}
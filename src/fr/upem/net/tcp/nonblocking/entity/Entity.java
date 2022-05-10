package fr.upem.net.tcp.nonblocking.entity;

import fr.upem.net.tcp.nonblocking.visitor.Visitor;


public interface Entity {
    /**
     * Performs the entity action on the given visitor.
     *
     * @param visitor The visitor to perform the action.
     */
    void accept(Visitor visitor);

    /**
     * Performs the entity action on the given visitor.
     *
     * @param visitor The visitor to perform the action.
     */
    void process(Visitor visitor);

    /**
     * Retrieves the entity content value
     *
     * @return The entity string value
     */
   String getValue();
	
}
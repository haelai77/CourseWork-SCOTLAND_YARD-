package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSmallPlayer {
    protected final int id;
    protected final int location;
    protected final ImmutableList<Integer> tickets;

    AbstractSmallPlayer(int id, int location, ImmutableList<Integer> tickets) {
        this.id = id;
        this.location = location;
        this.tickets = tickets;
    }

    public Integer id() {return this.id;}

    public Integer location() {return this.location;}

    public ImmutableList<Integer> tickets() {return this.tickets;}

    Boolean has(int ticket) {return (this.tickets.get(ticket) > 0);}

    //------------------------------------------------------------------
    //returns a new player that has "travelled" to the next destination and used up the ticket.
//    abstract AbstractSmallPlayer travel (Integer destination, Iterable<Integer> tickets);

}

package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class MrxSmallPlayer extends AbstractSmallPlayer{
//    protected final ImmutableList<Integer> tickets;

    MrxSmallPlayer(int location, ImmutableList<Integer> tickets) {
        super(0, location, tickets);
//        this.tickets = tickets;
        if (tickets.size() != 5 || super.id() != 0) {
            throw new IllegalArgumentException("wrong instantiation of mr x");
        }
    }

    @Override
    public MrxSmallPlayer travel (Integer destination, Iterable<Integer> tickets) { // covariant overriding
        List<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));
        for (Integer ticket : tickets) {
            newTickets.set(ticket, newTickets.get(ticket) - 1);
        }
        return new MrxSmallPlayer(destination, ImmutableList.copyOf(newTickets));
    }

    public MrxSmallPlayer receive (ImmutableList<Integer> tickets) { // only in mrxsmall player so leave it out
        if (tickets.size() != 3) {
            throw new IllegalArgumentException("Mr X receives wrong tickets");
        }
        ArrayList<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));
        for ( int i = 0; i < tickets.size(); i++) {
            newTickets.set(i, newTickets.get(i) + tickets.get(i));
        }
        return new MrxSmallPlayer(this.location, ImmutableList.copyOf(newTickets));
    }
}

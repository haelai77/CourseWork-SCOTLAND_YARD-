package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class MrxSmallPlayer extends AbstractSmallPlayer{
//    protected final ImmutableList<Integer> tickets;

    MrxSmallPlayer(int id, int location, ImmutableList<Integer> tickets) {
        super(id, location, tickets);
//        this.tickets = tickets;
    }

    public MrxSmallPlayer travel (Integer destination, Iterable<Integer> tickets) { //Covariant return type
        List<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));
        for (Integer ticket : tickets) {
            newTickets.set(ticket, newTickets.get(ticket) - 1);
        }

        return new MrxSmallPlayer(this.id, destination, ImmutableList.copyOf(newTickets));
    }

    public MrxSmallPlayer receive (ImmutableList<Integer> tickets) { // only in mrxsmall player so leave it out
        ArrayList<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));
        for ( int i = 0; i < tickets.size(); i++) {
            newTickets.set(i, newTickets.get(i) + tickets.get(i));
        }
        return new MrxSmallPlayer(this.id, this.location, ImmutableList.copyOf(newTickets));
    }
}

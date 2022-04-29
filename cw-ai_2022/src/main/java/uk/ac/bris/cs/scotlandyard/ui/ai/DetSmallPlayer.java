package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class DetSmallPlayer extends AbstractSmallPlayer{
//    protected final int ticket;

    DetSmallPlayer(int id, int location, ImmutableList<Integer> tickets){
        super(id, location, tickets);
        if (id == 0 || tickets.size() != 5 || tickets.get(3) > 0 || tickets.get(4) > 0) {
            throw new IllegalArgumentException("wrong instantiation of detective");
        }
    }

    @Override
    public DetSmallPlayer travel(Integer destination, Iterable<Integer> ticket) { //covariant overriding
        List<Integer> newTicket = new ArrayList<>(List.copyOf(this.tickets));
        int ticketD = ticket.iterator().next();
        newTicket.set(ticketD, newTicket.get(ticketD) - 1);
        return new DetSmallPlayer(this.id, destination, ImmutableList.copyOf(newTicket));
    }
}

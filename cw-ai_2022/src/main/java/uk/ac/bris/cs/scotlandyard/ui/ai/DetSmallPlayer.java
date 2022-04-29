package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class DetSmallPlayer extends AbstractSmallPlayer{
//    protected final int ticket;

    DetSmallPlayer(int id, int location, ImmutableList<Integer> tickets){
        super(id, location, tickets);
    }

    public DetSmallPlayer travel(Integer destination, int ticket) {
        List<Integer> newTicket = new ArrayList<>(List.copyOf(this.tickets));
        newTicket.set(ticket, newTicket.get(ticket) - 1);
        return new DetSmallPlayer(this.id, destination, ImmutableList.copyOf(newTicket));
    }
}

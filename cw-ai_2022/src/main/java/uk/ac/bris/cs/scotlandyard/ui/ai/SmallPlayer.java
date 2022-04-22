package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

//a small version of a player. holds id (can be thought of like a piece), location, and an array of numbers which represent tickets.
//ticket amounts are held as follows: [TAXI, BUS, UNDERGROUND, DOUBLE, SECRET]
public class SmallPlayer {
    int id;
    int location;
    ImmutableList<Integer> tickets;


    SmallPlayer(int id, int location, ImmutableList<Integer> tickets) {
        this.id = id;
        this.location = location;
        this.tickets = tickets;
    }

    public Integer location() {
        return this.location;
    }

    //returns a new player that has "travelled" to the next destination and used up the ticket.
    public SmallPlayer travel (Integer destination, Iterable<Integer> tickets) {
        List<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));
        for (Integer ticket : tickets) {

            newTickets.set(ticket, newTickets.get(ticket) - 1);

        }
        return new SmallPlayer(this.id, destination, ImmutableList.copyOf(newTickets));
    }

    public SmallPlayer receive (ImmutableList<Integer> tickets) {
        ArrayList<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));
        for ( int i = 0; i < tickets.size(); i++) {
            newTickets.set(i, newTickets.get(i) + tickets.get(i));
        }
        return new SmallPlayer(this.id, this.location, ImmutableList.copyOf(newTickets));
    }

}


package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.concurrent.Immutable;
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
    public SmallPlayer travel (Integer destination, Integer ticket) {
        List<Integer> newTickets = new ArrayList<>(List.copyOf(this.tickets));

        newTickets.set(ticket, newTickets.get(ticket) - 1);

        return new SmallPlayer(this.id, destination, ImmutableList.copyOf(newTickets));
    }

}


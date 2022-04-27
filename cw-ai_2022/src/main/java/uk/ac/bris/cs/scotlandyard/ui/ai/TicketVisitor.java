package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.concurrent.Immutable;

public class TicketVisitor implements Move.Visitor<ImmutableList<ScotlandYard.Ticket>>{
    @Override
    public ImmutableList<ScotlandYard.Ticket> visit(Move.SingleMove move) {
        return ImmutableList.of(move.ticket);
    }

    @Override
    public ImmutableList<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
        return ImmutableList.of(move.ticket1, move.ticket2);
    }
}

package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

public class DestinationVisitor implements Move.Visitor<Integer>{
        @Override
        public Integer visit(Move.SingleMove move) {
            return move.destination;
        }

        @Override
        public Integer visit(Move.DoubleMove move) {
            return move.destination2;
        }
    }


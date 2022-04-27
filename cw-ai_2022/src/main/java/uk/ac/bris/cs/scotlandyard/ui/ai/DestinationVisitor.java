package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

public class DestinationVisitor implements Move.Visitor<Integer>{
        @Override
        public Integer visit(Move.SingleMove move) {
            return move.destination; // returns destination of single move
        }

        @Override
        public Integer visit(Move.DoubleMove move) {
            return move.destination2; // returns final destination of double move
        }
    }


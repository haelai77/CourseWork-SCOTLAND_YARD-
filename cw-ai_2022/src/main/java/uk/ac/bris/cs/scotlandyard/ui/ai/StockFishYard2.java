package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class StockFishYard2 implements Ai {

    @Nonnull @Override public String name() { return "StockFishYard2"; }

    //------------------------------------------------------------------------------------------------

    private List<Integer> getDetectiveLocations(Board board) {
        List<Integer> locations = new ArrayList<>();
        for (Piece piece : board.getPlayers()) {
            if (piece.isDetective()) {
                locations.add(board.getDetectiveLocation((Piece.Detective) piece).orElse(0));
            }
        }
        return locations;
    }
//----------------------------------------------------------------------------------------------------
    @Nonnull public Integer biBFS_dist(@Nonnull Board board, Integer node1, Integer node2){
        //--------------------- for node1
        Map<Integer, Integer>  node1Map = new HashMap<Integer, Integer>(); //contains previous node to a node
        List<Integer> queue1 = new ArrayList<Integer>(); // nodes to be searched next
        List<Integer> visited1 = new ArrayList<Integer>(); // nodes that have been searched
        //--------------------- for node2
        Map<Integer, Integer>  node2Map = new HashMap<Integer, Integer>();
        List<Integer> queue2 = new ArrayList<Integer>();
        List<Integer> visited2 = new ArrayList<Integer>();
        //---------------------
        queue1.add(node1);  //adds nodes to their respective visited and queue lists
        queue2.add(node2);
        visited1.add(node1);
        visited2.add(node2);
        node1Map.put(node1, null);
        node2Map.put(node2, null);
        //---------------------
        Integer currentNode1 = node1; //"pointers" to the current nodes
        Integer currentNode2 = node2;

        while (!visited2.contains(currentNode1) || !visited1.contains(currentNode2)) {  // while visited lists do not overlap
            currentNode1 = queue1.get(0); // sets current node to first element in queue
            queue1.remove(currentNode1); // removes currentNode1 which is the first element from the queue

            for (Integer neighbour1 : board.getSetup().graph.adjacentNodes(currentNode1)) { // for every neighbour to a node
                if (!visited1.contains(neighbour1)) { // if the neighbour is unvisited
                    queue1.add(neighbour1);  // add it the queue
                    visited1.add(neighbour1); // add it neighbour1 to visited
                    node1Map.put(neighbour1, currentNode1); // add previous node entry to map
                }
            }

            currentNode2 = queue2.get(0);
            queue2.remove(currentNode2);

            for (Integer neighbour2 : board.getSetup().graph.adjacentNodes(currentNode2)) {
                if (!visited2.contains(neighbour2)) {
                    queue2.add(neighbour2);
                    visited2.add(neighbour2);
                    node2Map.put(neighbour2, currentNode2);
                }
            }
        }
        Integer overlap = null;
        //-----------------------------------
        if (visited2.contains(currentNode1)){
            overlap = currentNode1;
        }
        else {overlap = currentNode2;}
        //-----------------------------------

        int length = 0;
        Integer ptr = 0;
        while (ptr != null) {
            ptr = node1Map.get(overlap);
            length += 1;
        }
        ptr = 0;
        while (ptr != null) {
            ptr = node2Map.get(overlap);
            length += 1;
        }



        return length;
    }
//----------------------------------------------------------------------------------------------------
    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair)
    {
        // returns a random move, replace with your own implementation
        var moves = board.getAvailableMoves().asList(); // mrx moves

        Map<Move, Integer> score = new HashMap<>(); // map for scores for moves
        for (Move move : moves) {   // goes through mrx moves

            int destination = move.accept(new Move.Visitor<Integer>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            });

            for (Integer location : getDetectiveLocations(board)) {
                score.put(move, biBFS_dist(board, destination, location));
            }
        }



    }
}
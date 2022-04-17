package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class StockFishYard2 implements Ai {

    @Nonnull @Override public String name() { return "StockFishYard2"; }

    //------------------------------------------------------------------------------------------------------------------

    private List<Integer> getDetectiveLocations(Board board) {  // gets the detective locations 
        List<Integer> locations = new ArrayList<>();
        for (Piece piece : board.getPlayers()) {
            if (piece.isDetective()) {
                locations.add(board.getDetectiveLocation((Piece.Detective) piece).orElse(0)); // shouldn't this return nothing instead of 0?
            }
        }
        return locations;
    }
    //------------------------------------------------------------------------------------------------------------------
    @Nonnull private Integer biBFS_dist(@Nonnull Board board, Integer node1, Integer node2){
        //--------------------- for node1
        Map<Integer, Integer>  node1Map = new HashMap<Integer, Integer>(){{put(node1, null);}}; //contains previous node map
        List<Integer> queue1 = new ArrayList<Integer>(List.of(node1));                   // next nodes for search
        List<Integer> visited1 = new ArrayList<Integer>(List.of(node1));                 // visited nodes
        //--------------------- for node2
        Map<Integer, Integer>  node2Map = new HashMap<Integer, Integer>(){{put(node2, null);}};
        List<Integer> queue2 = new ArrayList<Integer>(List.of(node2));
        List<Integer> visited2 = new ArrayList<Integer>(List.of(node2));
        //---------------------
        Integer currentNode1 = node1; //"pointers" to the current nodes
        Integer currentNode2 = node2;

        while (!visited2.contains(currentNode1) || !visited1.contains(currentNode2)) {  // while visited lists do not overlap
            currentNode1 = bfs_loop(board, node1Map, queue1, visited1);
            currentNode2 = bfs_loop(board, node2Map, queue2, visited2);
        }
        //-----------------------------------
        Integer overlap;
        if (visited2.contains(currentNode1)){ //calculates where the overlapping node is for the two sets of visited nodes. This is where they will meet.
            overlap = currentNode1;
        }
        else {overlap = currentNode2;}
        //-----------------------------------

        int length = distCalc(overlap, List.of(node1Map, node2Map));
        return length-1;
    }
    private Integer bfs_loop(@Nonnull Board board, Map<Integer, Integer> nodeMap, List<Integer> queue, List<Integer> visited) {
        Integer currentNode;
        currentNode = queue.get(0); // sets current node to first element in queue
        queue.remove(currentNode); // removes currentNode which is the first element from the queue

        for (Integer neighbour : board.getSetup().graph.adjacentNodes(currentNode)) { // for every neighbour to a node
            if (!visited.contains(neighbour)) { // if the neighbour is unvisited
                queue.add(neighbour);  // add it the queue
                nodeMap.put(neighbour, currentNode); // add previous node entry to map
                visited.add(neighbour); // add it neighbour to visited
            }
        }
        return currentNode;
    }
    private Integer distCalc(Integer overlap, List<Map<Integer, Integer>> nodeMapList){
        int length = 0;

        for(Map<Integer, Integer>nodeMap: nodeMapList) {
            Integer ptr = overlap;
            while (ptr != null) {  //go through the two nodeMaps that point to the previous node of each one in the visited set, and build the path backwards
            ptr = nodeMap.get(ptr);
            length += 1; //with each new node, add 1 to the total length
            }
        }
        return length;
    }

    private double score(){
        // calculates score
        return 0.0;
    }
    //------------------------------------------------------------------------------------------------------------------

    public ImmutableMap<ScotlandYard.Ticket, Integer> ticketMap(Board board, Piece piece){
        List<ScotlandYard.Ticket> ticketTypes = new ArrayList<>(List.of(ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.BUS,ScotlandYard.Ticket.TAXI,ScotlandYard.Ticket.SECRET, ScotlandYard.Ticket.UNDERGROUND));
        Map<ScotlandYard.Ticket, Integer> ticketMap = new HashMap<>();
        for (ScotlandYard.Ticket ticket : ticketTypes) {
            Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
            optTicket.ifPresent(tickValue -> ticketMap.put(ticket ,tickValue.getCount(ticket)));
        }
        return ImmutableMap.copyOf(ticketMap);
    }

    //------------------------------------------------------------------------------------------------------------------

    public List<List<Move>> permutations (Board.GameState gameState, Boolean maximisingPlayer){
        ImmutableSet<Move> allMoves = gameState.getAvailableMoves();
        ImmutableSet<Piece> pieces = gameState.getPlayers();
        Map<Piece, List<Move>> pieceMoves = new HashMap<Piece, List<Move>>();


        if (!maximisingPlayer) {
            for (Piece piece : pieces) {
                if(piece.isDetective()){
                    pieceMoves.put(piece, (allMoves.stream().filter(move -> move.commencedBy() == piece)).collect(Collectors.toList()));
                }
            }
        }
        else {
            for (Piece piece : pieces) {
                if(!piece.isDetective()){
                    pieceMoves.put(piece, allMoves.stream().filter(move -> move.commencedBy() == piece).collect(Collectors.toList()));
                }
            }
        }

        if (!maximisingPlayer){
            return Lists.cartesianProduct(pieceMoves.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
        }
        else {
            return List.of(allMoves.stream().filter(move -> !move.commencedBy().isDetective()).collect(Collectors.toList()));
        }


    }


    public Integer miniMax (List<Move> moves, List<Player> detectives, Player mrX, int depth, int alpha, int beta, Boolean maximisingPlayer, MyGameStateFactory factory, Board board){
        Board.GameState gamestate = null;
            Player newX = null;
            for (Move move : moves) {
                if (move.commencedBy() == mrX.piece()) {
                    newX = mrX.use(move.tickets()).at(move.accept(new DestinationVisitor()));
                }
                else {
                    newX = mrX;
                }
            }
        if (maximisingPlayer) {
            gamestate = factory.build(board.getSetup(), newX, ImmutableList.copyOf(detectives));
        }
        else {
            List<Player> newPlayers = new ArrayList<>();
            for (Player detective : detectives) {
                for (Move move : moves){
                    if (move.commencedBy() == detective.piece()) {
                        newPlayers.add(detective.use(move.tickets()).at(move.accept(new DestinationVisitor())));
                    }
                    else {
                        newPlayers.add(detective);
                    }
                }
            }
            gamestate = factory.build(board.getSetup(), newX, ImmutableList.copyOf(newPlayers));

        }

     if (depth == 0 || !(gamestate.getWinner().isEmpty())) {
         // return evaluation game score statically
     };



    }
//    public List<List<>>
    //------------------------------------------------------------------------------------------------------------------
    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        long start = System.nanoTime(); //for calculating how long a pickMove takes
        var moves = board.getAvailableMoves().asList(); // mrx moves
        //---------------

        MyGameStateFactory factory = new MyGameStateFactory();

        List<Player> detectives = new ArrayList<>();
        Player mrX = null;

        for (Piece piece : board.getPlayers()) {
            if (piece.isDetective()) {
                detectives.add(new Player(piece,
                        ticketMap(board, piece),
                        board.getDetectiveLocation((Piece.Detective) piece).orElse(0)));
            }
            else {
                mrX = new Player(piece,
                        ticketMap(board, piece),
                        moves.get(0).source()
                        );
            }
        }

        Board.GameState first = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectives));




        Map<Move, Integer> scores = new HashMap<>(); // map for scores for moves

        //a new visitor that returns the final destination of a move.
        DestinationVisitor visitor = new DestinationVisitor();

        for (Move move : moves) {   // goes through mrx moves //fills out map of moves to scores
            int destination = move.accept(visitor);

            //the below loop could be added to a helper method called score or something, which gives and array of scores for each move.
            for (Integer location : getDetectiveLocations(board)) { //for each possible move, use mrx's destination and the other detective locations to calculate the score for this move
                scores.put(move, biBFS_dist(board, destination, location));
            }
        }

        //the below code calculates the move that corresponds to the biggest score in the move:score map, and returns it.
        int bestScore = 0;
        Move move = moves.get(0);

        for (Map.Entry<Move, Integer> score : scores.entrySet()) { //go through all of scores
            if (score.getValue() > bestScore) { //if the next score is larger, then take note of it
                bestScore = score.getValue();
                move = score.getKey(); //move is the move that corresponds to this score.
            }
        }

        //info about the decision
        long end = System.nanoTime();
        System.out.println( "Out of possible scores " + scores.values());
        System.out.println("mrX chose " + bestScore);
        System.out.println("taking time " + (end - start)/1000 + " microseconds" +  "\n");

        return move;


    }
}
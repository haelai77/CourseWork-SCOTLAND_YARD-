package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StockFishYardSmall2 implements Ai {

    public static int nodes = 0;

    @Nonnull
    @Override
    public String name() {return "StockFishYardSmall2";}
    //----------------------------------------------------------------------------------------------------
    @Nonnull
    private Integer biBFS_dist(Integer node1, Integer node2) {
        //--------------------- for node1
        Map<Integer, Integer> node1Map = new HashMap<>() {{
            put(node1, null);
        }}; //contains previous node map
        List<Integer> queue1 = new ArrayList<>(List.of(node1));                   // next nodes for search
        List<Integer> visited1 = new ArrayList<>(List.of(node1));                 // visited nodes
        //--------------------- for node2
        Map<Integer, Integer> node2Map = new HashMap<>() {{
            put(node2, null);
        }};
        List<Integer> queue2 = new ArrayList<>(List.of(node2));
        List<Integer> visited2 = new ArrayList<>(List.of(node2));
        //---------------------
        Integer currentNode1 = node1; //"pointers" to the current nodes
        Integer currentNode2 = node2;

        while (!visited2.contains(currentNode1) || !visited1.contains(currentNode2)) {  // while visited lists do not overlap
            currentNode1 = bfs_loop(node1Map, queue1, visited1);
            currentNode2 = bfs_loop(node2Map, queue2, visited2);
        }
        //-----------------------------------
        Integer overlap;
        if (visited2.contains(currentNode1)) { //calculates where the overlapping node is for the two sets of visited nodes. This is where they will meet.
            overlap = currentNode1;
        } else {
            overlap = currentNode2;
        }
        //-----------------------------------

        int length = distCalc(overlap, List.of(node1Map, node2Map));
        return length - 1;
    }

    private Integer bfs_loop(Map<Integer, Integer> nodeMap, List<Integer> queue, List<Integer> visited) {
        Integer currentNode;
        currentNode = queue.get(0); // sets current node to first element in queue
        queue.remove(currentNode); // removes currentNode which is the first element from the queue

        for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(currentNode)) { // for every neighbour to a node
            if (!Setup.getInstance().graph.edgeValue(neighbour, currentNode).equals(ScotlandYard.Transport.FERRY)) {
                if (!visited.contains(neighbour)) { // if the neighbour is unvisited
                    queue.add(neighbour);  // add it the queue
                    nodeMap.put(neighbour, currentNode); // add previous node entry to map
                    visited.add(neighbour); // add it neighbour to visited
                }
            }
        }
        return currentNode;
    }

    private Integer distCalc(Integer overlap, List<Map<Integer, Integer>> nodeMapList) {
        int length = 0;

        for (Map<Integer, Integer> nodeMap : nodeMapList) {
            Integer ptr = overlap;
            while (ptr != null) {  //go through the two nodeMaps that point to the previous node of each one in the visited set, and build the path backwards
                ptr = nodeMap.get(ptr);
                length += 1; //with each new node, add 1 to the total length
            }
        }
        return length;
    }

    private Integer score(SmallGameState gameState) {
        int score = 0;
        for (SmallPlayer detective : gameState.detectives()) {
            score += biBFS_dist(gameState.mrX().location(), detective.location());
        }
        return score;
    }

    //------------------------------------------------------------------------------------------------------------------

    //given a game board, this method converts it into a small game state,
    // which is the object passed throughout the game tree.
    private SmallGameState makeSmallGameState(Board board) {
        List<ScotlandYard.Ticket> ticketTypes = new ArrayList<>(List.of(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));

        //gets the log number of the gamestate.
        int logNumber = board.getMrXTravelLog().size();

        //initialises new mrX and players to be filled by using the information from the board.
        SmallPlayer mrX = null;
        List<SmallPlayer> detectives = new ArrayList<>();

        //an arraylist that holds small ticket board for each player
        List<Integer> log = new ArrayList<>();

        //player id variable for each detective
        byte playerId = 1;

        //for each piece in the board
        for (Piece piece : board.getPlayers()) {
            if (piece.isMrX()) {
                //make a new ticket array that holds the amount
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
                for (ScotlandYard.Ticket ticket : ticketTypes) {
                    optTicket.ifPresent(tickValue -> log.add(tickValue.getCount(ticket)));
                }
                //new player with id 0, at the location of mrX and with the ticket array previously made
                mrX = new SmallPlayer(0, board.getAvailableMoves().asList().get(0).source(), ImmutableList.copyOf(log));
                log.clear();
            }

            else {
                // do the same for each detective, but also assign them an id value from 1 going upwards.
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
                for (ScotlandYard.Ticket ticket : ticketTypes) {
                    optTicket.ifPresent(tickValue -> log.add(tickValue.getCount(ticket)));
                }

                detectives.add(new SmallPlayer(playerId, board.getDetectiveLocation((Piece.Detective) piece).orElse(0), ImmutableList.copyOf(log)));
                playerId += 1;
                log.clear();
            }
        }

        return new SmallGameState(logNumber, mrX, ImmutableList.copyOf(detectives));

    }

    //this is the minimax method, which follows the classic minimax algorithm that we all know and love
    public Integer miniMax(SmallGameState gameState, int depth, int alpha, int beta, Boolean mrXturn, PositionGetter x, PositionGetter d) {
        if ((depth == 0) || gameState.didSomeoneWin(mrXturn)) {
            nodes +=1;
            return score(gameState);
        }

        else if (mrXturn) {
            int maxEval = Integer.MIN_VALUE;
            int eval;
            for (SmallGameState gameState1 : x.getNextPositions(gameState)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, false, x, d);
                maxEval = Integer.max(maxEval, eval);
                alpha = Integer.max(alpha, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            nodes +=1;
            return maxEval;
        }

        else {
            int minEval = Integer.MAX_VALUE;
            int eval;
            for (SmallGameState gameState1 : d.getNextPositions(gameState)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, true, x, d);
                minEval = Integer.min(minEval, eval);
                beta = Integer.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }

            }
            nodes+=1;
            return minEval;
        }
    }

    //the first minimax call, which calls minimax itself. instead of recursively returning the score of the possible gamestates, it returns the location of the best gamestate to choose.
    public Integer firstMiniMax(SmallGameState gameState, int depth, PositionGetter x, PositionGetter d) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int maxEval = Integer.MIN_VALUE;
        int eval;
        SmallGameState g = null;
        for (SmallGameState gameState1 : x.getNextPositions(gameState)) {
            eval = miniMax(gameState1, depth-1, alpha, beta, false, x, d);
            if (eval > maxEval) {
                maxEval = eval;
                g = gameState1;
            }
            alpha = Integer.max(alpha, eval);
            if (beta <= alpha) {
                break;
            }
        }
        nodes +=1;
        assert g != null;
        return g.mrX().location();
    }

    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {

        // setup is a singleton class that holds moves and graph, since these never change across the algorithm
        Setup.getInstance(board.getSetup().moves, board.getSetup().graph);

        //make the starting small gamestate
        SmallGameState start = makeSmallGameState(board);

        DestinationVisitor v = new DestinationVisitor();

        //STRATEGY PATTERN
        PositionGetter xGetter = new PositionGetterMrX();
        PositionGetter dGetter = new PositionGetterDetectives();

        int moveTo = firstMiniMax(start, 3, xGetter, dGetter);

        for (Move move : board.getAvailableMoves().asList()) {
            if (move.accept(v) == moveTo) {
                return move;
            }
        }

        //TODO:
        //unit tests
        //add polymorphism with players and mrx
        //getSmallTicket in global area
        //improve score

        return board.getAvailableMoves().asList().get(0);
    }
}


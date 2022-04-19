package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StockFishYardSmall implements Ai {

    @Nonnull
    @Override
    public String name() {
        return "StockFishYardSmall";
    }

    //------------------------------------------------------------------------------------------------

    private List<Integer> getDetectiveLocations(Board board) {  // gets the detective locations
        List<Integer> locations = new ArrayList<>();
        for (Piece piece : board.getPlayers()) {
            if (piece.isDetective()) {
                locations.add(board.getDetectiveLocation((Piece.Detective) piece).orElse(0)); // shouldn't this return nothing instead of 0?
            }
        }
        return locations;
    }

    //----------------------------------------------------------------------------------------------------
    @Nonnull
    private Integer biBFS_dist(Integer node1, Integer node2) {
        //--------------------- for node1
        Map<Integer, Integer> node1Map = new HashMap<Integer, Integer>() {{
            put(node1, null);
        }}; //contains previous node map
        List<Integer> queue1 = new ArrayList<Integer>(List.of(node1));                   // next nodes for search
        List<Integer> visited1 = new ArrayList<Integer>(List.of(node1));                 // visited nodes
        //--------------------- for node2
        Map<Integer, Integer> node2Map = new HashMap<Integer, Integer>() {{
            put(node2, null);
        }};
        List<Integer> queue2 = new ArrayList<Integer>(List.of(node2));
        List<Integer> visited2 = new ArrayList<Integer>(List.of(node2));
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
            if (Setup.getInstance().graph.edgeValue(neighbour, currentNode).equals(ScotlandYard.Transport.FERRY)) {
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
        for (SmallPlayer detective : gameState.detectives) {
            score += biBFS_dist(gameState.mrX.location, detective.location);
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
                    optTicket.ifPresent(tickValue -> {
                        log.add(tickValue.getCount(ticket));
                    });
                }
                //new player with id 0, at the location of mrX and with the ticket array previously made
                mrX = new SmallPlayer(0, board.getAvailableMoves().asList().get(0).source(), ImmutableList.copyOf(log));
                log.clear();
            }

            else {
                // do the same for each detective, but also assign them an id value from 1 going upwards.
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
                for (ScotlandYard.Ticket ticket : ticketTypes) {
                    optTicket.ifPresent(tickValue -> {
                        log.add(tickValue.getCount(ticket));

                    });
                }

                detectives.add(new SmallPlayer(playerId, board.getDetectiveLocation((Piece.Detective) piece).orElse(0), ImmutableList.copyOf(log)));
                playerId += 1;
                log.clear();
            }
        }

        return new SmallGameState(logNumber, mrX, ImmutableList.copyOf(detectives));

    }

    //given a tyoe of transport in the game, return the index of the ticket array that corresponds to the required ticket
    private Integer getSmallTicket(ScotlandYard.Transport transport) {
        return switch (transport) {
            case TAXI -> 0;
            case BUS -> 1;
            case UNDERGROUND -> 2;
            case FERRY -> 4;
        };
    }

    //given a list of players, tells if there are no players able to move
    private Boolean isTrapped(SmallGameState gameState, ImmutableList<SmallPlayer> players) {
        for (SmallPlayer player : players) {   // for each player
            for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(player.location)) {   //for each of their neighbouring nodes
                int smallTicket = getSmallTicket(Setup.getInstance().graph.edgeValueOrDefault(player.location, neighbour, ImmutableSet.of()).asList().get(0));
                if ((player.tickets.get(smallTicket) > 0) //if the player has the ticket for this
                        && gameState.detectives.stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, player.location()))) { //and no detectives are here
                    return false; //then at least one player can move, and the game can continue for the forseeable future
                }
            }
        }
        return true; // if this is reached then no one can move
    }

    //checks if someone has won the game
    private Boolean didSomeoneWin(SmallGameState gameState, Boolean mrXturn) {

        if (mrXturn) { // if its mr X's turn
            if ((isTrapped(gameState, ImmutableList.of(gameState.mrX))) //if mrX is trapped
                    || (gameState.detectives.stream().map(SmallPlayer::location).anyMatch(x -> Objects.equals(x, gameState.mrX.location())))) { // or detectives have captured mrX
                return true;
            }
            // on the detectives' turn
            else {
                if ((Setup.getInstance().moves.size() == gameState.logNumber) // if the log number has been filled
                        || (isTrapped(gameState, gameState.detectives))) { // or all detectives are trapped
                    return true;
                }
            }

        }

        return false;
    }

    // this method is UNFINISHED AND ANNOYING, hopefully it can use recursion to compute all possible permutations of detective moves
    private ArrayList<SmallGameState> getNextDetectivePositions (SmallGameState gameState, int count, ArrayList<SmallPlayer> existing, ArrayList<SmallGameState> result) {

        if (count > gameState.detectives.size()) {
            SmallGameState another = new SmallGameState(gameState.logNumber + 1, gameState.mrX, ImmutableList.copyOf(existing));
            result.add(another);

            return result;
        }

        else {

            SmallPlayer detective = gameState.detectives.get(count);
            for (int neighbour : Setup.getInstance().graph.adjacentNodes(detective.location())) {
                int smallTicket = getSmallTicket(Setup.getInstance().graph.edgeValueOrDefault(detective.location, neighbour, ImmutableSet.of()).asList().get(0));
                if ((detective.tickets.get(smallTicket) > 0)) {

                    ArrayList<SmallPlayer> next = new ArrayList<>(List.copyOf(existing));
                    next.add(detective.travel(neighbour, smallTicket));

                    result.addAll(getNextDetectivePositions(gameState, count + 1, next, result));

                }

            }
            return result;
        }
    }

    //this method gets the next positions down the minimax tree, given a gameState and the turn.
    private ArrayList<SmallGameState> getNextPositions (SmallGameState gameState, Boolean mrXturn) {
        ArrayList<SmallGameState> result = new ArrayList<>();
        if (mrXturn) {

            for (int neighbour : Setup.getInstance().graph.adjacentNodes(gameState.mrX.location)) { // for each neighbouring node to mrX

                //the transport ticket required (kind of long, new method?)
                int smallTicket = getSmallTicket(Setup.getInstance().graph.edgeValueOrDefault(gameState.mrX.location, neighbour, ImmutableSet.of()).asList().get(0));


                if ((gameState.mrX.tickets.get(smallTicket) > 0 || gameState.mrX.tickets.get(4) > 0) //if mrX has this ticket or a secret ticket
                        && gameState.detectives.stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))) { //and no detectives are at this node

                    SmallGameState singleMoveState = new SmallGameState(gameState.logNumber + 1, gameState.mrX.travel(neighbour, smallTicket), gameState.detectives); // new gamestate with this new move
                    result.add(singleMoveState); //add this to the result

                    if (singleMoveState.mrX.tickets.get(3) > 0) { // if mrX has a double ticket

                        //compute double moves; do the same as with single ticket and add all of these new gamestates to the result.

                        for (int neighbour2 : Setup.getInstance().graph.adjacentNodes(singleMoveState.mrX.location)) {

                            int smallTicket2 = getSmallTicket(Setup.getInstance().graph.edgeValueOrDefault(singleMoveState.mrX.location, neighbour2, ImmutableSet.of()).asList().get(0));

                            if ((singleMoveState.mrX.tickets.get(smallTicket) > 0 || gameState.mrX.tickets.get(4) > 0)
                                    && singleMoveState.detectives.stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour2))) {

                                SmallGameState doubleMoveState = new SmallGameState(gameState.logNumber + 1, gameState.mrX.travel(neighbour, smallTicket), gameState.detectives);
                                result.add(doubleMoveState);
                            }
                        }
                    }
                }
            }
            //sorry for these curly braces

            return result;
        }

        else {

            //in here was going to be getnextpositions for the detectives but this isnt finished,
            // maybe could look into the strategy pattern to alternate between them?

            return result;
        }
    }

    //this is the minimax method, which follows the classic minimax algorithm that we all know and love
    private Integer miniMax(SmallGameState gameState, int depth, int alpha, int beta, Boolean mrXturn) {
        if ((depth == 0) || didSomeoneWin(gameState, mrXturn)) {
            return score(gameState);
        }

        if (mrXturn) {
            int maxEval = Integer.MIN_VALUE;
            int eval;
            for (SmallGameState gameState1 : getNextPositions(gameState, true)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, false);
                maxEval = Integer.max(maxEval, eval);
                alpha = Integer.max(alpha, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        }

        else {
            int minEval = Integer.MAX_VALUE;
            int eval;
            for (SmallGameState gameState1 : getNextPositions(gameState, false)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, true);
                minEval = Integer.min(minEval, eval);
                beta = Integer.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }

            }
            return minEval;
        }
    }

    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {

        // setup is a singleton class that holds moves and graph, since these never change across the algorithm
        Setup.getInstance(board.getSetup().moves, board.getSetup().graph);

        SmallGameState start = makeSmallGameState(board);

        //initial call to minimax
        //then decode what minimax returns to select a move for mrX

        return null;


    }
}
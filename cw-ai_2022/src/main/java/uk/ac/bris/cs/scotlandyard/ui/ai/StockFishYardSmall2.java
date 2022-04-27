package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StockFishYardSmall2 implements Ai {

    @Nonnull
    @Override
    public String name() {return "StockFishYardSmall2";}
    //---------------------------------------------------------------------------------------------------- scoring stuff
    @Nonnull
    private Integer biBFS_dist(Integer node1, Integer node2) { // finds distance
        //--------------------- for node1
        Map<Integer, Integer> node1Map = new HashMap<>() {{put(node1, null);}}; //contains previous node map
        List<Integer> queue1 = new ArrayList<>(List.of(node1)); // next nodes for search
        List<Integer> visited1 = new ArrayList<>(List.of(node1)); // visited nodes
        //--------------------- for node2
        Map<Integer, Integer> node2Map = new HashMap<>() {{put(node2, null);}};
        List<Integer> queue2 = new ArrayList<>(List.of(node2));
        List<Integer> visited2 = new ArrayList<>(List.of(node2));
        //---------------------
        Integer currentNode1 = node1; //"pointers" to the current nodes
        Integer currentNode2 = node2;

        while (!visited2.contains(currentNode1) || !visited1.contains(currentNode2)) {  // while visited lists do not overlap
            currentNode1 = bfs_loop(node1Map, queue1, visited1); // execute bfs from source and destination (i.e. detective, mrx)
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
        int length = distCalc(overlap, List.of(node1Map, node2Map)); //calculates distance between detective and mrx
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
        //score += connectivity(gameState, gameState.mrX().location(), gameState.mrX());
        return score;
    }

    private int connectivity(SmallGameState gameState, int node, SmallPlayer duplicateX){
        int connectivityScore = 0;

        mainLoop:
        for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(node)) { // for every neighbour to the node
            for (SmallPlayer dets : gameState.detectives()){  // checks if neighbours are already occupied by detective take away score ?
                if (Objects.equals(dets.location(), neighbour)) {
                    connectivityScore -= 100;
                    continue mainLoop;
                }
            }
            Optional<ImmutableSet<ScotlandYard.Transport>> transportSet = Setup.getInstance().graph.edgeValue(neighbour, node); // get the transport type(s) between current node and neighbour node(s)
            if (transportSet.isPresent()) {
                for (ScotlandYard.Transport transport : transportSet.get()) {   // for every type of transport between the neighbour and source node
                    if (duplicateX.has(Setup.getSmallTicket(transport))) {   // check if mrX has tickets to move there
                        connectivityScore += 5;
                        break;  //if mrx has a ticket to move there then connectivityScore is increased
                        //connectivityScore += connectivity()
                    }
                }
            }
        }

        return connectivityScore;
    }

    //------------------------------------------------------------------------------------------------------------------

    //given a game board, this method converts it into a small game state,
    // which is the object passed throughout the game tree.
    private SmallGameState makeSmallGameState(Board board) {
        List<ScotlandYard.Ticket> ticketTypes = new ArrayList<>(List.of(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));

        //gets the log size of the gamestate.
        int logNumber = board.getMrXTravelLog().size();

        //initialises new mrX and players to be filled by using the information from the board.
        SmallPlayer mrX = null;
        List<SmallPlayer> detectives = new ArrayList<>();

        //an arraylist that holds small ticket board for each player
        List<Integer> newTickets = new ArrayList<>();

        //player id variable for each detective
        byte playerId = 1;

        //for each piece in the board
        for (Piece piece : board.getPlayers()) {
            if (piece.isMrX()) {
                //make a new ticket array that holds the amount (turns mrX's ticket board into a 'mini ticket board' in a SmallGameStateObject)
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
                for (ScotlandYard.Ticket ticket : ticketTypes) { // for each ticket type add the ticket amount into the log
                    optTicket.ifPresent(tickValue -> newTickets.add(tickValue.getCount(ticket)));
                }
                //------------------------------------------------------------------------------------------------------
                //creates a new smallPlayer which is the equivalent of Player mrX with info (location, ticketBoard)
                mrX = new SmallPlayer(0, board.getAvailableMoves().asList().get(0).source(), ImmutableList.copyOf(newTickets)); //(id, mrxLocation, ticket amounts)
                newTickets.clear();
            }

            else {
                // do the same for each detective, but also assign them an id value from 1 going upwards.
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
                for (ScotlandYard.Ticket ticket : ticketTypes) {
                    optTicket.ifPresent(tickValue -> newTickets.add(tickValue.getCount(ticket)));
                }

                detectives.add(new SmallPlayer(playerId, board.getDetectiveLocation((Piece.Detective) piece).orElse(0), ImmutableList.copyOf(newTickets))); // (id, location, ticket amounts)
                playerId += 1;
                newTickets.clear();
            }
        }

        return new SmallGameState(logNumber, mrX, ImmutableList.copyOf(detectives));

    }

    //this is the minimax method, which follows the classic minimax algorithm that we all know and love
    public Integer miniMax(SmallGameState gameState, int depth, int alpha, int beta, Boolean mrXturn, PositionGetter x, PositionGetter d) {
        int win = gameState.didSomeoneWin(mrXturn);
        if (depth == 0) {
            return score(gameState);
        }

        else if (win != 0) {
            return win;
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

            return minEval;
        }
    }

    //the first minimax call, which calls minimax itself. It returns the location of the best gamestate to choose.
    public Integer firstMiniMax(SmallGameState gameState, int depth, PositionGetter x, PositionGetter d) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int maxEval = Integer.MIN_VALUE;
        int eval;
        SmallGameState g = null;

        HashSet<Integer> deathNodes = new HashSet<>();
        for ( SmallPlayer det : gameState.detectives()){
            deathNodes.addAll(Setup.getInstance().graph.adjacentNodes(det.location()));
        }

        for (SmallGameState gameState1 : x.getNextPositions(gameState)) {
            eval = miniMax(gameState1, depth-1, alpha, beta, false, x, d);
            if (eval >= maxEval) {
                maxEval = eval;
                g = gameState1;
            }
            alpha = Integer.max(alpha, eval);
            if (beta <= alpha) {
                break;
            }
        }

        assert g != null;
        return g.mrX().location();
    }

    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {

        // setup is a singleton class that holds moves and graph, since these never change across the algorithm
        Setup.getInstance(board.getSetup().moves, board.getSetup().graph);

        //make the starting small gamestate
        SmallGameState start = makeSmallGameState(board);

        //VISITOR PATTERN
        DestinationVisitor destinationFinder = new DestinationVisitor(); //used for finding destination of a ticket (single and double moves)
        TicketVisitor ticketFinder = new TicketVisitor();  //used for finding tickets required (single and double moves)

        //STRATEGY PATTERN
        PositionGetter nextPosX = new PositionGetterMrX(); // gets the next favourable positions of mrX
        PositionGetter nextPosDet = new PositionGetterDetectives(); // gets the set of permutations of detectives inside small game states

        int moveTo = firstMiniMax(start, 3, nextPosX, nextPosDet);  // returns the best node to travel to via minimax.

        boolean single = (Setup.getInstance().graph.adjacentNodes(start.mrX().location()).contains(moveTo)); // check if the node is reachable via single move

        //TODO: tidy up :)😋
        ArrayList<Move> moves = new ArrayList<>(); // empty array of moves
        for (Move move : board.getAvailableMoves().asList()) { // for every possible move
            if (move.accept(destinationFinder) == moveTo) { // if the move's destination is equal to the move returned by minmax (optimal move)
                if (single && move.accept(ticketFinder).size() == 1) { // if it's possible to do in a single move filter out double moves
                    moves.add(move);
                }
                else if (!single && move.accept(ticketFinder).size() == 2) { //if it's not possible by singe moves add only double moves
                    moves.add(move);
                }
            }
        }

        boolean hide = ((board.getMrXTravelLog().size() > 0) && (board.getMrXTravelLog().get(board.getMrXTravelLog().size()-1).location().orElse(-1) > -1)); // if the previous was revealed

        if (!hide) { // if previous move wasn't revealed
            for (Move move : moves) { // go through all moves and return the first non-secret move
                    if (!move.accept(ticketFinder).contains(ScotlandYard.Ticket.SECRET)) {
                        return move;
                    }
            }
        }
        else { // if previous move was revealed
            for (Move move : moves) { // go through all the moves and return the first secret move
                    if (move.accept(ticketFinder).contains(ScotlandYard.Ticket.SECRET)) {
                        return move;
                }
            }
        }

        return moves.get(0); //default return first move in possible moves
        //TODO:
        //unit tests
        //add polymorphism with players and mrx
        //getSmallTicket in global area
        //improve score

        //return board.getAvailableMoves().asList().get(0);
    }
}


//TODO: can't escape when deathnode is avoidable by double move
//TODO: improve run time (filter out double moves that lead to the same destination).
//TODO: filter our single moves that lead to the same destination (dunno if this makes a huge difference cus)
//TODO: filter out moves that lead to the same place that are already in the move set.<<<<<<<<<<<<<<

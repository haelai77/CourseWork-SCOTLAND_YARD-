package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StockFishYardSmall2 implements Ai {

    @Nonnull
    @Override
    public String name() {return "StockFishYardSmall2\uD83D\uDE0B";}
    //---------------------------------------------------------------------------------------------------- scoring stuff
    @Nonnull
    private Integer biBreadthFirstSearch(Integer mrxLocation, Integer detectiveLocation) { // finds distance between a single detective and Mrx
        //--------------------- for mrxLocation
        Map<Integer, Integer> mrXPreviousNodes = new HashMap<>() {{put(mrxLocation, null);}}; //maps nodes to their previous node
        Map<Integer, Integer> detectivePreviousNodes = new HashMap<>() {{put(detectiveLocation, null);}};
        // next nodes to be searched
        List<Integer> queue1 = new ArrayList<>(List.of(mrxLocation));
        List<Integer> queue2 = new ArrayList<>(List.of(detectiveLocation));
        // visited nodes
        List<Integer> visited1 = new ArrayList<>(List.of(mrxLocation));
        List<Integer> visited2 = new ArrayList<>(List.of(detectiveLocation));
        //---------------------
        Integer currentMrxLocation = mrxLocation; // holds node currently being searched on mrX's side
        Integer currentDetectiveLocation = detectiveLocation; // holds node currently being searched on detective's side

        while (!visited2.contains(currentMrxLocation) || !visited1.contains(currentDetectiveLocation)) {  // while visited lists do not overlap
            currentMrxLocation = bfsLoop(mrXPreviousNodes, queue1, visited1); // execute 1 bfs loop from source and destination (i.e. detective, mrx)
            currentDetectiveLocation = bfsLoop(detectivePreviousNodes, queue2, visited2);
        }
        //-----------------------------------
        Integer overlap;
        if (visited2.contains(currentMrxLocation)) { //calculates where the overlapping node is for the two sets of visited nodes. This is where they will meet.
            overlap = currentMrxLocation;
        } else {
            overlap = currentDetectiveLocation;
        }
        //-----------------------------------
        int length = distanceCalculator(overlap, List.of(mrXPreviousNodes, detectivePreviousNodes)); //calculates distance between detective and mrx
        return length - 1; // minus 1 because overlap is counted twice when calculating
    }

    private Integer bfsLoop(Map<Integer, Integer> previousNodeMap, List<Integer> queue, List<Integer> visited) {
        Integer currentNode;
        currentNode = queue.get(0); // sets current node to first element in queue
        queue.remove(currentNode); // removes currentNode which is the first element from the queue

        for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(currentNode)) { // for every neighbour to a node
            if (!Setup.getInstance().graph.edgeValue(neighbour, currentNode).equals(ScotlandYard.Transport.FERRY)) {
                if (!visited.contains(neighbour)) { // if the neighbour is unvisited
                    queue.add(neighbour);  // add it the queue
                    previousNodeMap.put(neighbour, currentNode); // add previous node entry to map
                    visited.add(neighbour); // add it neighbour to visited
                }
            }
        }
        return currentNode;
    }

    private Integer distanceCalculator(Integer overlap, List<Map<Integer, Integer>> nodeMapList) { // goes back through
        int length = 0;

        for (Map<Integer, Integer> nodeMap : nodeMapList) { // calculates the distance from the overlapping node back to the detective and mrx
            Integer ptr = overlap;
            while (ptr != null) {  //go through the nodeMaps that point to the previous node of each one in the visited set, and build the path backwards
                ptr = nodeMap.get(ptr);
                length += 1; //with each new node, add 1 to the total length
            }
        }
        return length;
    }

    private Integer score(SmallGameState gameState) {
        int score = 0;
        for (DetSmallPlayer detective : gameState.detectives()) {
            score += biBreadthFirstSearch(gameState.mrX().location(), detective.location());
        }
        score += connectivity(gameState, gameState.mrX().location());
        return score;
    }

    private int connectivity(SmallGameState gameState, int node){
        int connectivityScore = 0;

        mainLoop:
        for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(node)) { // for every neighbour to the node
            for (DetSmallPlayer dets : gameState.detectives()){  // checks if neighbours are already occupied by detective, if so take away score
                if (Objects.equals(dets.location(), neighbour)) {
                    connectivityScore -= 1000;
                    continue mainLoop;
                }
            }
            connectivityScore += 5;
        }
        return connectivityScore;
    }

    //------------------------------------------------------------------------------------------------------------------
    private SmallGameState makeSmallGameState(Board board) { // converts a board into it's smaller counterpart also known as SmallGameState
        List<ScotlandYard.Ticket> ticketTypes = new ArrayList<>(List.of(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));
        //initialises new mrX and players to be filled by using the information from the board.
        MrxSmallPlayer mrX = null;
        List<DetSmallPlayer> detectives = new ArrayList<>();
        List<Integer> newTickets = new ArrayList<>(); //an arraylist that holds small ticket board for each player
        int playerId = 1; //player id variable for each detective
        //----------------------------------------------------------------------------- MAKES A SMALL PLAYER FOR MRX AND DETECTIVES TO GO INTO A SMALLGAMESTATE
        for (Piece piece : board.getPlayers()) { //for each piece in the board
            if (piece.isMrX()) {                 //make a new ticket array that holds the amount (turns mrX's ticket board into a 'mini ticket board' to be put into a small game state)
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece);
                for (ScotlandYard.Ticket ticket : ticketTypes) {    // for each ticket type add the ticket amount into the log
                    optTicket.ifPresent(tickValue -> newTickets.add(tickValue.getCount(ticket)));
                }
                //creates a new smallPlayer which is the equivalent of Player mrX with info (location, ticketBoard)
                mrX = new MrxSmallPlayer(0, board.getAvailableMoves().asList().get(0).source(),  ImmutableList.copyOf(newTickets));
                newTickets.clear();
            }
            else {
                Optional<Board.TicketBoard> optTicket = board.getPlayerTickets(piece); // do the same for each detective, but also assign them an id value from 1 going upwards.
                for (ScotlandYard.Ticket ticket : ticketTypes) {
                    optTicket.ifPresent(tickValue -> newTickets.add(tickValue.getCount(ticket)));
                }
                detectives.add(new DetSmallPlayer(playerId, board.getDetectiveLocation((Piece.Detective) piece).orElse(0), ImmutableList.copyOf(newTickets))); // (id, location, ticket amounts)
                playerId += 1;
                newTickets.clear();
            }
        }
        return new SmallGameState(board.getMrXTravelLog().size(), mrX, ImmutableList.copyOf(detectives));
    }

    //this is the minimax method, which follows the classic minimax algorithm that we all know and love
    private Integer miniMax(SmallGameState gameState, int depth, int alpha, int beta, Boolean mrXturn, PositionGetter x, PositionGetter d, long timelimit, long start) {

        if(System.currentTimeMillis() - start > timelimit) {
            return score(gameState);
        }

        int win = gameState.didSomeoneWin(mrXturn);
        if (win != 0) {return win;}
        else if (depth == 0) {return score(gameState);}

        else if (mrXturn) {
            int maxEval = Integer.MIN_VALUE;
            int eval;
            for (SmallGameState gameState1 : x.getNextPositions(gameState)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, false, x, d, timelimit, start);
                maxEval = Integer.max(maxEval, eval);
                alpha = Integer.max(alpha, eval);
                if (beta <= alpha
                        || System.currentTimeMillis() - start > timelimit) {
                    break;
                }
            }
            return maxEval;
        }
        else {
            int minEval = Integer.MAX_VALUE;
            int eval;
            for (SmallGameState gameState1 : d.getNextPositions(gameState)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, true, x, d, timelimit, start);
                minEval = Integer.min(minEval, eval);
                beta = Integer.min(beta, eval);
                if (beta <= alpha || System.currentTimeMillis() - start > timelimit) {
                    break;
                }
            }
            return minEval;
        }
    }

    //the first minimax call, which calls minimax itself. It returns the location of the best gamestate to choose.
    private Integer firstMiniMax(SmallGameState gameState, int depth, PositionGetter x, PositionGetter d, long timelimit) {
        long start = System.currentTimeMillis();

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int maxEval = Integer.MIN_VALUE;
        int eval;
        SmallGameState g = null;

        for (SmallGameState gameState1 : x.getNextPositions(gameState)) {
            eval = miniMax(gameState1, depth-1, alpha, beta, false, x, d, timelimit, start);
            if (eval == -1) {return -1;}
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

    private Move hiddenMoveFilter(SmallGameState start, Board board, int moveTo, DestinationVisitor destinationFinder, TicketVisitor ticketFinder) {
        boolean single = (Setup.getInstance().graph.adjacentNodes(start.mrX().location()).contains(moveTo)); // check if the node is reachable via single move

        //---------------------------------------------- puts all the moves lead to the same out come provided minimax into an array
        ArrayList<Move> moves = new ArrayList<>();
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
        //-----------------------------------------------calculates if mr x does a hidden after a revealed move
        boolean hide = ((board.getMrXTravelLog().size() > 0) && board.getMrXTravelLog().get(board.getMrXTravelLog().size()-1).location().isPresent()); //check if the previous move was revealed

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
    }

    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        // setup is a singleton class that holds moves and graph, since these never change across the algorithm
        Setup.getInstance(board.getSetup().moves, board.getSetup().graph);

        //convert the timeoutpair to a time in seconds
        TimeUnit time = TimeUnit.MILLISECONDS;
        long timelimit = time.convert(timeoutPair.left(), timeoutPair.right());

        //make the starting small gamestate
        SmallGameState start = makeSmallGameState(board);

        //VISITOR PATTERN
        DestinationVisitor destinationFinder = new DestinationVisitor(); //used for finding destination of a ticket (single and double moves)
        TicketVisitor ticketFinder = new TicketVisitor();  //used for finding tickets required (single and double moves)

        //STRATEGY PATTERN
        PositionGetter nextPosX = new PositionGetterMrX(); // gets the next favourable positions of mrX
        PositionGetter nextPosDet = new PositionGetterDetectives(); // gets the set of permutations of detectives inside small game states

        int moveTo = firstMiniMax(start, 3, nextPosX, nextPosDet, timelimit -250);  // returns the best node to travel to via minimax
        return hiddenMoveFilter(start, board, moveTo, destinationFinder, ticketFinder) ; //default return first move in possible moves

    }
}


//TODO: can't escape when deathnode is avoidable by double move xx
//TODO: improve run time (filter out double moves that lead to the same destination). xx
//TODO: filter our single moves that lead to the same destination (dunno if this makes a huge difference cus) xx
//TODO: filter out moves that lead to the same place that are already in the move set. xx
//TODO: add tests for scoring fucntion. <<<<<<<<<<<<<
//TODO: refactor code
//TODO: fix 0 taxi tickets
//TODO: add 15 second thread timer to default to a move before 15 seconds

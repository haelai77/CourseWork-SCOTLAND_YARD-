package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StockFishYardSmall implements Ai {

    public static int nodes = 0;

    @Nonnull
    @Override
    public String name() {
        return "StockFishYardSmall";
    }

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
        for (SmallPlayer detective : gameState.detectives) {
            score += biBFS_dist(gameState.mrX.location(), detective.location());
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

    //given a type of transport in the game, return the index of the ticket array that corresponds to the required ticket
    //for example, given a TAXI, would return 0, since in a player's ticket array the 0th index holds the number of taxi tickets the player has
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
                for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(player.location, neighbour, ImmutableSet.of()))) {
                    int smallTicket = getSmallTicket(t);
                    if ((player.tickets.get(smallTicket) > 0) //if the player has the ticket for this
                            && gameState.detectives.stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))) { //and no detectives are here
                        return false; //then at least one player can move, and the game can continue for now
                    }
                }
            }
        }
        return true; // if this is reached then no one can move
    }

    //checks if someone has won the game
    private Boolean didSomeoneWin(SmallGameState gameState, Boolean mrXturn) {

        if (mrXturn) { // if its mr X's turn
            // or detectives have captured mrX
            return (isTrapped(gameState, ImmutableList.of(gameState.mrX))) //if mrX is trapped
                    || (gameState.detectives.stream().map(SmallPlayer::location).anyMatch(x -> Objects.equals(x, gameState.mrX.location())));
        }
        // on the detectives' turn
        else {
            // or all detectives are trapped
            return (Setup.getInstance().moves.size() == gameState.logNumber) // if the log number has been filled
                    || (isTrapped(gameState, gameState.detectives));
        }
    }

    // this method is UNFINISHED AND ANNOYING, hopefully it can use recursion to compute all possible permutations of detective moves
    public ArrayList<SmallGameState> getNextDetectivePositions (SmallGameState gameState, int count, ImmutableList<SmallPlayer> existing, ImmutableList<Integer> usedTickets) {

        //int count represents the index of player to calculate the next moves for. if count is 0, it looks at the 0th detective in the gamestate and so on...

        //until count is the size of the number of players:
        //in this case, existing has been filled up with all the players, so a new gamestate of this permutation can be made and is returned as the base of this recursive call
        if (count >= gameState.detectives.size()) {
            SmallGameState newState = new SmallGameState(gameState.logNumber, gameState.mrX.receive(ImmutableList.copyOf(usedTickets)), ImmutableList.copyOf(existing));

            return new ArrayList<>(List.of(newState));
        }

        else {
            //derive the current detective to calculate the next available moves for
            SmallPlayer detective = gameState.detectives.get(count);

            //dontSwap deals with the logic behind disallowing adjacent detectives to swap places.
            //if a player in existing has moved to the location of the detective, then this detective's starting location
            //is stored in an array. this starting location is now unable to be travelled to by the current detective, ie swapping places.
            List<Integer> dontSwap = new ArrayList<>();
            for (SmallPlayer other : existing) {
                if (other.location == detective.location) {
                    dontSwap.add(gameState.getPlayer(other.id).location());
                }
            }

            //a new list of gamestates that is to be returned
            ArrayList<SmallGameState> newStates = new ArrayList<>();

            //foundmove is made true when at least one single viable move for the detective is found, otherwise it raises the flag that
            //the next permutation should include the detective in its original position, with no tickets used.
            boolean foundMove = false;

            for (int neighbour : Setup.getInstance().graph.adjacentNodes(detective.location())) {  //for each of the detectives neighbouring nodes

                if (existing.stream().map(player -> player.location).anyMatch(x -> x == neighbour)  //if any player in existing already occupies this space
                        || (!dontSwap.isEmpty() && dontSwap.stream().anyMatch(x -> x == neighbour))) { //if any location in dontSwap matches this space
                    continue;
                }

                for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(detective.location, neighbour, ImmutableSet.of()))) { //for each transport type between these nodes

                    ArrayList<SmallPlayer> newExisting = new ArrayList<>(List.copyOf(existing)); //make a new existing to put this new player in

                    int smallTicket = getSmallTicket(t); //the ticket index

                    if ((detective.tickets.get(smallTicket) > 0)) { //if the detective has this ticket

                        ArrayList<Integer> newUsedTickets = new ArrayList<>(List.copyOf(usedTickets)); //copy of used tickets to give to mrX
                        newUsedTickets.set(smallTicket, newUsedTickets.get(smallTicket) + 1); //add this ticket to the usedtickets
                        newExisting.add(detective.travel(neighbour, List.of(smallTicket)));//add to existing the new player at this location, having used up a ticket

                        newStates.addAll(getNextDetectivePositions(gameState, count + 1, ImmutableList.copyOf(newExisting), ImmutableList.copyOf(newUsedTickets))); //call a recursive call of this method with existing having this new detective and count incremeneted by 1

                        foundMove = true; //foundmove is true since a viable move has been reached.
                    }
                }
            }


            if (!foundMove && dontSwap.isEmpty()) { //if a move hasnt been found, detective is trapped. if dontswap isnt empty, then that means a detective in existing is overlapping with it and it itself is trapped, so this is not valid
            //however if dontswap is empty, then a new permutation can be made with the detective not having moved.
                    ArrayList<SmallPlayer> newExisting = new ArrayList<>(List.copyOf(existing));
                    newExisting.add(detective);
                    newStates.addAll(getNextDetectivePositions(gameState, count + 1, ImmutableList.copyOf(newExisting), usedTickets));
            }

            return newStates; //return the new states.
        }
    }

    //this method gets the next positions down the minimax tree, given a gameState and the turn.
    public ArrayList<SmallGameState> getNextMrXPositions (SmallGameState gameState) {
        ArrayList<SmallGameState> result = new ArrayList<>();
        for (int neighbour : Setup.getInstance().graph.adjacentNodes(gameState.mrX.location)) { // for each neighbouring node to mrX
            for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(gameState.mrX.location, neighbour, ImmutableSet.of()))) {
                //the transport ticket required
                int smallTicket = getSmallTicket(t);

                if ((gameState.mrX.tickets.get(smallTicket) > 0 || gameState.mrX.tickets.get(4) > 0) //if mrX has this ticket or a secret ticket
                        && gameState.detectives.stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))) { //and no detectives are at this node

                    SmallGameState singleMoveState = new SmallGameState(gameState.logNumber + 1, gameState.mrX.travel(neighbour, List.of(smallTicket)), gameState.detectives); // new gamestate with this new move
                    result.add(singleMoveState); //add this to the result

                    if (singleMoveState.mrX.tickets.get(3) > 0) { // if mrX has a double ticket

                        //compute double moves; do the same as with single ticket and add all of these new gamestates to the result.

                        for (int neighbour2 : Setup.getInstance().graph.adjacentNodes(singleMoveState.mrX.location)) {
                            for (ScotlandYard.Transport t2 : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(singleMoveState.mrX.location, neighbour2, ImmutableSet.of()))) {
                                int smallTicket2 = getSmallTicket(t2);
                                if ((singleMoveState.mrX.tickets.get(smallTicket2) > 0 || gameState.mrX.tickets.get(4) > 0)
                                        && singleMoveState.detectives.stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour2))) {

                                    SmallGameState doubleMoveState = new SmallGameState(gameState.logNumber + 1, gameState.mrX.travel(neighbour2, List.of(smallTicket2, 3)), gameState.detectives);
                                    result.add(doubleMoveState);
                                }
                            }
                        }
                    }
                }
            }
        }
        //sorry for these curly braces

        return result;
    }

    //this is the minimax method, which follows the classic minimax algorithm that we all know and love
    public Integer miniMax(SmallGameState gameState, int depth, int alpha, int beta, Boolean mrXturn) {
        if ((depth == 0) || didSomeoneWin(gameState, mrXturn)) {
            nodes +=1;
            return score(gameState);
        }

        else if (mrXturn) {
            int maxEval = Integer.MIN_VALUE;
            int eval;
            for (SmallGameState gameState1 : getNextMrXPositions(gameState)) {
                eval = miniMax(gameState1, depth-1, alpha, beta, false);
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
            for (SmallGameState gameState1 : getNextDetectivePositions(gameState, 0, ImmutableList.of(), ImmutableList.of(0,0,0))) {
                eval = miniMax(gameState1, depth-1, alpha, beta, true);
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
    public Integer firstMiniMax(SmallGameState gameState, int depth) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int maxEval = Integer.MIN_VALUE;
        int eval;
        SmallGameState g = null;
        for (SmallGameState gameState1 : getNextMrXPositions(gameState)) {
            eval = miniMax(gameState1, depth-1, alpha, beta, false);
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
        return g.mrX.location();
    }

    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {

        // setup is a singleton class that holds moves and graph, since these never change across the algorithm
        Setup.getInstance(board.getSetup().moves, board.getSetup().graph);

        //make the starting small gamestate
        SmallGameState start = makeSmallGameState(board);

        DestinationVisitor v = new DestinationVisitor();

        int moveTo = firstMiniMax(start, 3);

        for (Move move : board.getAvailableMoves().asList()) {
            if (move.accept(v) == moveTo) {
                return move;
            }
        }

        //TODO:
        //unit tests
        //add polymorphism with players and mrx
        //strategy pattern with getnextpositions

        return board.getAvailableMoves().asList().get(0);
    }
}
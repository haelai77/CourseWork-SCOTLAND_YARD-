package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PositionGetterMrX implements PositionGetter {

//    private ArrayList<SmallGameState> getNextSinglePositions(SmallGameState gameState) {
//
//    }

    @Override
    public ArrayList<SmallGameState> getNextPositions(SmallGameState gameState) {

        ArrayList<SmallGameState> result = new ArrayList<>();
        SmallGameState sureDeath = null;

        //--------------------------------------------------------------------------------------------------------------
        HashSet<Integer> deathNodes = new HashSet<>();  //holds all nodes in which mrx can be taken on next turn and detective locations
        for ( SmallPlayer det : gameState.detectives()){
            deathNodes.addAll(Setup.getInstance().graph.adjacentNodes(det.location()));
        }
        deathNodes.addAll(gameState.detectives().stream().map(SmallPlayer::location).toList());
        //--------------------------------------------------------------------------------------------------------------

        HashSet<Integer> closeBy = new HashSet<>(Setup.getInstance().graph.adjacentNodes(gameState.mrX().location())); // set of nodes adjacent to mrX
        boolean detectiveClose = gameState.detectives().stream().map(SmallPlayer::location).anyMatch(closeBy::contains); // boolean to check if detective is on adjacent node to mrX

        for (int neighbour : closeBy) { // for each adjacent node to mrX
            for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(gameState.mrX().location(), neighbour, ImmutableSet.of()))) { // for each type of transport between 2 nodes
                int smallTicket = Setup.getSmallTicket(t); //the transport ticket required to go between nodes
                //------------------------------------------------------------------------------------------------------
                if (gameState.mrX().has(smallTicket) //if mrX has this ticket
                        && gameState.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour)))// if neighbour node isn't occupied by detective
                {
                    SmallGameState singleMoveState = new SmallGameState(gameState.logNumber() + 1, gameState.mrX().travel(neighbour, List.of(smallTicket)), gameState.detectives()); // new gamestate with mrx moved to neighbour

                    if (deathNodes.stream().noneMatch(deathNode -> Objects.equals(deathNode, singleMoveState.mrX().location()))) { // if mrX's location isn't a death node
                        result.add(singleMoveState); //add this to the result
                    }

                    else if (sureDeath == null) {
                        sureDeath = singleMoveState;
                    }

                    if (detectiveClose && gameState.mrX().has(3)) { // if mrX has a double ticket && detective is next to mrX
                        //compute double moves; do the same as with single ticket and add all of these new gamestates to the result.
                        for (int neighbour2 : Setup.getInstance().graph.adjacentNodes(singleMoveState.mrX().location())) {
                            for (ScotlandYard.Transport t2 : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(singleMoveState.mrX().location(), neighbour2, ImmutableSet.of()))) {
                                int smallTicket2 = Setup.getSmallTicket(t2);
                                if ((singleMoveState.mrX().has(smallTicket2))
                                        && singleMoveState.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour2))) {
                                    SmallGameState doubleMoveState = new SmallGameState(gameState.logNumber() + 1, gameState.mrX().travel(neighbour2, List.of(smallTicket2, 3)), gameState.detectives());

                                    if (deathNodes.stream().noneMatch(deathNode -> Objects.equals(deathNode, doubleMoveState.mrX().location()))) { // if mrX's location isn't a death node
                                        result.add(doubleMoveState); //add this to the result
                                    }

                                    else if (sureDeath == null) {
                                        sureDeath = doubleMoveState;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                //------------------------------------------------------------------------------------------------------
                    break;
                }
            }
        }
        if (result.isEmpty()) {
            result.add(sureDeath);
        }

        //sorry for these curly braces

        return result;
    }
}
package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PositionGetterMrX implements PositionGetter {

    Boolean detCloseBy = false;

    @Override
    public ArrayList<SmallGameState> getNextPositions(SmallGameState gameState) {

        ArrayList<SmallGameState> result = new ArrayList<>();
        SmallGameState iWelcomeDeath = null;

        HashSet<Integer> deathNodes = new HashSet<>();
        for ( SmallPlayer det : gameState.detectives()){
            deathNodes.addAll(Setup.getInstance().graph.adjacentNodes(det.location()));
        }
        deathNodes.addAll(gameState.detectives().stream().map(SmallPlayer::location).toList());


        HashSet<Integer> closeBy = new HashSet<>(Setup.getInstance().graph.adjacentNodes(gameState.mrX().location()));
        boolean detectiveClose = gameState.detectives().stream().map(SmallPlayer::location).anyMatch(closeBy::contains);

        for (int neighbour : closeBy) { // for each neighbouring node to mrX
            for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(gameState.mrX().location(), neighbour, ImmutableSet.of()))) {
                //the transport ticket required
                int smallTicket = Setup.getSmallTicket(t);

                if ((gameState.mrX().tickets().get(smallTicket) > 0 || gameState.mrX().tickets().get(4) > 0) //if mrX has this ticket or a secret ticket
                        && gameState.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))
                         // if mrX neighbours are all death nodes
                ) { //and no detectives are at this node

                    SmallGameState singleMoveState = new SmallGameState(gameState.logNumber() + 1, gameState.mrX().travel(neighbour, List.of(smallTicket)), gameState.detectives()); // new gamestate with this new move

                    if (deathNodes.stream().noneMatch(x -> Objects.equals(x, singleMoveState.mrX().location()))) {
                        result.add(singleMoveState); //add this to the result
                    }
                    else if (iWelcomeDeath == null) {
                        iWelcomeDeath = singleMoveState;
                    }

                        if (detectiveClose && gameState.mrX().has(3)) { // if mrX has a double ticket && detective is next to mrX

                            //compute double moves; do the same as with single ticket and add all of these new gamestates to the result.

                            for (int neighbour2 : Setup.getInstance().graph.adjacentNodes(singleMoveState.mrX().location())) {
                                for (ScotlandYard.Transport t2 : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(singleMoveState.mrX().location(), neighbour2, ImmutableSet.of()))) {
                                    int smallTicket2 = Setup.getSmallTicket(t2);
                                    if ((singleMoveState.mrX().has(smallTicket2) || gameState.mrX().has(4)) //4 is a secret ticket
                                            && singleMoveState.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour2))) {

                                        SmallGameState doubleMoveState = new SmallGameState(gameState.logNumber() + 1, gameState.mrX().travel(neighbour2, List.of(smallTicket2, 3)), gameState.detectives());
                                        result.add(doubleMoveState);
                                    }
                                }
                            }
                        }
                }
            }
        }
        if (result.isEmpty()) {
            result.add(iWelcomeDeath);
        }

        //sorry for these curly braces

        return result;
    }
}
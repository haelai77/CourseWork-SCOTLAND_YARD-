package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PositionGetterMrX implements PositionGetter {

    @Override
    public ArrayList<SmallGameState> getNextPositions(SmallGameState gameState) {
        ArrayList<SmallGameState> result = new ArrayList<>();
        for (int neighbour : Setup.getInstance().graph.adjacentNodes(gameState.mrX().location())) { // for each neighbouring node to mrX
            for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(gameState.mrX().location(), neighbour, ImmutableSet.of()))) {
                //the transport ticket required
                int smallTicket = Setup.getSmallTicket(t);

                if ((gameState.mrX().tickets().get(smallTicket) > 0 || gameState.mrX().tickets().get(4) > 0) //if mrX has this ticket or a secret ticket
                        && gameState.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))) { //and no detectives are at this node

                    SmallGameState singleMoveState = new SmallGameState(gameState.logNumber() + 1, gameState.mrX().travel(neighbour, List.of(smallTicket)), gameState.detectives()); // new gamestate with this new move
                    result.add(singleMoveState); //add this to the result

                    if (gameState.mrX().has(3)) { // if mrX has a double ticket

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
        //sorry for these curly braces

        return result;
    }
}
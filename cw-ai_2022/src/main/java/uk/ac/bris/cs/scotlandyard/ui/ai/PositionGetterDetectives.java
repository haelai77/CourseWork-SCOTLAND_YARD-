package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PositionGetterDetectives implements PositionGetter{

    public ArrayList<SmallGameState> getNextDetectivePositions (SmallGameState gameState, int count, ImmutableList<SmallPlayer> existing, ImmutableList<Integer> usedTickets) {

        //int count represents the index of player to calculate the next moves for. if count is 0, it looks at the 0th detective in the gamestate and so on...

        //until count is the size of the number of players:
        //in this case, existing has been filled up with all the players, so a new gamestate of this permutation can be made and is returned as the base of this recursive call
        if (count >= gameState.detectives().size()) {
            SmallGameState newState = new SmallGameState(gameState.logNumber(), gameState.mrX().receive(ImmutableList.copyOf(usedTickets)), ImmutableList.copyOf(existing));

            return new ArrayList<>(List.of(newState));
        }

        else {
            //derive the current detective to calculate the next available moves for
            SmallPlayer detective = gameState.detectives().get(count);

            //dontSwap deals with the logic behind disallowing adjacent detectives to swap places.
            //if a player in existing has moved to the location of the detective, then this detective's starting location
            //is stored in an array. this starting location is now unable to be travelled to by the current detective, ie swapping places.
            List<Integer> dontSwap = new ArrayList<>();
            for (SmallPlayer other : existing) {
                if (Objects.equals(other.location(), detective.location())) {
                    dontSwap.add(gameState.getPlayer(other.id()).location());
                }
            }

            //a new list of gamestates that is to be returned
            ArrayList<SmallGameState> newStates = new ArrayList<>();

            //foundmove is made true when at least one single viable move for the detective is found, otherwise it raises the flag that
            //the next permutation should include the detective in its original position, with no tickets used.
            boolean foundMove = false;

            for (int neighbour : Setup.getInstance().graph.adjacentNodes(detective.location())) {  //for each of the detectives neighbouring nodes

                if (existing.stream().map(SmallPlayer::location).anyMatch(x -> x == neighbour)  //if any player in existing already occupies this space
                        || (!dontSwap.isEmpty() && dontSwap.stream().anyMatch(x -> x == neighbour))) { //if any location in dontSwap matches this space
                    continue;
                }

                for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(detective.location(), neighbour, ImmutableSet.of()))) { //for each transport type between these nodes

                    ArrayList<SmallPlayer> newExisting = new ArrayList<>(List.copyOf(existing)); //make a new existing to put this new player in

                    int smallTicket = Setup.getSmallTicket(t); //the ticket index

                    if (detective.has(smallTicket)) { //if the detective has this ticket

                        ArrayList<Integer> newUsedTickets = new ArrayList<>(List.copyOf(usedTickets)); //copy of used tickets to give to mrX
                        newUsedTickets.set(smallTicket, newUsedTickets.get(smallTicket) + 1); //add this ticket to the usedtickets
                        newExisting.add(detective.travel(neighbour, List.of(smallTicket)));//add to existing the new player at this location, having used up a ticket

                        newStates.addAll(getNextDetectivePositions(gameState, count + 1, ImmutableList.copyOf(newExisting), ImmutableList.copyOf(newUsedTickets))); //call a recursive call of this method with existing having this new detective and count incremeneted by 1

                        foundMove = true; //foundmove is true since a viable move has been reached.
                        break;
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

    @Override
    public ArrayList<SmallGameState> getNextPositions(SmallGameState gameState) {
        return getNextDetectivePositions(gameState, 0, ImmutableList.of(), ImmutableList.of(0,0,0));
    }
}


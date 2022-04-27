package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.Objects;

//A small version of a gamestate. Holds small players and the log number representing the number of turns taken.
public class SmallGameState {
    private final int logNumber;
    private final SmallPlayer mrX;
    private final ImmutableList<SmallPlayer> detectives;

    SmallGameState(Integer logNumber, SmallPlayer mrX, ImmutableList<SmallPlayer> detectives){
        this.logNumber = logNumber;
        this.mrX = mrX;
        this.detectives = detectives;
    }

    public int logNumber() {return this.logNumber;}

    public SmallPlayer mrX() {return this.mrX;}

    public ImmutableList<SmallPlayer> detectives() {return this.detectives;}

    SmallPlayer getPlayer(int id) {
        if (id == 0) {return this.mrX;}
        else {
            for (SmallPlayer player : detectives) {
                if (player.id() == id)
                    return player;
            }
        }
        return null;
    }

    //given a list of players, tells if there are no players able to move
    private Boolean isTrapped(Boolean mrX) { // optimise

        if (mrX) {
            for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(this.mrX.location())) {   //for each of their neighbouring nodes
                for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(this.mrX.location(), neighbour, ImmutableSet.of()))) {
                    int smallTicket = Setup.getSmallTicket(t);
                    if ((this.mrX.tickets().get(smallTicket) > 0) //if the player has the ticket for this
                            && this.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))) { //and no detectives are here
                        return false; //then at least one player can move, and the game can continue for now
                    }
                }
            }
        }
        else {
            for (SmallPlayer player : this.detectives) {   // for each player
                for (Integer neighbour : Setup.getInstance().graph.adjacentNodes(player.location())) {   //for each of their neighbouring nodes
                    for (ScotlandYard.Transport t : Objects.requireNonNull(Setup.getInstance().graph.edgeValueOrDefault(player.location(), neighbour, ImmutableSet.of()))) {
                        int smallTicket = Setup.getSmallTicket(t);
                        if ((player.tickets().get(smallTicket) > 0) //if the player has the ticket for this
                                && this.detectives().stream().map(SmallPlayer::location).noneMatch(x -> Objects.equals(x, neighbour))) { //and no detectives are here
                            return false; //then at least one player can move, and the game can continue for now
                        }
                    }
                }
            }
        }
        return true;
    }

    //checks if someone has won the game
    public Integer didSomeoneWin(Boolean mrXturn) {

        if (mrXturn) { // if its mr X's turn
            if (this.isTrapped(true) //if mrX is trapped
                    || (this.detectives().stream().map(SmallPlayer::location).anyMatch(x -> Objects.equals(x, this.mrX().location())))) {
                return Integer.MIN_VALUE;
            } // or detectives have captured mrX
            else return 0;
        }
        // on the detectives' turn
        else {
            if (Setup.getInstance().moves.size() == this.logNumber() // if the log number has been filled
                    || (this.isTrapped(false))) {
                return Integer.MAX_VALUE;// or all detectives are trapped
            }
            else return 0;
        }
    }

}

package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;

//A small version of a gamestate. Holds small players and the log number representing the number of turns taken.
public class SmallGameState {
    int logNumber;
    final SmallPlayer mrX;
    ImmutableList<SmallPlayer> detectives;

    SmallGameState(Integer logNumber, SmallPlayer mrX, ImmutableList<SmallPlayer> detectives){
        this.logNumber = logNumber;
        this.mrX = mrX;
        this.detectives = detectives;
    }


    SmallPlayer getPlayer(int id) {
        if (id == 0) {return this.mrX;}
        else {
            for (SmallPlayer player : detectives) {
                if (player.id == id)
                    return player;
            }
        }
        return null;
    }

}

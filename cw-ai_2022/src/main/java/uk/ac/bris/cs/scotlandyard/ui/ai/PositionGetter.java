package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;

public interface PositionGetter {
    ArrayList<SmallGameState> getNextPositions(SmallGameState gameState);
}

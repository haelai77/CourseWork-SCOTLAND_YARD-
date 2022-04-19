package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

// setup is a singleton class that holds moves and graph that are found in the setup of board, since these never change across the algorithm
public class Setup {
    final ImmutableList<Boolean> moves;
    final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;
    static Setup setup;

    private Setup(ImmutableList<Boolean> moves, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        this.moves = moves;
        this.graph = graph;
    }

    static void getInstance(ImmutableList<Boolean> moves, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        if (setup == null) {
            setup = new Setup(moves, graph);
        }
    }

    static Setup getInstance() {
        return setup;
    }
}
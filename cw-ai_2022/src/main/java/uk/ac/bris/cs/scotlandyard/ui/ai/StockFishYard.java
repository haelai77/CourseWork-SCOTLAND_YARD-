package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class StockFishYard implements Ai {

	@Nonnull @Override public String name() { return "StockFishYard"; }


	private List<Integer> getDetectiveLocations(Board board) {
		List<Integer> locations = new ArrayList<>();
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				locations.add(board.getDetectiveLocation((Piece.Detective) piece).orElse(0));
			}
		}
		return locations;
	}

	private Integer score(Board board, Integer source) {
		ArrayList<Integer> queue = new ArrayList<>();
		queue.add(source);

		ArrayList<Integer> visited = new ArrayList<>();
		visited.add(source);
		
		ArrayList<Integer> prev = new ArrayList<>();
		for (int i = 0; i < board.getSetup().graph.nodes().size()+1; i++) {
			prev.add(0);
		}

		int node;
		while(!queue.isEmpty()) {

			node = queue.get(0);	// make the node we analyse the one at the front of the queue
			queue.remove(0);	// we can now remove this node from the queue
			for(Integer neighbour : board.getSetup().graph.adjacentNodes(node)) { // goes through all adjacent nodes to a
				if (!visited.contains(neighbour)) {//if neighbour is a new unvisited node,
					queue.add(neighbour); // add neighbour to the queue
					visited.add(neighbour); // make the neighbours a visited so they won't be visited again
					prev.set(neighbour, node); // make the previous nodes of each neighbour the current node
				}
			}
		}

		// here we build the paths and calculate the path lengths from each detective to mrX
		int lengths = 0;

		for (Integer location : getDetectiveLocations(board)) { //for each location, access its previous nodes all the way down
			int length = 0;
			int ptr = location;
			while (ptr != source) { //until you reach the original source, increase length by one for each node and change pointer to the next previous node
				length +=1;
				ptr = prev.get(ptr);
			}
			lengths += length; //add this length to the total lengths
		}

		return lengths; //return the total lengths
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		var moves = board.getAvailableMoves().asList(); //list of all possible moves
		List<Integer> scores = new ArrayList<>();
		for (int i = 0; i < moves.size(); i++) { //for each move, use visitor pattern to discern the final destination
			Integer destination = moves.get(i).accept(new Move.Visitor<>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			} );
			int score = score(board, destination); //use the destination and board with other detective location to calculate total score
			scores.add(score); //add this to the array of scores
		}

		//below is an inefficient way to calculate the move with the highest score. should replace this.
		int bestScore = 0;
		int bestScoreIndex = 0;
		for (int score : scores) {
			if (score > bestScore) {
				bestScore = score;
				bestScoreIndex = scores.indexOf(bestScore);
			}
		}
//		System.out.println(scores);
//		System.out.println(bestScore);
//
//		System.out.println(" ");

		return moves.get(bestScoreIndex);



	}
}

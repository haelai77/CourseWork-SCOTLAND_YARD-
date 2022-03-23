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
			node = queue.get(0);	//make the node we analyse the one at the front of the queue
			queue.remove(0);	//we can now remove this node from the queue
			for(Integer neighbour : board.getSetup().graph.adjacentNodes(node)) { // goes through all adjacent nodes to a
				if (!visited.contains(neighbour)) {//if neighbour is a new unvisited node,
					//add neighbour to the queue
					queue.add(neighbour);
					//make the neighbours a visited so they won't be visited again
					visited.add(neighbour);
					//make the previous nodes of each neighbour the current node
					prev.set(neighbour, node);
				}
			}
		}

		// here we build the paths and calculate the path lengths for each
		int lengths = 0;
		for (Integer location : getDetectiveLocations(board)) {
			int length = 0;
			int ptr = location;
			while (ptr != source) {
				length +=1;
				ptr = prev.get(ptr);
			}
			lengths += length;
		}

		return lengths;
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
//
		int nodeTotal = board.getSetup().graph.nodes().size();

		var moves = board.getAvailableMoves().asList();
		List<Integer> scores = new ArrayList<>();
		for (int i = 0; i < moves.size(); i++) {
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
			int score = score(board, destination);
			scores.add(score);
		}
		int bestScore = 0;
		int bestScoreIndex = 0;
		for (int score : scores) {
			if (score > bestScore) {
				bestScore = score;
				bestScoreIndex = scores.indexOf(bestScore);
			}
		}
		System.out.println(scores);
		System.out.println(bestScore);

		System.out.println(" ");

		//Move move = moves.get(new Random().nextInt(moves.size()));

//		System.out.println("scores" + score(board, move.source()));
//		System.out.println("locations" + getDetectiveLocations(board));

		return moves.get(bestScoreIndex);



	}
}

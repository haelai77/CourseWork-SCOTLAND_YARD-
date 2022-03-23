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

//		ArrayList<Integer> locations = new ArrayList(List.copyOf(getDetectiveLocations(board)));
//		ArrayList<Integer> ignoreLocations = new ArrayList<>();


		ArrayList<Integer> prev = new ArrayList<>();
		for (int i = 0; i < board.getSetup().graph.nodes().size()+1; i++) {
			prev.add(0);
		}

		int node;
//		int length = 1;
		while(!queue.isEmpty()) {
			node = queue.get(0);
			queue.remove(0);
			for(Integer neighbour : board.getSetup().graph.adjacentNodes(node)) {
				if (!visited.contains(neighbour)) {
					queue.add(neighbour);
					visited.add(neighbour);
					prev.set(neighbour, node);

//					for(Integer location : locations) {
//						if (Objects.equals(location, neighbour) && (!ignoreLocations.contains(location))) {
//							ignoreLocations.add(location);
//							lengths.add(length);
//						}


				}

			}
		}
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

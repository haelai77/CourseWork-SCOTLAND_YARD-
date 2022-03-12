package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.*;
import javax.annotation.Nonnull;

import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;




/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	
	private final class MyGameState implements GameState{

		private GameSetup setup; 			  // Thing to return
		//-------------------------------------------------------
		private Player mrX;		 			  // Holds Mr. x
		private List<Player> detectives;      // Detectives
		//-------------------------------------------------------
		private ImmutableSet<Piece> remaining;// Pieces remaining
		private ImmutableSet<Piece> winner;   // Current Winner(s)
		//-------------------------------------------------------
		private ImmutableList<LogEntry> log;  // Mr. x's move log
		private ImmutableSet<Move> moves;	  // Holds the current possible moves
		//--------------------------------------------------------------------------------------------------------------

		private static void verifyNotNull(Collection maybeNull, String var){
			//if (maybeNull == null)  { throw new NullPointerException(String.format("%s", var)); }
			if (maybeNull.isEmpty()){ throw new IllegalArgumentException(String.format("%s v1", var)); }
		}
		private static void verifyNotNull(Object maybeNull, String var){
			if (maybeNull == null){ throw new IllegalArgumentException(String.format("%s v2", var)); }
		}
//		private static void verifyGraphSupplied(GameState setup) throws IOException {
//			if (!(setup.getSetup().graph.equals(ScotlandYard.standardGraph()))){
//				throw new IllegalArgumentException("Graph is not standard");
//			}
//		}

		private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives)
		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			verifyNotNull(setup.moves, "You don't have ");
			verifyNotNull(setup.graph, "There's no graph");
			verifyNotNull(remaining, "remaining pieces");
			verifyNotNull(log, "log empty");

			//not sure this MRX stuff does anything.
//			verifyNotNull(mrX.piece(), "MrX.piece is Null");
//			verifyNotNull(mrX.location(), "MrX.location is Null");
//			verifyNotNull(mrX.tickets(), "MrX.tickets is Null");
//			verifyNotNull(mrX.tickets(), "MrX.tickets is Null");

			verifyNotNull(detectives, "There are no detectives");



		}

		@Nonnull @Override //TODO
		public GameSetup getSetup() {
			return null;
		}

		@Nonnull @Override //TODO
		public ImmutableSet<Piece> getPlayers() {
			return null;
		}

		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			return null;
		}

		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			return null;
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return null;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {
			return null;
		}

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			return null;
		}

		@Nonnull @Override //from interface GameState //TODO
		public GameState advance(Move move) {
			return null;
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		// TODO
		return new MyGameState(setup,
							   ImmutableSet.of(MrX.MRX), // remaining pieces
							   ImmutableList.of(), 		 // log
							   mrX,
							   detectives);
	}




}

package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import static com.google.common.base.Verify.verifyNotNull;


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
			this.detectives = detectives;	// idk if verifyNotNUll works (com.google.guava:guava:30.1.1-jre (guava-30.1.1-jre.jar))
			// the below throw verifyExceptions, we want IllegalArgumentExceptions //TODO
			verifyNotNull(setup, "%s is empty", "setup");
			verifyNotNull(remaining, "%s is empty", "remaining");
			verifyNotNull(log, "%s is empty", "log");
			verifyNotNull(mrX, "%s is empty", "mrX");
			verifyNotNull(detectives, "%s is empty", "detectives");
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

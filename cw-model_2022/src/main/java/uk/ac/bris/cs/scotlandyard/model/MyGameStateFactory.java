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
		private List<Player> allPlayers;
		//-------------------------------------------------------
		private ImmutableSet<Piece> remaining;// Pieces remaining
		private ImmutableSet<Piece> winner;   // Current Winner(s)
		//-------------------------------------------------------
		private ImmutableList<LogEntry> log;  // Mr. x's move log
		private ImmutableSet<Move> moves;	  // Holds the current possible moves
		//--------------------------------------------------------------------------------------------------------------

//		private static void verifyNotNull(Collection maybeNull, String var){
//			//if (maybeNull == null)  { throw new NullPointerException(String.format("%s", var)); }
//			if (maybeNull.isEmpty()){ throw new IllegalArgumentException(String.format("%s v1", var)); }
//		}
//		private static void verifyNotNull(Object maybeNull, String var){
//			if (maybeNull == null){ throw new IllegalArgumentException(String.format("%s v2", var)); }
//		}
//		private static void verifyGraphSupplied(GameState setup) throws IOException {
//			if (!(setup.getSetup().graph.equals(ScotlandYard.standardGraph()))){
//				throw new IllegalArgumentException("Graph is not standard");
//			}
//		}

		private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining, // remaining pieces
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives
		)
		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();

			this.allPlayers = new ArrayList<>();
			allPlayers.addAll(detectives);
			allPlayers.add(mrX);


			if (setup.moves.isEmpty()){ throw new IllegalArgumentException("moves are empty");}
			if (setup.graph == null){ throw new IllegalArgumentException(("graph is null"));}
			if ((setup.graph.nodes()).size() == 0 ) { throw new IllegalArgumentException("graph is empty");}

			if (remaining == null){throw new IllegalArgumentException("remaining pieces null");}
			if (remaining.isEmpty()){throw new IllegalArgumentException("remaining pieces empty");}
			//if (log.isEmpty()){ throw new IllegalArgumentException(("log is empty"));} // weirdly passes 6 test
			for (LogEntry entry : log){
				if (entry == null) { throw new IllegalArgumentException("log entry is null");}
			}
			if (mrX.tickets().isEmpty()) {throw new IllegalArgumentException("tickets are empty");}
			if (mrX.piece() == null) {throw new IllegalArgumentException("piece is null");}

			for (Player player : detectives){
				List<Player> others = new ArrayList<Player>(detectives);
				others.remove(player);

				if (player.piece() == null) { throw	new IllegalArgumentException("player piece is null");}
				if (player.tickets().isEmpty()) { throw new IllegalArgumentException("player tickets are empty");}
				if (!(player.tickets().get(Ticket.DOUBLE) == 0)) { throw new IllegalArgumentException("detective has DOUBLE ticket");}
				if (!(player.tickets().get(Ticket.SECRET) == 0)) { throw new IllegalArgumentException("detective has SECRET ticket");}
				for (Player otherPlayers : others){
					if (player.location() == otherPlayers.location()){ throw new IllegalArgumentException("detective Locations overlap");}
					if (player.location() == mrX.location()) {throw new IllegalArgumentException("detective and mrx locations overlap");}
				}
			}
		}




		@Nonnull @Override //TODO
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override //TODO //DONE
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<>();

			for (Player piece : detectives) {
				players.add(piece.piece());
			}

			players.add(mrX.piece());
			return ImmutableSet.copyOf(players);
		}

		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
				for (Player player : detectives) {
					if (player.piece().webColour().equals(detective.webColour())) {
						return Optional.of(player.location());
					}
				}
			return Optional.empty();
		}

		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {


			for (Player player : allPlayers) {
				if (player.piece().webColour().equals(piece.webColour())) {
					return Optional.of(new TicketBoard() {
						@Override
						public int getCount(@Nonnull Ticket ticket) {
							return player.tickets().get(ticket);
						}
					});
				}
			}
			return Optional.empty();
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		@Nonnull @Override //from interface GameState //TODO
		public GameState advance(Move move) {
			return null;
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) { //, ImmutableSet<Piece> winner
		// TODO
		return new MyGameState(setup,
							   ImmutableSet.of(MrX.MRX), // remaining pieces
							   ImmutableList.of(), 		 // log
							   mrX,
							   detectives); //, ImmutableSet.of(winner)
	}




}

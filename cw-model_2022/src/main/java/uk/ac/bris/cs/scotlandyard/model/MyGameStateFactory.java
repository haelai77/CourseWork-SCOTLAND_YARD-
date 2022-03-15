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

			this.allPlayers = new ArrayList<>(detectives);
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

			for (Player player : allPlayers) {
				players.add(player.piece());
			}

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



		// HELPER METHOD: given a player, its location, and a set of all possible SingleMoves it can make, output a set of all possible DoubleMoves it can make.
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> singleMoves){
			// create an empty set to hold all possible DoubleMoves, given all SingleMoves possible.
			Set<DoubleMove> doubleMoves = new HashSet<>();

			//create a list of detective locations.
			List<Integer> detLocations = new ArrayList<>();
			for(Player det : detectives) {
				detLocations.add(det.location());
			}
			//for all possible single moves,
			for (SingleMove singleMove : singleMoves) {
				int source = singleMove.source();
				int destination = singleMove.destination;
				//go through all possible journeys from each single move's destination,
				for (int destination2 : setup.graph.adjacentNodes(destination)) {
					//  if the location is occupied, don't add to the collection of moves to return
					if (!detLocations.contains(destination2)) {
						//go through all the possible tickets between the single move destination and its next destination.
						for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination, destination2, ImmutableSet.of()))) {

							Ticket ticket2 = t.requiredTicket();
							//  if the tickets are the same for both moves, make sure the player has at least 2.
							// (if the player had less, it wouldn't be able to play this double move and use both tickets.)
							// if so, construct a DoubleMove and add it the collection of moves to return
							if ((singleMove.ticket == ticket2) && (player.hasAtLeast(ticket2, 2))) {
								doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, ticket2, destination2));
							}
							//  else if the player has the required tickets, and they are different, construct a DoubleMove and add it the collection of moves to return
							else if ((singleMove.ticket != ticket2) && player.has(ticket2)) {
								doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, ticket2, destination2));
							}
						}
						// do the same here but for Secret Tickets.
						if ((singleMove.ticket == Ticket.SECRET) && (player.hasAtLeast(Ticket.SECRET, 2))) {
							doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, Ticket.SECRET, destination2));
						}
						else if ((singleMove.ticket != Ticket.SECRET) && (player.has(Ticket.SECRET))) {
							doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, Ticket.SECRET, destination2));
						}
					}
				}
			}
			// return the collection of moves
			return doubleMoves;
		}

		//HELPER METHOD: given a player's current location, output a set of all possible SingleMoves the player can make.
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// create an empty set to hold all possible SingleMoves for a player.
			Set<SingleMove> singleMoves = new HashSet<>();

			// create a list of locations of the other detectives.
			List<Player> otherDet = new ArrayList<>(detectives);
			otherDet.remove(player);
			List<Integer> detLocations = new ArrayList<>();
			for(Player det : otherDet) {
				detLocations.add(det.location());
			}
			//go through all the possible journey's from the source (player location)
			for(int destination : setup.graph.adjacentNodes(source)) {
				// find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if (!detLocations.contains(destination)) {
					for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
						// find out if the player has the required tickets
						// if it does, construct a SingleMove and add it the collection of moves to return
						Ticket ticket = t.requiredTicket();
						if (player.has(ticket)) {
							singleMoves.add(new SingleMove(player.piece(), source, ticket, destination));
						}
					}
					// consider the rules of secret moves here
					// add moves to the destination via a secret ticket if there are any left with the player
					if (player.has(Ticket.SECRET)) {
						singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}
			//return the collection of moves
			return singleMoves;
		}


		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> playerMoves = new HashSet<>();
			for (Player player : allPlayers) {

				for (Piece piece : remaining) {
					if (player.piece().webColour().equals(piece.webColour())) {
						Set<SingleMove> playerSingleMoves = makeSingleMoves(setup, detectives, player, player.location());
						playerMoves.addAll(playerSingleMoves);

						if (player.piece().isMrX() && player.has(Ticket.DOUBLE)) {
								playerMoves.addAll(makeDoubleMoves(setup, detectives, player, playerSingleMoves));
						}
					}

				}
			}
			return ImmutableSet.copyOf(playerMoves);

		}




		@Nonnull @Override //from interface GameState //TODO
		public GameState advance(Move move) {
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			/*TODO:
			 If it's Mr X's turn (which can be checked using move.commencedBy):
				Add their move(s) to the log
				If a move should be revealed according to the GameSetup, reveal the destination in the log, otherwise keep the desination hidden
				Take the used ticket(s) away from Mr X
				Move Mr X's position to their new destination
				Swap to the detectives turn
			If it's the detectives' turn:
				Move the detective to their new destination
				Take the used ticket from the detective and give it to Mr X
				Ensure that particular detective won't move again this round (i.e. when getAvailableMoves() is called, it won't include any moves from that detective)
				If there are no more possible detective moves, swap to Mr X's turn

			 */
			if (move.commencedBy() == mrX.piece()) {
				Integer destination = move.accept(new Move.Visitor<>(){
					  @Override public Integer visit(SingleMove singleMove){

						  return 0;
					  }
					  @Override public Integer visit(DoubleMove doubleMove){
						  return 0;
					  }
				});
			}
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

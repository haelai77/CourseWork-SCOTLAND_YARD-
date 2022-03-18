package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.security.KeyStore;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import static java.util.stream.Collectors.toList;


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
		private List<Player> allPlayers;	  // Holds Mr.X and detectives

		//-------------------------------------------------------
		private ImmutableSet<Piece> remaining;// Pieces remaining
		private Set<Piece> winner;   // Current Winner(s) (was once immutable idk if this a good thing)
		//-------------------------------------------------------
		private ImmutableList<LogEntry> log;  // Mr. x's move log
		private ImmutableSet<Move> moves;	  // Holds the current possible moves TODO ASK ABOUT THIS IN THE LAB
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

			if (setup.moves.isEmpty()){
				throw new IllegalArgumentException("moves are empty");}
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
					//if (player.location() == mrX.location()) {throw new IllegalArgumentException("detective and mrx locations overlap");}
				}
			}
		}

		@Nonnull @Override //TODO
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override //TODO //DONE
		public ImmutableSet<Piece> getPlayers() { // returns all player pieces

			Set<Piece> players = allPlayers
					.stream()
					.map(Player::piece)
					.collect(Collectors.toSet());//new HashSet<>();

			return ImmutableSet.copyOf(players);
		}

		private Player getPlayerFromPiece(Piece piece) {
			return allPlayers
					.stream()
					.filter(player -> (player.piece() == piece))
					.findFirst()
					.orElse(null);
		}

		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			//				for (Player player : detectives) {
//					if (player.piece().webColour().equals(detective.webColour())) {
//						return Optional.of(player.location());
//					}
//		return Optional.empty();

			Player player = getPlayerFromPiece(detective);
			if (player == null) {return Optional.empty();}
			else {return Optional.of(player.location());}

		}

		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {


//			for (Player player : allPlayers) {
//				if (player.piece().webColour().equals(piece.webColour())) {
//					return Optional.of(new TicketBoard() {
//						@Override
//						public int getCount(@Nonnull Ticket ticket) {
//							return player.tickets().get(ticket);
//						}
//					});
//				}
//			}
//			return Optional.empty();
			Player player = getPlayerFromPiece(piece);
			if (player == null) return Optional.empty();
			else {
				return Optional.of(new TicketBoard() {
						@Override
						public int getCount(@Nonnull Ticket ticket) {
							return player.tickets().get(ticket);
						}
				});
			}
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {
			if (!winner.isEmpty()) {
				return ImmutableSet.copyOf(winner);
			}

			//if detectives win
			if (detectives.stream().map(Player::location).anyMatch( x -> x == mrX.location())//if any of the locations of detectives are the same as mister x
//					|| (makeSingleMoves(setup, detectives, mrX, mrX.location()).size() == 0)){	//if mr.X has no more space to go to
					|| (getAvailableMovesGeneric(ImmutableSet.of(mrX.piece())).isEmpty() && remaining.contains(mrX.piece())) // if mister x's turn, and he can't move
//					|| ((detectives.stream().map(Player::location).collect(Collectors.toSet()).containsAll(setup.graph.adjacentNodes(mrX.location())))) // if mrx adjacent nodes is a subset of detective locations (mrx surrounded)
//					|| (remaining.contains(mrX.piece()) && getAvailableMoves().isEmpty())
			){
				winner = detectives.stream().map(Player::piece).collect(Collectors.toSet());
				System.out.println("det win :" + remaining);
				return ImmutableSet.copyOf(detectives.stream().map(Player::piece).collect(Collectors.toSet())); //return set of detective pieces
			}
			//if mrx wins
			else if (getAvailableMovesGeneric(detectives.stream().map(Player::piece).collect(Collectors.toSet())).isEmpty() //if at anypoint all the detectives can't move
//				|| (detectives.stream().map(detective -> detective.tickets().values().stream().mapToInt(Integer::intValue).sum()).reduce(0, Integer::sum) == 0)
			)
			{
				System.out.println("x Win :" + remaining);
				winner = Set.of(mrX.piece());
				return ImmutableSet.of(mrX.piece());
			}

			System.out.println("no weinners");
			System.out.println(remaining);
			System.out.println(getAvailableMoves());
			return ImmutableSet.of();
		}

		// HELPER METHOD: given a player, its location, and a set of all possible SingleMoves it can make, output a set of all possible DoubleMoves it can make.
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> singleMoves){
			// create an empty set to hold all possible DoubleMoves, given all SingleMoves possible.
			Set<DoubleMove> doubleMoves = new HashSet<>();

			//create a list of detective locations.
			List<Integer> otherDetLocations = detectives.stream().map(Player::location).toList();

			//for all possible single moves,
			for (SingleMove singleMove : singleMoves) {
				int source = singleMove.source();
				int destination = singleMove.destination;
				//go through all possible journeys from each single move's destination,
				for (int destination2 : setup.graph.adjacentNodes(destination)) {
					//  if the location is occupied, don't add to the collection of moves to return
					if (!otherDetLocations.contains(destination2)) {
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
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

			// create an empty set to hold all possible SingleMoves for a player.
			Set<SingleMove> singleMoves = new HashSet<>();

			// create a list of locations of the other detectives.
			List<Player> otherDets = new ArrayList<>(detectives);
			otherDets.remove(player);

			List<Integer> otherDetLocations = new ArrayList<>();
			otherDets.forEach(x -> otherDetLocations.add(x.location())); //finds all the locations of the other players

			//go through all the possible journeys (single moves) from the source (player location)
			for(int destination : setup.graph.adjacentNodes(source)) {
				// find out if destination is occupied by a detective
				// if the location is occupied, don't add to the collection of moves to return
				if (!otherDetLocations.contains(destination)) {
					for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {// << big thing is vehicles (edge values)
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
			return singleMoves;

//			setup.graph.adjacentNodes(source) // <<<the destinations
//					.stream()
//					.filter(destination -> otherDetLocations.stream()    //<< detective locations
//							.anyMatch(detLocation -> !detLocation.equals(destination)))    //<< keeps all destinations that aren't detective locations.
//						.forEach(
//							destination ->
//							{
//							setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())// << for each destination filter out where tickets don't exist
//							.stream()
//							.filter(ticket -> player.has(ticket.requiredTicket()))
//							.forEach(ticket -> singleMoves.add(new SingleMove(player.piece(), source, ticket.requiredTicket(), destination)));
//
//							if (player.has(Ticket.SECRET)) {
//							singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
//							}
//
//							});
//			return singleMoves;
		}

		@Nonnull
		public ImmutableSet<Move> getAvailableMovesGeneric(Set<Piece> pieces){
			if (!winner.isEmpty()) {
				return ImmutableSet.of();
			}
			Set<Move> playerMoves = new HashSet<>();


			for (Piece piece : pieces) { // for each remaining pieces left to move
				Player player = getPlayerFromPiece(piece);
				Set<SingleMove> playerSingleMoves = makeSingleMoves(setup, detectives, player, player.location());
				playerMoves.addAll(playerSingleMoves);

				if (player.piece().isMrX() && player.has(Ticket.DOUBLE) && (setup.moves.size() > 1)) {
					playerMoves.addAll(makeDoubleMoves(setup, detectives, player, playerSingleMoves));
				}

			}

			return ImmutableSet.copyOf(playerMoves);

		}

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves(){
			return getAvailableMovesGeneric(remaining);
		}

		@Nonnull @Override //from interface GameState //TODO
		public GameState advance(Move move) {
//			getAvailableMoves().forEach(System.out::println);
//			System.out.println("------------------");				i
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			/*TODO:
			 If it's Mr X's turn (which can be checked using move.commencedBy):
				Add their move(s) to the log
				If a move should be revealed according to the GameSetup, reveal the destination in the log, otherwise keep the destination hidden
				Take the used ticket(s) away from Mr X
				Move Mr X's position to their new destination
				Swap to the detectives turn
			If it's the detectives' turn:
				Move the detective to their new destination
				Take the used ticket from the detective and give it to Mr X
				Ensure that particular detective won't move again this round (i.e. when getAvailableMoves() is called, it won't include any moves from that detective)
				If there are no more possible detective moves, swap to Mr X's turn

			 */
			GameState result = null;
				result = move.accept(new Visitor<>() {
					@Override
					public GameState visit(SingleMove singleMove) {
						/*
						if mrX is moving:
							add move to log (either hidden or reveal)
							update remaining with all detectives.
							take away tickets used
						*/
						if (move.commencedBy().equals(mrX.piece())){

							List<Boolean> newMoves = new ArrayList<>(List.copyOf(setup.moves)); // copy of setup.moves
							List<LogEntry> newLog =  new ArrayList<>(List.copyOf(log));	//copy of the old log

							Set<Piece> detectivePieces = detectives.stream().map(Player::piece).collect(Collectors.toSet());

							//handles log updating for mrx single move
							LogEntry newEntry;

							if (setup.moves.get(0)){ newEntry = LogEntry.reveal(singleMove.ticket, singleMove.destination);} // if  reveal move add revealed location to log
							else {newEntry = LogEntry.hidden(singleMove.ticket);}	// if on hidden move add hidden entry to log

							newLog.add(newEntry);
							//System.out.println("log updated for mrX single Move");
							//handles updating newSetup.move
							//------------------------------------TESTING

							if (newMoves.size() > 1) {
								newMoves.remove(0);//WARNING WARNING THIS DOESN'T GET REMOVED ON THE LAST MOVE !!!!!! BAD BAD ED SMH
							}

							//------------------------------------
							//handles newSetup initialization
							GameSetup newSetup = new GameSetup(setup.graph , ImmutableList.copyOf(newMoves));

							return new MyGameState(newSetup,
									ImmutableSet.copyOf(detectivePieces),
									ImmutableList.copyOf(newLog),
									mrX.use(singleMove.ticket).at(singleMove.destination),
									detectives);
						}
						/*
						if detective is moving:
							remove this player from remaining.
							give mr X this ticket.
						*/
						else{//if (!move.commencedBy().equals(mrX.piece()))

							List<Player> newDetectives = new ArrayList<>(List.copyOf(detectives));

							newDetectives.remove(getPlayerFromPiece(move.commencedBy()));
							Player newDetective = getPlayerFromPiece(singleMove.commencedBy()).at(singleMove.destination).use(singleMove.ticket);
							newDetectives.add(newDetective);


							Set<Piece> newRemaining = new HashSet<>(Set.copyOf(remaining));

								newRemaining.remove(singleMove.commencedBy());

								newRemaining.removeAll(remaining.stream().filter(piece -> getAvailableMovesGeneric(ImmutableSet.of(piece)).isEmpty()).collect(Collectors.toSet()));

								if (newRemaining.isEmpty()) {
									newRemaining.add(mrX.piece());
								}

//							Player
							return new MyGameState(setup,
													ImmutableSet.copyOf(newRemaining),
													log,
													mrX.give(singleMove.ticket),
													newDetectives);
						}

					}

					@Override
					public GameState visit(DoubleMove doubleMove) {
						/*
						add log entry of first destination
						add log entry of second destination
						take away the tickets from mr X.
						update remaining with all detectives.
						 */
						List<Boolean> newMoves = new ArrayList<>(List.copyOf(setup.moves));
						List<LogEntry> newLog = new ArrayList<>(List.copyOf(log));

						if (newMoves.get(0)) {
							newLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
						}
						else {newLog.add(LogEntry.hidden(doubleMove.ticket1));}
						if (newMoves.size() > 1) {
							newMoves.remove(0);
						}

						if (newMoves.get(0)) {
							newLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
						}
						else {newLog.add(LogEntry.hidden(doubleMove.ticket2));}
						if (newMoves.size() > 1) {
							newMoves.remove(0);
						}


						Set<Piece> detectivePieces = detectives.stream().map(Player::piece).collect(Collectors.toSet());
						//Set<Piece> detectivePieces = new HashSet<>(Set.copyOf(detectives));


						return new MyGameState(new GameSetup(
								setup.graph, ImmutableList.copyOf(newMoves)), //setup
								ImmutableSet.copyOf(detectivePieces), //remaining
								ImmutableList.copyOf(newLog), //log
								mrX
										.use(ImmutableList.of(doubleMove.ticket1, doubleMove.ticket2, Ticket.DOUBLE))
										.at(doubleMove.destination2), //mrX
								detectives); // detectives

					}
				});

			return result;
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) { //, ImmutableSet<Piece> winner
		// TODO
		return new MyGameState(setup,
							   ImmutableSet.of(MrX.MRX), // MRX at the start (remaining pieces)
							   ImmutableList.of(), 		 // log
							   mrX,
							   detectives); // detectives
	}




}

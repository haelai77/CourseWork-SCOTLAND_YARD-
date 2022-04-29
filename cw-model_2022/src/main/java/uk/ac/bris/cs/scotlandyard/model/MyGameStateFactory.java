package uk.ac.bris.cs.scotlandyard.model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
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
		//ATTRIBUTES
		private final GameSetup setup;
		//------------------------------------------------------- COLLECTIONS AND MRX ATTRIBUTE

		private final Player mrX;		 			  // The Player of mrX
		private final List<Player> detectives;        // Detectives Players
		private final List<Player> allPlayers;		  // list of detectives and mrX

		//------------------------------------------------------- IMMUTABLES

		private ImmutableSet<Piece> winner;   		 	   // The winner of the GameState, evaluated in constructor
		private final ImmutableList<LogEntry> log;   	   // List of Mr X Log Entries
		private final ImmutableSet<Piece> remaining; 	   // Pieces remaining that have yet to move
		private final ImmutableSet<Piece> detectivePieces; // Detective Pieces

		//-------------------------------------------------------------------------------------------------------------- CHECKS FOR NULL OR EMPTINESS

		private static <T> void verifyNotNullOrEmpty(T element, String message){ //function for checking whether initialised fields are null or empty
			if (element == null){					 	// throws exception if argument is null
				throw new IllegalArgumentException(String.format("%s", message));
			}
			else if (element instanceof Integer){	//checks if graph is empty
				if (message.contains("graph") && ((Integer)element == 0)){throw new IllegalArgumentException(String.format("%s", message));}
			}
			else if (element instanceof Collection){	// throws exception if collection is empty
				if (((Collection<?>) element).isEmpty()){throw new IllegalArgumentException(String.format("%s", message));}
			}
		}
		//--------------------------------------------------------------------------------------------------------------
		//CONSTRUCTOR FOR MyGameState
		private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives
		)
		{	//INITIALISE FIELDS
			this.setup = setup;
			this.remaining = remaining;
			this.detectives = detectives;
			this.log = log;
			this.mrX = mrX;
			this.allPlayers = Stream.of(detectives, List.of(mrX))
								.flatMap(Collection::stream)
								.collect(toList());  // stream to merge a list of detectives and mrX into allPlayers

			this.detectivePieces = detectives
									.stream()
									.map(Player::piece)
									.collect(ImmutableSet.toImmutableSet()); // maps detectives to their pieces

			this.winner = ImmutableSet.of();

			//------------------------------------------------------ CHECKS FOR WINNERS;

			//-----------------CHECKS IF DETECTIVES WIN
			if (detectives.stream().map(Player::location).anyMatch( x -> x == mrX.location())//if any of the locations of detectives are the same as mister x
					|| (getAvailableMovesGeneric(ImmutableSet.of(mrX.piece())).isEmpty() && remaining.contains(mrX.piece()))) //if mister x's turn, and he can't move (surrounded or incorrect tickets)
			{this.winner = detectivePieces;}
			//-----------------CHECKS IF MRX WINS
			else if (getAvailableMovesGeneric(detectives.stream().map(Player::piece).collect(Collectors.toSet())).isEmpty()//if at anypoint all the detectives can't move
					|| ((setup.moves.size() == log.size() && remaining.contains(mrX.piece())))) //if the log is full
			{this.winner = ImmutableSet.of(mrX.piece());}

			//------------------------------------------------------- DO CHECKS FOR FIELDS ETC.
			verifyNotNullOrEmpty(setup, "setup is null");
			verifyNotNullOrEmpty(setup.graph, "graph is null");
			verifyNotNullOrEmpty(remaining,"remaining pieces null");

			verifyNotNullOrEmpty(setup.moves,"setup moves are empty");
			verifyNotNullOrEmpty(setup.graph.nodes().size(), "graph is empty");
			verifyNotNullOrEmpty(remaining,"remaining pieces empty");

			log.forEach(logEntry -> verifyNotNullOrEmpty(logEntry, "logEntry is null"));

			//for each player,
			detectives.forEach(player -> {
				verifyNotNullOrEmpty(player.piece(), String.format("%s piece is null", player.piece().webColour())); // check that the piece isn't null,
				verifyNotNullOrEmpty(player.tickets(), String.format("%s tickets are empty", player.piece().webColour())); //tickets aren't empty,
				if ((player.tickets().get(Ticket.DOUBLE) != 0)) { throw new IllegalArgumentException("detective has DOUBLE ticket");} //detectives don't have special tickets,
				if ((player.tickets().get(Ticket.SECRET) != 0)) { throw new IllegalArgumentException("detective has SECRET ticket");}
				otherDetectives(player).forEach(player1 -> {
										if (player.location() == player1.location())
										{throw new IllegalArgumentException("detective Locations overlap");}}); //and detective locations don't overlap
			});
		}

		@Nonnull @Override
		public GameSetup getSetup() {return setup;}

		@Nonnull @Override
		public ImmutableSet<Piece> getPlayers() { // returns all player pieces
			return  allPlayers
					.stream()
					.map(Player::piece)
					.collect(ImmutableSet.toImmutableSet());
		}

		private Player getPlayerFromPiece(Piece piece) { //Gets the Player which owns the piece
			return allPlayers
					.stream()
					.filter(player -> (player.piece() == piece))
					.findFirst()
					.orElse(null);
		}

		private ImmutableList<Player> otherDetectives(Player player) { //returns all other detectives that aren't the player
			if (player.isMrX()) {return ImmutableList.copyOf(detectives);}
			else {	//returns immutableList of detectives with the player filtered out
				return ImmutableList.copyOf(detectives.stream().filter(otherDetective -> otherDetective != player).collect(Collectors.toList()));
			}
		}

		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) { //gets detective location
			Player player = getPlayerFromPiece(detective);
			if (player == null) {return Optional.empty();}
			else {return Optional.of(player.location());}
		}

		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			Player player = getPlayerFromPiece(piece);
			if (player == null) return Optional.empty();
			else {
				return Optional.of(ticket -> player.tickets().get(ticket)); // returns optional of implementation of getCount(...) in the TicketBoard interface
			}
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {return log;}

		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {return winner;}

		// HELPER METHOD: given a player, its location, and a set of all possible DoubleMoves it can make, output a set of all possible DoubleMoves it can make.
		private Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> singleMoves){
			Set<DoubleMove> doubleMoves = new HashSet<>();
			List<Integer> otherDetLocations = detectives.stream().map(Player::location).toList();

			singleMoves.forEach(singleMove ->
					setup.graph.adjacentNodes(singleMove.destination).stream()	// adjacent nodes of destination
							.filter(destination2 -> !otherDetLocations.contains(destination2)) //filters out any that are detective locations
							.forEach(destination2 ->
								{Objects.requireNonNull(setup.graph.edgeValueOrDefault(singleMove.destination, destination2, ImmutableSet.of())).forEach(ticket2 -> {
										//if the tickets are the same and the player has 2 of them
										if ((singleMove.ticket == ticket2.requiredTicket()) && (player.hasAtLeast(ticket2.requiredTicket(), 2))) {
											doubleMoves.add(new DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, ticket2.requiredTicket(), destination2));}
										//if the tickets aren't the same and the player has 2 different tickets
										else if ((singleMove.ticket != ticket2.requiredTicket()) && player.has(ticket2.requiredTicket())) {
											doubleMoves.add(new DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, ticket2.requiredTicket(), destination2));}
									}); // Same as above but for secret tickets
									if ((singleMove.ticket == Ticket.SECRET) && (player.hasAtLeast(Ticket.SECRET, 2))) {
										doubleMoves.add(new DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, Ticket.SECRET, destination2));}
									else if ((singleMove.ticket != Ticket.SECRET) && (player.has(Ticket.SECRET))) {
										doubleMoves.add(new DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, Ticket.SECRET, destination2));}
								})
			);
			return doubleMoves;
		}

		//HELPER METHOD: given a player's current location, output a set of all possible SingleMoves the player can make.
		private Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<SingleMove> singleMoves = new HashSet<>();
			List<Integer> otherDetLocations = detectives.stream().filter(detective -> !detective.equals(player)).map(Player::location).toList(); // stream for other detective locations

			setup.graph.adjacentNodes(source).stream() // the destinations (adjacent nodes to the player)
					.filter(destination -> !otherDetLocations.contains(destination))	// filters out destinations that are other detective locations
						.forEach(destination ->
							{Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())).stream()    // goes through available single move routs
							.filter(ticket -> player.has(ticket.requiredTicket()))	// filter out the routes that the player doesn't have a ticket for
							.forEach(ticket -> singleMoves.add(new SingleMove(player.piece(), source, ticket.requiredTicket(), destination)));	// add the remaining possibilities to singleMoves

							if (player.has(Ticket.SECRET)) {singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));}	// add secret move possibility if available
							});
			return singleMoves;
		}

		//given a set of pieces, calculates the available moves for all of them as a set
		//used in getAvailableMoves and calculating the winner
		@Nonnull
		public ImmutableSet<Move> getAvailableMovesGeneric(Set<Piece> pieces){
			if (!winner.isEmpty()) {
				return ImmutableSet.of();
			}

			Set<Move> playerMoves = new HashSet<>();
			pieces.forEach(piece -> {
				Player player = getPlayerFromPiece(piece);
				Set<SingleMove> playerSingleMoves = makeSingleMoves(setup, detectives, player, player.location());
				playerMoves.addAll(playerSingleMoves);

				if (player.piece().isMrX() && player.has(Ticket.DOUBLE) && (setup.moves.size() > 1)) {
					playerMoves.addAll(makeDoubleMoves(setup, detectives, player, playerSingleMoves));
				}
			});

			return ImmutableSet.copyOf(playerMoves);

		}

		//Use the generic method with the same name to calculate the available moves for remaining
		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves(){
			return getAvailableMovesGeneric(remaining);
		}

		@Nonnull @Override
		public GameState advance(Move move) {
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
				return move.accept(new Visitor<>() { //anonymous visitor class that returns a new GameState after a move has been made
					@Override
					public GameState visit(SingleMove singleMove) { //if the move is a single move
						if (move.commencedBy().equals(mrX.piece())){  //if mr x is moving

							List<LogEntry> newLog =  new ArrayList<>(List.copyOf(log));	//copy of the old log
							//handles log updating for mrx single move
							LogEntry newEntry;
							if (setup.moves.get(log.size())) {
								newEntry = LogEntry.reveal(singleMove.ticket, singleMove.destination);
							}
							else {newEntry = LogEntry.hidden(singleMove.ticket);}	// if on hidden move add hidden entry to log
							newLog.add(newEntry);

							return new MyGameState(
									setup,
									detectivePieces,
									ImmutableList.copyOf(newLog),
									mrX.use(singleMove.ticket).at(singleMove.destination),
									detectives);
						}

						else{// otherwise, a player is moving

							List<Player> newDetectives = new ArrayList<>(List.copyOf(detectives));

							newDetectives.remove(getPlayerFromPiece(singleMove.commencedBy()));
							Player newDetective = getPlayerFromPiece(singleMove.commencedBy()).at(singleMove.destination).use(singleMove.ticket);
							newDetectives.add(newDetective);// replace the old detectives with a new one at the destination with a ticket used up


							Set<Piece> newRemaining = new HashSet<>(Set.copyOf(remaining)); //remove the player from remaining
								newRemaining.remove(singleMove.commencedBy());
								newRemaining.removeAll(remaining.stream().filter(piece -> getAvailableMovesGeneric(ImmutableSet.of(piece)).isEmpty()).collect(Collectors.toSet()));

							if (newRemaining.isEmpty()) { //if the last player moved then add Mr X to remaining
								newRemaining.add(mrX.piece());
							}

							return new MyGameState(
									setup,
									ImmutableSet.copyOf(newRemaining),
									log,
									mrX.give(singleMove.ticket),
									newDetectives
							);
						}

					}

					@Override
					public GameState visit(DoubleMove doubleMove) { //if a double move was made, then make a new Mr X at after this move
						List<LogEntry> newLog = new ArrayList<>(List.copyOf(log));

						//add the two single moves to the log entries
						LogEntry newEntry;

						if (setup.moves.get(log.size())) {newEntry = LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1);}
						else {newEntry = LogEntry.hidden(doubleMove.ticket1);}
						newLog.add(newEntry);

						if (setup.moves.get(newLog.size())) {newEntry = LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2);}
						else {newEntry = LogEntry.hidden(doubleMove.ticket2);}
						newLog.add(newEntry);

						return new MyGameState(
								setup, //setup
								detectivePieces, //remaining
								ImmutableList.copyOf(newLog), //log
								mrX.use(ImmutableList.of(doubleMove.ticket1, doubleMove.ticket2, Ticket.DOUBLE)).at(doubleMove.destination2), //mrX
								detectives); // detectives

					}
				});


		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) { //, ImmutableSet<Piece> winner
		return new MyGameState(setup,
				ImmutableSet.of(MrX.MRX), // MRX at the start (remaining pieces)
				ImmutableList.of(),         // log
				mrX,
				detectives); // detectives
	}
}

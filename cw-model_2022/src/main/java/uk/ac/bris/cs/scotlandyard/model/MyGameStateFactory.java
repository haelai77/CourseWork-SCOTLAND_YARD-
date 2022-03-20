package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.IllegalChannelGroupException;
import java.security.KeyStore;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

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
		//ATTRIBUTES
		private final GameSetup setup;
		//------------------------------------------------------- COLLECTIONS AND MRX ATTRIBUTE

		private final Player mrX;		 			  // The Player of mrX
		private final List<Player> detectives;        // Detectives Players
		private final List<Player> allPlayers;		  // list of detectives and mrX

		//------------------------------------------------------- IMMUTABLES

		private ImmutableSet<Piece> winner;   		 	   //
		private final ImmutableList<LogEntry> log;   	   // Log of Mr.x moves
		private final ImmutableSet<Piece> remaining; 	   // Pieces remaining to move for a round
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

			log.forEach(logEntry -> {verifyNotNullOrEmpty(logEntry, "logEntry is null");});

			detectives.forEach(player -> {
				verifyNotNullOrEmpty(player.piece(), String.format("%s piece is null", player.piece().webColour()));
				verifyNotNullOrEmpty(player.tickets(), String.format("%s tickets are empty", player.piece().webColour()));
				if ((player.tickets().get(Ticket.DOUBLE) != 0)) { throw new IllegalArgumentException("detective has DOUBLE ticket");}	//player checks for double and secret tickets
				if ((player.tickets().get(Ticket.SECRET) != 0)) { throw new IllegalArgumentException("detective has SECRET ticket");}
				otherDetectives(player).forEach(player1 -> {
										if (player.location() == player1.location())
										{throw new IllegalArgumentException("detective Locations overlap");}});
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
//		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> singleMoves){
//			// create an empty set to hold all possible DoubleMoves, given all SingleMoves possible.
//			Set<DoubleMove> doubleMoves = new HashSet<>();
//
//			//create a list of detective locations.
//			List<Integer> otherDetLocations = detectives.stream().map(Player::location).toList();
//
//			//for all possible single moves,
//			for (SingleMove singleMove : singleMoves) {
//				int source = singleMove.source();
//				int destination = singleMove.destination;
//				//go through all possible journeys from each single move's destination,
//				for (int destination2 : setup.graph.adjacentNodes(destination)) {
//					//  if the location is occupied, don't add to the collection of moves to return
//					if (!otherDetLocations.contains(destination2)) {
//						//go through all the possible tickets between the single move destination and its next destination.
//						for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination, destination2, ImmutableSet.of()))) {
//
//							Ticket ticket2 = t.requiredTicket();
//							//  if the tickets are the same for both moves, make sure the player has at least 2.
//							// (if the player had less, it wouldn't be able to play this double move and use both tickets.)
//							// if so, construct a DoubleMove and add it the collection of moves to return
//							if ((singleMove.ticket == ticket2) && (player.hasAtLeast(ticket2, 2))) {
//								doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, ticket2, destination2));
//							}
//							//  else if the player has the required tickets, and they are different, construct a DoubleMove and add it the collection of moves to return
//							else if ((singleMove.ticket != ticket2) && player.has(ticket2)) {
//								doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, ticket2, destination2));
//							}
//						}
//						// do the same here but for Secret Tickets.
//						if ((singleMove.ticket == Ticket.SECRET) && (player.hasAtLeast(Ticket.SECRET, 2))) {
//							doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, Ticket.SECRET, destination2));
//						}
//						else if ((singleMove.ticket != Ticket.SECRET) && (player.has(Ticket.SECRET))) {
//							doubleMoves.add(new DoubleMove(player.piece(), source, singleMove.ticket, destination, Ticket.SECRET, destination2));
//						}
//					}
//				}
//			}
//			// return the collection of moves
//			return doubleMoves;
//		}

		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, Set<SingleMove> singleMoves){
			Set<DoubleMove> doubleMoves = new HashSet<>();
			List<Integer> otherDetLocations = detectives.stream().map(Player::location).toList();

			singleMoves.forEach(singleMove ->
						{setup.graph.adjacentNodes(singleMove.destination).stream()	// adjacent nodes of destination
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
											doubleMoves.add(new DoubleMove(player.piece(), singleMove.source(), singleMove.ticket, singleMove.destination, Ticket.SECRET, destination2));};
									});
						}
			);
			return doubleMoves;
		}

		//HELPER METHOD: given a player's current location, output a set of all possible SingleMoves the player can make.
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
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

		@Nonnull
		public ImmutableSet<Move> getAvailableMovesGeneric(Set<Piece> pieces){
			if (!winner.isEmpty()) {
				return ImmutableSet.of();
			}

//			for (Piece piece : pieces) { // for each remaining pieces left to move
//				Player player = getPlayerFromPiece(piece);
//				Set<SingleMove> playerSingleMoves = makeSingleMoves(setup, detectives, player, player.location());
//				playerMoves.addAll(playerSingleMoves);
//
//				if (player.piece().isMrX() && player.has(Ticket.DOUBLE) && (setup.moves.size() > 1)) {
//					playerMoves.addAll(makeDoubleMoves(setup, detectives, player, playerSingleMoves));
//				}
//
//			}
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

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves(){
			return getAvailableMovesGeneric(remaining);
		}

		@Nonnull @Override //from interface GameState //TODO
		public GameState advance(Move move) {
//			getAvailableMoves().forEach(System.out::println);
//			System.out.println("------------------");				i		sout

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
				return move.accept(new Visitor<>() {
					@Override
					public GameState visit(SingleMove singleMove) {
						/*
						if mrX is moving:
							add move to log (either hidden or reveal)
							update remaining with all detectives.
							take away tickets used
						*/
						if (move.commencedBy().equals(mrX.piece())){

							List<LogEntry> newLog =  new ArrayList<>(List.copyOf(log));	//copy of the old log
							//Set<Piece> detectivePieces = detectives.stream().map(Player::piece).collect(Collectors.toSet());

							//handles log updating for mrx single move
							LogEntry newEntry;
							if (setup.moves.get(log.size())) {
								newEntry = LogEntry.reveal(singleMove.ticket, singleMove.destination);
							}
							else {newEntry = LogEntry.hidden(singleMove.ticket);}	// if on hidden move add hidden entry to log

							newLog.add(newEntry);


							//System.out.println("log updated for mrX single Move");
							//handles updating newSetup.move
							//------------------------------------TESTING

//							if (newMoves.size() > 1) {
//								newMoves.remove(0);//WARNING WARNING THIS DOESN'T GET REMOVED ON THE LAST MOVE !!!!!! BAD BAD ED SMH
//							}

							//------------------------------------
							//handles newSetup initialization

							return new MyGameState(
								setup,
								detectivePieces,
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

							newDetectives.remove(getPlayerFromPiece(singleMove.commencedBy()));
							Player newDetective = getPlayerFromPiece(singleMove.commencedBy()).at(singleMove.destination).use(singleMove.ticket);
							newDetectives.add(newDetective);


							Set<Piece> newRemaining = new HashSet<>(Set.copyOf(remaining));
								newRemaining.remove(singleMove.commencedBy());
								newRemaining.removeAll(remaining.stream().filter(piece -> getAvailableMovesGeneric(ImmutableSet.of(piece)).isEmpty()).collect(Collectors.toSet()));

							if (newRemaining.isEmpty()) {
								newRemaining.add(mrX.piece());
							}

//							Player
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
					public GameState visit(DoubleMove doubleMove) {
						/*
						add log entry of first destination
						add log entry of second destination
						take away the tickets from mr X.
						update remaining with all detectives.
						 */
						List<LogEntry> newLog = new ArrayList<>(List.copyOf(log));

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
		// TODO
		return new MyGameState(setup,
							   ImmutableSet.of(MrX.MRX), // MRX at the start (remaining pieces)
							   ImmutableList.of(), 		 // log
							   mrX,
							   detectives); // detectives
	}
}

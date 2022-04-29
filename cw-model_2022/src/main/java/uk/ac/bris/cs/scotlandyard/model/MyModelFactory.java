package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	@Nonnull @Override public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		// TODO
	return new Model()
	{
		//instantiate a set of observer objects
		final HashSet<Observer> observers = new HashSet<>();
		Board.GameState state = new MyGameStateFactory().build(setup, mrX, detectives);


		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		//adds an observer to the set of observers
		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) {
				throw new IllegalArgumentException("the observer is null");
			}
			if (observers.contains(observer)) {
				throw new IllegalArgumentException("registered observer can't be registered");
			}
			observers.add(observer);
		}

		//removes an observer from the set of observers
		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) {
				throw new IllegalArgumentException("the observer is null");
			}
			if (!observers.contains(observer)) {
				throw new IllegalArgumentException("can't unregister unregistered observer");
			}
			observers.remove(observer);
		}

		//return all observers
		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		//advance the model with a move and update the observers
		@Override
		public void chooseMove(@Nonnull Move move){
			state = state.advance(move);
			if (state.getWinner().isEmpty()) {
				observers.forEach(observer -> observer.onModelChanged(state, Observer.Event.MOVE_MADE));
			}
			else {
				observers.forEach(observer -> observer.onModelChanged(state, Observer.Event.GAME_OVER));
			}
		}
	};
	}
}

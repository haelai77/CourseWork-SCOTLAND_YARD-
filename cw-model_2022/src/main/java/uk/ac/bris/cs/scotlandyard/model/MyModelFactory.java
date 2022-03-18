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
		HashSet<Observer> observers = new HashSet<>();
		Board.GameState state = new MyGameStateFactory().build(setup, mrX, detectives);


		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) {
				throw new IllegalArgumentException("the observer is null");
			}
			if (observers.contains(observer)) {
				throw new IllegalArgumentException("observer can't be added twice");
			}
			observers.add(observer);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) {
				throw new IllegalArgumentException("the observer is null");
			}
			if (!observers.contains(observer)) {
				throw new IllegalArgumentException("can't unregister observer not in observers");
			}
			observers.remove(observer);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move){
			state = state.advance(move);
			if (state.getWinner().isEmpty()) {
				for (Observer observer : observers) {
					observer.onModelChanged(state, Observer.Event.MOVE_MADE);
				}
				System.out.println("winners:" + state.getWinner());
			}
			else {

				for (Observer observer : observers) {
					observer.onModelChanged(state, Observer.Event.GAME_OVER);
				}
				System.out.println("winners:" + state.getWinner());

			}
		}
	};
	}
}

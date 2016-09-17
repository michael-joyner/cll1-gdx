package com.cherokeelessons.cll2ev1;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public abstract class AbstractGame extends Game {
	protected final String TAG = this.getClass().getSimpleName();
	protected void log(String message) {
		Gdx.app.log(TAG, message);
	}
	protected List<Screen> screens = new ArrayList<Screen>();

	/**
	 * Replaces the top of the "deck" of screens with this screen.<br/>
	 * Current screen (if any) {@link Screen#hide()} is called.<br/>
	 * New screen {@link Screen#show()} is called.<br/>
	 * Previous screen (if any) {@link Screen#dispose()} is called.
	 */
	@Override
	public void setScreen(Screen screen) {
		int activeScreen = screens.size() - 1;
		screens.add(screen);
		super.setScreen(screen);
		if (activeScreen > -1) {
			final Screen forDisposal = screens.remove(activeScreen);
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					forDisposal.dispose();
				}
			});
		}
	}

	/**
	 * Adds this screen to the top of the "deck" of screens.<br/>
	 * Replaces current screen (if any) with a new screen.<br/>
	 * Current screen (if any) {@link Screen#hide()} is called.<br/>
	 * New screen {@link Screen#show()} is called.<br/>
	 */
	public void addScreen(Screen screen) {
		screens.add(screen);
		super.setScreen(screen);
	}
	
	@Override
	public void create() {
	}
	
	@Override
	public void dispose() {
		log("Dispose");
		super.dispose();
		for (Screen screen: screens) {
			screen.dispose();
		}
	}
}
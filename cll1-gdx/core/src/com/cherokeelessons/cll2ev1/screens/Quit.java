package com.cherokeelessons.cll2ev1.screens;

import com.badlogic.gdx.Gdx;
import com.cherokeelessons.cll2ev1.AbstractGame;
import com.cherokeelessons.cll2ev1.CLL1;

public class Quit extends AbstractScreen {
	public Quit(AbstractGame game) {
		super(game);
		setBackdrop(CLL1.BACKDROP);
	}

	@Override
	public void show() {
		super.show();
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				Gdx.app.exit();
			}
		});
	}

	@Override
	protected boolean onBack() {
		return false;
	}

	@Override
	protected boolean onMenu() {
		return false;
	}

	@Override
	protected void act(float delta) {
		// TODO Auto-generated method stub

	}

}

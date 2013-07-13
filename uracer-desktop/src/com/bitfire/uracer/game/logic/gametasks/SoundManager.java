
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.utils.ItemsManager;

public class SoundManager extends GameTask implements DisposableTasks {
	private final ItemsManager<SoundEffect> manager = new ItemsManager<SoundEffect>();

	public SoundManager () {
	}

	@Override
	public void dispose () {
		super.dispose();
		disposeTasks();
	}

	@Override
	public void disposeTasks () {
		manager.dispose();
	}

	public void add (SoundEffect effect) {
		manager.add(effect);
	}

	public void remove (SoundEffect effect) {
		effect.stop();
		manager.remove(effect);
	}

	@Override
	public void onGamePause () {
		for (SoundEffect s : manager) {
			s.gamePause();
		}
	}

	@Override
	public void onGameResume () {
		for (SoundEffect s : manager) {
			s.gameResume();
		}
	}

	@Override
	protected void onTick () {
		for (SoundEffect s : manager) {
			s.tick();
		}
	}

	@Override
	public void onGameRestart () {
		for (SoundEffect s : manager) {
			s.gameRestart();
		}
	}

	@Override
	public void onGameReset () {
		for (SoundEffect s : manager) {
			s.gameReset();
		}
	}

	@Override
	public void onPlayer (PlayerCar player) {
		super.onPlayer(player);
		for (SoundEffect s : manager) {
			s.player(player);
		}
	}

	@Override
	public void onGameQuit () {
		Gdx.app.log("SoundManager", "Stopping sound manager.");
		stop();
	}

	private void stop () {
		for (SoundEffect s : manager) {
			s.stop();
		}
	}
}

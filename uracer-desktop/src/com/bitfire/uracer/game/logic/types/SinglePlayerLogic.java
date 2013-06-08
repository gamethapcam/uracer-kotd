
package com.bitfire.uracer.game.logic.types;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.Messager;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.InterpolatedFloat;

public class SinglePlayerLogic extends CommonLogic {

	private Messager messager;

	public SinglePlayerLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
		messager = gameTasksManager.messager;
	}

	@Override
	public void dispose () {
		super.dispose();
	}

	private float prevZoom = GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow;
	// private InterpolatedFloat speed = new InterpolatedFloat()
	private InterpolatedFloat drift = new InterpolatedFloat();

	// the camera needs to be positioned
	@Override
	protected float updateCamera (float timeModFactor) {
		if (hasPlayer()) {
			// speed.set(playerCar.carState.currSpeedFactor, 0.02f);
			drift.set(playerCar.driftState.driftStrength, 0.02f);
		}

		float minZoom = GameWorldRenderer.MinCameraZoom;
		float maxZoom = GameWorldRenderer.MaxCameraZoom;

		float cameraZoom = (minZoom + GameWorldRenderer.ZoomWindow);
		cameraZoom += (maxZoom - cameraZoom) * timeModFactor;
		cameraZoom += 0.25f * GameWorldRenderer.ZoomWindow * drift.get();

		// cameraZoom = minZoom;

		cameraZoom = AMath.lerp(prevZoom, cameraZoom, 0.1f);
		cameraZoom = AMath.clampf(cameraZoom, minZoom, maxZoom);
		cameraZoom = AMath.fixupTo(cameraZoom, minZoom + GameWorldRenderer.ZoomWindow);
		// Gdx.app.log("", "zoom=" + cameraZoom);

		gameWorldRenderer.setCameraZoom(cameraZoom);
		prevZoom = cameraZoom;

		// update player's headlights and move the world camera to follows it, if there is a player
		if (hasPlayer()) {

			if (gameWorld.isNightMode()) {
				gameWorldRenderer.updatePlayerHeadlights(playerCar);
			}

			gameWorldRenderer.setCameraPosition(playerCar.state().position, playerCar.state().orientation,
				playerCar.carState.currSpeedFactor);

		} else if (isGhostActive(0)) {
			gameWorldRenderer.setCameraPosition(getGhost(0).state().position, getGhost(0).state().orientation, 0);
		} else {
			// no ghost, no player, WTF?
			gameWorldRenderer.setCameraPosition(Convert.mt2px(gameWorld.playerStart.position), gameWorld.playerStart.orientation, 0);
		}

		return cameraZoom;
	}

	// the game has been restarted
	@Override
	public void restartGame () {
		Gdx.app.log("SinglePlayerLogic", "Starting/restarting game");
		super.restartGame();
		gameTasksManager.messager.show("Game restarted", 3, Message.Type.Information, Position.Bottom, Size.Big);
	}

	// the game has been reset
	@Override
	public void resetGame () {
		Gdx.app.log("SinglePlayerLogic", "Resetting game");
		super.resetGame();
		gameTasksManager.messager.show("Game reset", 3, Message.Type.Information, Position.Bottom, Size.Big);
	}

	// a new Replay from the player is available: note that CommonLogic already perform
	// some basic filtering such as null checking, length validity, better-than-worst...
	@Override
	public void newReplay (Replay replay) {
		CarUtils.dumpSpeedInfo("Player", playerCar, replay.trackTimeSeconds);
	}
}

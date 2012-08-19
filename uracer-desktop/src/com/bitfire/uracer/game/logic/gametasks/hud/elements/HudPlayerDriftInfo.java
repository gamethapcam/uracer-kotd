
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.VMath;

/** Encapsulates player's drifting information shown on screen */
public final class HudPlayerDriftInfo extends HudElement {
	public enum EndDriftType {
		GoodDrift, BadDrift
	}

	// we need an HudLabel circular buffer since
	// the player could be doing combos and the time
	// needed for one single labelResult to ".slide"
	// and disappear could be higher than the time
	// needed for the user to initiate, perform and
	// finish the next drift.. in this case the label
	// will move from the last result position to the
	// current one
	private static final int MaxLabelResult = 3;

	// player info
	private EntityRenderState playerState = null;

	// presentation
	private HudLabel labelRealtime;
	private HudLabel[] labelResult;
	private HudLabel labelName;

	private int nextLabelResult = 0;

	private PlayerCar player;

	// gravitation
	private float carModelWidthPx, carModelLengthPx;
	private Vector2 displacement = new Vector2();
	private Vector2 tmpg = new Vector2();

	private Vector2 lastRealtimePos = new Vector2();
	private boolean began = false;

	public HudPlayerDriftInfo (ScalingStrategy scalingStrategy, PlayerCar player) {
		this.player = player;
		playerState = player.state();
		this.carModelWidthPx = Convert.mt2px(player.getCarModel().width);
		this.carModelLengthPx = Convert.mt2px(player.getCarModel().length);

		// labelRealtime role is to display PlayerCar values in real-time!
		labelRealtime = new HudLabel(scalingStrategy, Art.fontCurseYRbig, "+10.99", 0.5f);
		labelRealtime.setAlpha(0);
		lastRealtimePos.set(0, 0);

		labelResult = new HudLabel[MaxLabelResult];
		nextLabelResult = 0;
		for (int i = 0; i < MaxLabelResult; i++) {
			labelResult[i] = new HudLabel(scalingStrategy, Art.fontCurseR, "+10.99", 0.85f);
			labelResult[i].setAlpha(0);
		}

		// name is truncated at 16 chars
		String userName = "Manuel";
		labelName = new HudLabel(scalingStrategy, Art.fontCurseYRbig, userName, 1f);
		labelName.setAlpha(1);
		labelName.setPosition(0, 0);
	}

	@Override
	public void dispose () {
	}

	@Override
	public void onTick () {
		refreshLabelRealtime(false);

	}

	private void refreshLabelRealtime (boolean force) {
		if (force || (began && labelRealtime.isVisible())) {
			labelRealtime.setString("+" + NumberString.format(player.driftState.driftSeconds()));
		}
	}

	@Override
	public void onReset () {
		labelRealtime.setAlpha(0);
		for (int i = 0; i < MaxLabelResult; i++) {
			labelResult[i].setAlpha(0);
		}

		nextLabelResult = 0;
	}

	private float test = 0;

	@Override
	public void onRender (SpriteBatch batch) {
		test += 1;
		if (test > 360) {
			test -= 360;
		}

		gravitate(labelRealtime, 0);
// gravitate(labelName, test);

		lastRealtimePos.set(labelRealtime.getPosition());

		// draw earned/lost seconds
		if (labelRealtime.isVisible()) {
			labelRealtime.render(batch);
		}

		// draw player name
		Vector2 pp = GameRenderer.ScreenUtils.worldPxToScreen(playerState.position);

		labelName.setScale(0.6f);
// labelName.setPosition(pp.x, pp.y + (carModelWidthPx / 2 + labelName.boundsHeight / 2));
		labelName.setPosition(pp.x, pp.y + (carModelLengthPx / 2 + labelName.boundsHeight / 2) + 10);

		labelName.render(batch);

		// draw result
		for (int i = 0; i < MaxLabelResult; i++) {
			labelResult[i].render(batch);
		}
	}

	//
	// internal helpers
	//

	private void gravitate (HudLabel label, float offsetDeg) {
		label.setPosition(gravitate(label.boundsWidth, label.boundsHeight, offsetDeg));
	}

	/** Returns a position by placing a point on an imaginary circumference gravitating around the player, applying the specified
	 * orientation offset, expressed in radians, if any. */
	private Vector2 gravitate (float w, float h, float offsetDeg) {
		// compute heading
		tmpg.set(VMath.fromDegrees(playerState.orientation + offsetDeg));

		// compute displacement
		float displaceX = carModelWidthPx + w * 0.5f;
		float displaceY = carModelLengthPx + h * 0.5f;
		tmpg.mul(displaceX, displaceY);
		displaceX = tmpg.x;
		displaceY = tmpg.y;

		// gets pixel position and then displaces it
		tmpg.set(GameRenderer.ScreenUtils.worldPxToScreen(playerState.position));
		tmpg.sub(displaceX, displaceY);

		return tmpg;
	}

	//
	// supported external operations
	//

	/** Signals the hud element that the player is initiating a drift */
	public void beginDrift () {
		labelRealtime.fadeIn(300);
		began = true;
	}

	/** Signals the hud element that the player has finished drifting */
	public void endDrift (String message, EndDriftType type) {
		HudLabel result = labelResult[nextLabelResult++];

		if (nextLabelResult == MaxLabelResult) {
			nextLabelResult = 0;
		}

		switch (type) {
		case BadDrift:
			result.setFont(Art.fontCurseRbig);
			break;
		case GoodDrift:
		default:
			result.setFont(Art.fontCurseGbig);
			break;
		}

		result.setString(message);
		result.setPosition(lastRealtimePos);
		result.slide(type == EndDriftType.GoodDrift);

		began = false;
		refreshLabelRealtime(true);

		labelRealtime.fadeOut(300);
	}

}

package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.messager.Message.MessagePosition;
import com.bitfire.uracer.game.messager.Message.MessageSize;
import com.bitfire.uracer.game.messager.Message.Type;
import com.bitfire.uracer.game.player.Car;
import com.bitfire.uracer.game.player.CarModel;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

public final class HudDrifting extends HudElement {
	private Car car;
	private int carWidthPx, carLengthPx;

	private HudLabel labelRealtime;
	private HudLabel[] labelResult;

	// we need an HudLabel circular buffer since
	// the player could be doing combos and the time
	// needed for one single labelResult to ".slide"
	// and disappear could be higher than the time
	// needed for the user to initiate, perform and
	// finish the next drift.. in this case the label
	// will move from the last result position to the
	// current one
	private static final int MaxLabelResult = 3;

	private int nextLabelResult = 0;

	private Vector2 heading = new Vector2();

	private DriftStateEvent.Listener driftListener = new DriftStateEvent.Listener() {
		@Override
		public void driftStateEvent( DriftStateEvent.Type type ) {
			switch( type ) {
			case onBeginDrift:
				onBeginDrift();
				break;
			case onEndDrift:
				onEndDrift();
				break;
			}
		}
	};

	private void onBeginDrift() {
		labelRealtime.fadeIn( 300 );
	}

	private void onEndDrift() {
		DriftState drift = GameData.States.drift;
		Vector2 pos = tmpv.set( Director.screenPosForPx( car.state().position ) );

		labelRealtime.fadeOut( 300 );

		HudLabel result = labelResult[nextLabelResult++];
		if( nextLabelResult == MaxLabelResult ) {
			nextLabelResult = 0;
		}

		result.setPosition( pos.x - heading.x * (carWidthPx + result.halfBoundsWidth), pos.y - heading.y * (carLengthPx + result.halfBoundsHeight) );

		float driftSeconds = drift.driftSeconds();

		// premature end drift event due to collision?
		if( drift.hasCollided ) {
			result.setString( "-" + NumberString.format( driftSeconds ) );
			result.setFont( Art.fontCurseRbig );
		} else {
			result.setString( "+" + NumberString.format( driftSeconds ) );
			result.setFont( Art.fontCurseGbig );

			String seconds = NumberString.format( driftSeconds ) + "  seconds!";

			if( driftSeconds >= 1 && driftSeconds < 3f ) {
				GameData.Environment.messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
				GameData.Environment.messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 5f ) {
				GameData.Environment.messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, MessagePosition.Bottom, MessageSize.Big );
			}
		}

		result.slide();
	}

	public HudDrifting( Car car ) {
		GameEvents.driftState.addListener( driftListener );

		this.car = car;
		CarModel model = car.getCarModel();
		carWidthPx = (int)Convert.mt2px( model.width );
		carLengthPx = (int)Convert.mt2px( model.length );

		labelRealtime = new HudLabel( Art.fontCurseYRbig, "99.99", 0.5f );
		labelRealtime.setAlpha( 0 );

		labelResult = new HudLabel[ MaxLabelResult ];
		nextLabelResult = 0;
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i] = new HudLabel( Art.fontCurseR, "99.99", 0.85f );
			labelResult[i].setAlpha( 0 );
		}
	}

	@Override
	void onReset() {
		labelRealtime.setAlpha( 0 );
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i].setAlpha( 0 );
		}
		nextLabelResult = 0;
	}

	@Override
	void onTick() {
		heading.set( car.getSimulator().heading );
	}


	private Vector2 tmpv = new Vector2();
	private float lastDistance = 0f;

	@Override
	void onRender( SpriteBatch batch ) {
		DriftState drift = GameData.States.drift;

		// update from subframe-interpolated player position
		Vector2 pos = tmpv.set( Director.screenPosForPx( car.state().position ) );

		// float secRatio = 1f;
		// float distance = 0f;
		if( drift.isDrifting ) {
			// secRatio = AMath.clamp( (System.currentTimeMillis() - drift.driftStartTime) / 2000f, 0, 1);
			// labelRealtime.setAlpha( secRatio );
			// distance = (1f-secRatio) * 50f;
			// lastDistance = distance;
			lastDistance = 0;
		}

		labelRealtime.setPosition(
		// offset by heading.mul(distance factor)
				pos.x - heading.x * (carWidthPx + labelRealtime.halfBoundsWidth + lastDistance), pos.y - heading.y
						* (carLengthPx + labelRealtime.halfBoundsHeight + lastDistance) );

		//
		// draw earned/lost seconds
		//
		labelRealtime.setString( "+" + NumberString.format( drift.driftSeconds() ) );
		labelRealtime.render( batch );

		//
		// draw result
		//
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i].render( batch );
		}
	}

}
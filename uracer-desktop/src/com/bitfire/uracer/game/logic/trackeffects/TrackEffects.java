package com.bitfire.uracer.game.logic.trackeffects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.Manager;

public final class TrackEffects extends Task {
	private Manager<TrackEffect> manager = new Manager<TrackEffect>();

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( com.bitfire.uracer.game.events.GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				reset();
				break;
			}
		}
	};

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;
			Array<TrackEffect> items = manager.items;

			for( int i = 0; i < items.size; i++ ) {
				TrackEffect effect = items.get( i );
				if( effect != null ) {
					effect.render( batch );
				}
			}
		}
	};

	public TrackEffects() {
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, GameRendererEvent.Order.MINUS_4 );

		// TODO, custom render event
		// for CarSkidMarks GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_4 );
		// for SmokeTrails GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_3 );
	}

	public void add( TrackEffect effect ) {
		manager.add( effect );
	}

	public void remove( TrackEffect effect ) {
		manager.remove( effect );
	}

	@Override
	public void dispose() {
		super.dispose();
		manager.dispose();
	}

	@Override
	public void onTick() {
		Array<TrackEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			effect.tick();
		}
	}

	public void reset() {
		Array<TrackEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			effect.reset();
		}
	}

	public int getParticleCount() {
		Array<TrackEffect> items = manager.items;
		int total = 0;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			total += effect.getParticleCount();
		}

		return total;
	}
}

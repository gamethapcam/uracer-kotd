package com.bitfire.uracer.game.logic.post.animators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessingAnimator;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.CrtMonitor;
import com.bitfire.uracer.postprocessing.effects.Curvature;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoomer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;

public final class AggressiveWarm implements PostProcessingAnimator {
	public static final String Name = "AggressiveWarm";

	private GameWorld gameWorld;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Curvature curvature = null;

	public AggressiveWarm( GameWorld world, PostProcessing post ) {
		gameWorld = world;
		bloom = (Bloom)post.getEffect( "bloom" );
		zoom = (Zoomer)post.getEffect( "zoom" );
		vignette = (Vignette)post.getEffect( "vignette" );
		crt = (CrtMonitor)post.getEffect( "crt" );
		curvature = (Curvature)post.getEffect( "curvature" );

		reset();
	}

	@Override
	public void reset() {
		if( !Config.PostProcessing.Enabled ) {
			return;
		}

		if( bloom != null ) {
			float threshold = (gameWorld.isNightMode() ? 0.2f : 0.45f);
			Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
			bloom.setSettings( bloomSettings );
		}

		if( vignette != null ) {
			vignette.setCoords( 0.8f, 0.25f );
			vignette.setCenter( Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2 );
			vignette.setLut( Art.postXpro );
			vignette.setLutIndex( 7 );
			vignette.setEnabled( true );
		}

		if( crt != null ) {
			startMs = TimeUtils.millis();
			crt.setTime( 0 );

			// FIXME should find out a way to compute this per-resolution! OR!
			// Just try manually the game and write down the offset, this should work well, no much resolutions anyway
			// and will work better since this is human-tested!
			// tv.setOffset( 0.00145f ); // 1920x1080
			crt.setOffset( 0.002f );	// 1920x1080
			crt.setDistortion( 0.2f );
			crt.setZoom( 0.9f );
			crt.setTint( 0.95f, 0.8f, 1.0f );
		}

		if( curvature != null ) {
			curvature.setDistortion( 0.2f );
			curvature.setZoom( 0.9f );
		}
	}

	private float prevDriftStrength = 0;
	private long startMs = 0;

	@Override
	public void update( PlayerCar player, GhostCar ghost ) {
		if( player == null ) {
			return;
		}

		if( !Config.PostProcessing.Enabled ) {
			return;
		}

		float timeFactor = 1 - (URacer.timeMultiplier - GameLogic.TimeMultiplierMin) / (Config.Physics.PhysicsTimeMultiplier - GameLogic.TimeMultiplierMin);
		Vector2 playerScreenPos = GameRenderer.ScreenUtils.worldPxToScreen( player.state().position );

		float driftStrength = AMath.clamp( AMath.lerp( prevDriftStrength, player.driftState.driftStrength, 0.01f ), 0, 1 );
		prevDriftStrength = driftStrength;

		if( crt != null ) {
			// compute time (add noise)
			float secs = (float)(TimeUtils.millis() - startMs) / 1000;
			boolean randomNoiseInTime = false;
			if( randomNoiseInTime ) {
				crt.setTime( secs + MathUtils.random() / (MathUtils.random() * 64f + 0.001f) );
			} else {
				crt.setTime( secs );
			}
		}

		if( zoom != null && player != null ) {
			float zoomfactor = timeFactor;// * player.carState.currSpeedFactor;
			zoom.setOrigin( playerScreenPos );
			zoom.setBlurStrength( -0.1f * zoomfactor );
			zoom.setZoom( 1.0f + 0.15f * zoomfactor );
		}

		if( bloom != null ) {
			bloom.setBaseSaturation( AMath.lerp( 1, 0.15f, timeFactor ) );
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation( 1f - timeFactor * 0.5f );
			bloom.setThreshold( AMath.lerp( 0.4f, 0.45f, timeFactor ) );
		}

		if( vignette != null ) {
			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );

			if( vignette.controlSaturation ) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation( 1f - timeFactor * 0.55f );
				vignette.setSaturationMul( 1f + timeFactor * 0.125f );
			}

			// vignette.setCenter( playerScreenPos.x, playerScreenPos.y );
			// vignette.setCoords( 1.5f - driftStrength * 0.8f, 0.1f );

			vignette.setLutIntensity( timeFactor * 1.25f );
			vignette.setIntensity( timeFactor );
		}
	}
}

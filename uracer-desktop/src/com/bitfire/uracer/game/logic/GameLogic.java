package com.bitfire.uracer.game.logic;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.DebugHelper;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.CarState;
import com.bitfire.uracer.game.actors.CarStateEvent;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.helpers.PlayerTasks;
import com.bitfire.uracer.game.logic.hud.HudLabel;
import com.bitfire.uracer.game.logic.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.hud.elements.HudPlayerDriftInfo.EndDriftType;
import com.bitfire.uracer.game.logic.messager.Message;
import com.bitfire.uracer.game.logic.messager.Message.Position;
import com.bitfire.uracer.game.logic.messager.Message.Size;
import com.bitfire.uracer.game.logic.messager.Message.Type;
import com.bitfire.uracer.game.logic.messager.MessageAccessor;
import com.bitfire.uracer.game.logic.messager.Messager;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.animators.AggressiveCold;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.player.PlayerDriftStateEvent;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.WcTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.NumberString;

/** This concrete class manages to handle the inner game logic and its evolving states: it's all about gamedata, baby!
 * This should be refactored into smaller pieces of logic, that's where the components should came into.
 *
 * @author bmanuel */
public class GameLogic implements CarEvent.Listener, CarStateEvent.Listener, PlayerDriftStateEvent.Listener {
	// logic
	public boolean doQuit = false;

	// world
	private GameWorld gameWorld = null;

	// rendering
	// private GameRenderer gameRenderer = null;
	private GameWorldRenderer gameWorldRenderer = null;
	private PostProcessing postProcessing = null;

	// player
	private PlayerCar playerCar = null;
	private GhostCar ghostCar = null;

	// lap
	private LapManager lapManager = null;
	private boolean isFirstLap = true;

	// tasks
	private GameTasksManager gameTasksManager = null;
	private PlayerTasks playerTasks = null;

	// handles timeModulationBusy onComplete event
//	private long timeModStart = 0;
	private boolean timeModulation = false;
	private BoxedFloat timeMultiplier = new BoxedFloat();
	private Timeline seqIn, seqOut;
	public static final float TimeMultiplierMin = 0.3f;
	private TweenCallback timeModulationFinished = new TweenCallback() {
		@Override
		public void onEvent( int type, BaseTween<?> source ) {
			switch( type ) {
			case COMPLETE:
//				Gdx.app.log( "GameLogic", "Time modulation ended (took " + (TimeUtils.nanoTime() - timeModStart) + ")" );
			}
		}
	};

	public GameLogic( GameWorld gameWorld, GameRenderer gameRenderer, ScalingStrategy scalingStrategy ) {
		this.gameWorld = gameWorld;
		// this.gameRenderer = gameRenderer;
		this.gameWorldRenderer = gameRenderer.getWorldRenderer();
		this.doQuit = false;

		// create tweening support
		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );
		Gdx.app.log( "GameLogic", "Tweening helpers created" );

		// time modifier
		timeMultiplier.value = 1f;
		seqIn = Timeline.createSequence();
		seqOut = Timeline.createSequence();

		// post-processing
		postProcessing = new PostProcessing( gameWorld, gameRenderer );
		postProcessing.addAnimator( AggressiveCold.Name, new AggressiveCold( gameWorld, postProcessing ) );
		postProcessing.enableAnimator( AggressiveCold.Name );
		Gdx.app.log( "GameLogic", "Post-processing animator created" );

		// main game tasks
		gameTasksManager = new GameTasksManager( gameWorld, scalingStrategy );
		gameTasksManager.createTasks();

		// player tasks
		playerTasks = new PlayerTasks( gameTasksManager, scalingStrategy );

		lapManager = new LapManager( gameWorld );
		ghostCar = CarFactory.createGhost( gameWorld, CarPreset.Type.Default );

		// messager.show( "COOL STUFF!", 60, Message.Type.Information, MessagePosition.Bottom, MessageSize.Big );
	}

	public void dispose() {
		gameTasksManager.dispose();
		playerTasks.dispose();

		if( playerCar != null ) {
			playerCar.dispose();
		}

		if( ghostCar != null ) {
			ghostCar.dispose();
		}

		gameWorld.dispose();
		GameTweener.dispose();
		WcTweener.dispose();
	}

	/** Sets the player from the specified preset */
	public void setPlayer( CarPreset.Type presetType ) {
		if( hasPlayer() ) {
			Gdx.app.log( "GameLogic", "A player already exists." );
			return;
		}

		playerCar = CarFactory.createPlayer( gameWorld, presetType );

		configurePlayer( gameWorld, gameTasksManager.input, playerCar );
		Gdx.app.log( "GameLogic", "Player configured" );

		playerTasks.createTasks( playerCar, lapManager.getLapInfo() );
		Gdx.app.log( "GameLogic", "Game tasks created and configured" );

		registerPlayerEvents( playerCar );
		Gdx.app.log( "GameLogic", "Registered player-related events" );

		gameWorldRenderer.setRenderPlayerHeadlights( gameWorld.isNightMode() );

		restartGame();

		if( Config.Debug.UseDebugHelper ) {
			DebugHelper.setPlayer( playerCar );
		}
	}

	public void removePlayer() {
		if( !hasPlayer() ) {
			Gdx.app.log( "GameLogic", "There is no player to remove." );
			return;
		}

		// setting a null player (disabling player), unregister
		// previously registered events, if there was a player
		if( playerCar != null ) {
			unregisterPlayerEvents( playerCar );
			playerTasks.destroyTasks();
			playerCar.dispose();
		}

		playerCar = null;

		gameWorldRenderer.setRenderPlayerHeadlights( false );

		if( Config.Debug.UseDebugHelper ) {
			DebugHelper.setPlayer( null );
		}
	}

	private void registerPlayerEvents( PlayerCar player ) {
		player.carState.event.addListener( this, CarStateEvent.Type.onTileChanged );
		player.driftState.event.addListener( this, PlayerDriftStateEvent.Type.onBeginDrift );
		player.driftState.event.addListener( this, PlayerDriftStateEvent.Type.onEndDrift );
		player.event.addListener( this, CarEvent.Type.onCollision );
		player.event.addListener( this, CarEvent.Type.onComputeForces );
	}

	private void unregisterPlayerEvents( PlayerCar player ) {
		player.carState.event.removeListener( this, CarStateEvent.Type.onTileChanged );
		player.driftState.event.removeListener( this, PlayerDriftStateEvent.Type.onBeginDrift );
		player.driftState.event.removeListener( this, PlayerDriftStateEvent.Type.onEndDrift );
		player.event.removeListener( this, CarEvent.Type.onCollision );
		player.event.removeListener( this, CarEvent.Type.onComputeForces );
	}

	private void configurePlayer( GameWorld world, Input inputSystem, PlayerCar player ) {
		// create player and setup player input system and initial position in the world
		playerCar.setInputSystem( inputSystem );
		player.setWorldPosMt( world.playerStartPos, world.playerStartOrient );
		// player.setWorldPosMt( new Vector2(0,0), 0 );
		// playerCar.setWorldPosMt( new Vector2(50.29133f, -15.1445f), gameWorld.playerStartOrient );
	}

	public void setBestLocalReplay( Replay replay ) {
		restartGame();
		lapManager.setBestReplay( replay );
		// if( !hasPlayer() )
		{
			ghostCar.setReplay( replay );
		}
	}

	public GameWorld getGameWorld() {
		return gameWorld;
	}

	public boolean hasPlayer() {
		return playerCar != null;
	}

	public PlayerCar getPlayer() {
		return playerCar;
	}

	public void onSubstepCompleted() {
		gameTasksManager.physicsStep.onSubstepCompleted();
	}

	public void onBeforeRender() {
		boolean truncatePosition = true;

		// update player's headlights and move the world camera to follows it, if there is a player
		if( hasPlayer() ) {

			if( gameWorld.isNightMode() ) {
				gameWorldRenderer.updatePlayerHeadlights( playerCar );
			}

			gameWorldRenderer.setCameraPosition( playerCar.state().position, truncatePosition );
		} else if( ghostCar.hasReplay() ) {
			gameWorldRenderer.setCameraPosition( ghostCar.state().position, truncatePosition);
		} else {
			// no ghost, no player, WTF?
			gameWorldRenderer.setCameraPosition( gameWorld.playerStartPos, truncatePosition);
		}

		URacer.timeMultiplier = AMath.clamp( timeMultiplier.value, TimeMultiplierMin, Config.Physics.PhysicsTimeMultiplier );

		// tweener step
		WcTweener.update();
		GameTweener.update();

		// post-processing step
		postProcessing.onBeforeRender( playerCar, ghostCar );

		// Gdx.app.log( "GameLogic", NumberString.format(timeMultiplier.value) );
	}

	private Replay userRec = null;

	private int keycount = 0;

	public void onAcquireInput() {
		Input input = gameTasksManager.input;

		// dbg keys
		if( input.isPressed( Keys.NUM_1 ) || input.isReleased( Keys.NUM_1 ) ) {
			keycount++;
			// @formatter:off
			Gdx.app.log( "GameLogic", "Num1 => " +
					"isOn:" + (input.isOn(Keys.NUM_1) ? "1" : "0" ) + ", " +
					"isOff:" + (input.isOff(Keys.NUM_1) ? "1" : "0" ) + ", " +
					"isPressed:" + (input.isPressed(Keys.NUM_1) ? "1" : "0" ) + ", " +
					"isReleased:" + (input.isReleased(Keys.NUM_1) ? "1" : "0" ) + " (" + keycount + ")"
			);
			// @formatter:on
		}

		if( input.isPressed( Keys.C ) ) {

			if( lapManager.getBestReplay() != null ) {
				ghostCar.setReplay( lapManager.getBestReplay() );
			}

		} else if( input.isPressed( Keys.R ) ) {

			// restart

			restartGame();

		} else if( input.isPressed( Keys.T ) ) {

			// reset

			resetGame();

		} else if( input.isPressed( Keys.Z ) ) {

			// FIXME this should go in some sort of DebugLogic thing..

			// start recording
			playerCar.resetDistanceAndSpeed();
			ghostCar.setReplay( null );
			lapManager.abortRecording();
			userRec = lapManager.startRecording( playerCar );
			Gdx.app.log( "GameLogic", "Recording..." );

		} else if( input.isPressed( Keys.X ) ) {

			// FIXME this should go in some sort of DebugLogic thing..

			// stop recording and play
			playerCar.resetPhysics();
			lapManager.stopRecording();

			CarUtils.dumpSpeedInfo( "Player", playerCar, lapManager.getLastRecordedReplay().trackTimeSeconds );
			playerCar.resetDistanceAndSpeed();
			if( userRec != null ) {
				userRec.saveLocal( gameTasksManager.messager );
				ghostCar.setReplay( userRec );
			}

			// Gdx.app.log( "GameLogic", "Player final pos=" + playerCar.getBody().getPosition() );

		} else if( input.isPressed( Keys.Q ) ) {

			// quit

			Gdx.app.log( "GameLogic", "Quitting..." );
			Gdx.app.exit();
			doQuit = true;
			return;

		} else if( input.isPressed( Keys.O ) ) {

			// FIXME this should go in some sort of DebugLogic thing..

			// remove player

			removePlayer();

		} else if( input.isPressed( Keys.P ) ) {

			// FIXME this should go in some sort of DebugLogic thing..

			// add player

			setPlayer( CarPreset.Type.FordMustangShelbyGt500Coupe );

		} else if( input.isPressed( Keys.W ) ) {

			// FIXME this should go in some sort of DebugLogic thing..
			Config.Debug.RenderBox2DWorldWireframe = !Config.Debug.RenderBox2DWorldWireframe;

		} else if( input.isPressed( Keys.B ) ) {

			// FIXME this should go in some sort of DebugLogic thing..
			Config.Debug.Render3DBoundingBoxes = !Config.Debug.Render3DBoundingBoxes;

		} else if( input.isPressed( Keys.SPACE ) || input.isTouched( 1 ) ) {

			TweenEquation eqIn = Quad.OUT;
			TweenEquation eqOut = Quad.INOUT;

			timeModulation = !timeModulation;
//			timeModStart = TimeUtils.nanoTime();

			if( timeModulation ) {

				WcTweener.stop( timeMultiplier );
				seqIn = Timeline.createSequence();
				seqOut = Timeline.createSequence();

				seqIn.push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( TimeMultiplierMin ).ease( eqIn ) ).setCallback( timeModulationFinished );
				WcTweener.start( seqIn );

			} else {

				WcTweener.stop( timeMultiplier );
				seqIn = Timeline.createSequence();
				seqOut = Timeline.createSequence();

				seqOut.push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( Config.Physics.PhysicsTimeMultiplier ).ease( eqOut ) ).setCallback(
						timeModulationFinished );
				WcTweener.start( seqOut );
			}
		}
	}

	//
	// TODO, COULD THIS BE A TASK HANDLING IN-GAME USER CHOICES ??
	//

	public void restartGame() {
		restartLogic();
		gameTasksManager.restart();
	}

	public void resetGame() {
		restartLogic();
		resetLogic();
		gameTasksManager.reset();
	}

	private void resetPlayer( Car playerCar, GhostCar playerGhostCar ) {
		if( playerCar != null ) {
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed();
			playerCar.setWorldPosMt( gameWorld.playerStartPos, gameWorld.playerStartOrient );
		}

		if( playerGhostCar != null ) {
			playerGhostCar.resetPhysics();
			playerGhostCar.resetDistanceAndSpeed();
		}
	}

	private void restartLogic() {
		resetPlayer( playerCar, ghostCar );
		isFirstLap = true;
		timeModulation = false;
		timeMultiplier.value = Config.Physics.PhysicsTimeMultiplier;
		WcTweener.clear();
		GameTweener.clear();
		lapManager.abortRecording();
		gameTasksManager.restart();
	}

	private void resetLogic() {
		lapManager.abortRecording();
		lapManager.reset();
		gameTasksManager.reset();
	}

	//
	// EVENT HANDLERS
	//

	@Override
	public void carEvent( CarEvent.Type type, CarEvent.Data data ) {
		switch( type ) {
		case onCollision:
			if( playerCar.driftState.isDrifting ) {
				playerCar.driftState.invalidateByCollision();
			}
			break;
		case onComputeForces:
			lapManager.record( data.forces );
		}
	}

	@Override
	public void carStateEvent( CarState source, CarStateEvent.Type type ) {
		switch( type ) {
		case onTileChanged:
			playerTileChanged();
			break;
		}
	}

	// Pay attention here! Multiple DriftState objects trigger the same event! Check the source for
	// handling multiple and crossing beginDrift/endDrift calls!
	//
	// Anyway, we can't track GhostCar's drift states since we record the forces generated by the CarSimulator!
	@Override
	public void playerDriftStateEvent( PlayerCar player, PlayerDriftStateEvent.Type type ) {
		switch( type ) {
		case onBeginDrift:
			playerTasks.hudPlayerDriftInfo.beginDrift();
			break;
		case onEndDrift:
			String seconds = NumberString.format( player.driftState.driftSeconds() ) + "  seconds!";
			boolean driftEndByCollision = player.driftState.hasCollided;
			float driftSeconds = player.driftState.driftSeconds();

			if( !driftEndByCollision ) {
				if( driftSeconds >= 1 && driftSeconds < 3f ) {
					gameTasksManager.messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, Position.Middle, Size.Big );
				} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
					gameTasksManager.messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, Position.Middle, Size.Big );
				} else if( driftSeconds >= 5f ) {
					gameTasksManager.messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, Position.Bottom, Size.Big );
				}

				playerTasks.hudPlayerDriftInfo.endDrift( "+" + NumberString.format( driftSeconds ), EndDriftType.GoodDrift );

			} else {
				playerTasks.hudPlayerDriftInfo.endDrift( "-" + NumberString.format( driftSeconds ), EndDriftType.BadDrift );
			}

			break;
		}

		// Gdx.app.log( "GameLogic", "playerDriftStateEvent::ds=" + NumberString.format(
		// player.driftState.driftSeconds() ) + " (" + player.driftState.driftSeconds() + ")" );
	}

	private void playerTileChanged() {
		boolean onStartZone = (playerCar.carState.currTileX == gameWorld.playerStartTileX && playerCar.carState.currTileY == gameWorld.playerStartTileY);
		Messager messager = gameTasksManager.messager;

		if( onStartZone ) {
			if( isFirstLap ) {
				isFirstLap = false;

				// any record to play?
				if( lapManager.hasAnyReplay() ) {
					ghostCar.setReplay( lapManager.getAnyReplay() );
				}

				lapManager.startRecording( playerCar );

			} else {
				lapManager.stopRecording();

				if( !lapManager.hasAllReplays() ) {
					// only one single valid replay

					Replay any = lapManager.getAnyReplay();
					ghostCar.setReplay( any );
					any.saveLocal( messager );
					messager.show( "GO!  GO!  GO!", 3f, Type.Information, Position.Middle, Size.Big );

				} else {

					// both valid, replay best, overwrite worst

					Replay best = lapManager.getBestReplay();
					Replay worst = lapManager.getWorstReplay();

					float bestTime = AMath.round( best.trackTimeSeconds, 2 );
					float worstTime = AMath.round( worst.trackTimeSeconds, 2 );
					float diffTime = AMath.round( worstTime - bestTime, 2 );

					if( AMath.equals( worstTime, bestTime ) ) {
						// draw!
						messager.show( "DRAW!", 3f, Type.Information, Position.Top, Size.Big );
					} else {
						// has the player managed to beat the best lap?
						if( lapManager.isLastBestLap() ) {
							messager.show( "-" + NumberString.format( diffTime ) + " seconds!", 3f, Type.Good, Position.Top, Size.Big );
						} else {
							messager.show( "+" + NumberString.format( diffTime ) + " seconds", 3f, Type.Bad, Position.Top, Size.Big );
						}
					}

					ghostCar.setReplay( best );
					best.saveLocal( messager );
				}

				CarUtils.dumpSpeedInfo( "Player", playerCar, lapManager.getLastRecordedReplay().trackTimeSeconds );

				lapManager.startRecording( playerCar );
			}

			playerCar.resetDistanceAndSpeed();
		}
	}
}

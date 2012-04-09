package com.bitfire.uracer.game.audio;

public interface ISoundEffect {
	public void onDispose();

	public void onStart();

	public void onTick();

	public void onStop();

	public void onReset();
}
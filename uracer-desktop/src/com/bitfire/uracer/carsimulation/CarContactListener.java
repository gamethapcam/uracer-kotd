package com.bitfire.uracer.carsimulation;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

public class CarContactListener implements ContactListener
{
	private CarImpactManager impactManager = new CarImpactManager();

	@Override
	public void beginContact( Contact contact )
	{
//		if(Box2DUtils.isCar(contact.getFixtureA()) || Box2DUtils.isCar(contact.getFixtureB()))
//			System.out.println("beginContact");
	}

	@Override
	public void endContact( Contact contact )
	{
//		if(Box2DUtils.isCar(contact.getFixtureA()) || Box2DUtils.isCar(contact.getFixtureB()))
//			System.out.println("endContact");
	}

	@Override
	public void preSolve( Contact contact, Manifold oldManifold )
	{
	}

	@Override
	public void postSolve( Contact contact, ContactImpulse impulse )
	{
		impactManager.impact( contact, impulse );
	}
}

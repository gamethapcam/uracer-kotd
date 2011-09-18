package com.bitfire.uracer.simulations.car;

/**
 * Represents the forces computed by the car simulator, given the CarInput input forces.
 *
 * @author manuel
 *
 */

public class CarForces
{
	public float velocity_x;
	public float velocity_y;
	public float angularVelocity;

	public CarForces()
	{
		reset();
	}

	public CarForces(CarForces other)
	{
		set(other);
	}

	public void reset()
	{
		velocity_x = velocity_y = angularVelocity = 0f;
	}

	public CarForces set(CarForces other)
	{
		this.velocity_x = other.velocity_x;
		this.velocity_y = other.velocity_y;
		this.angularVelocity = other.angularVelocity;
		return this;
	}
}
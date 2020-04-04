package com.timofriedl.spaceai.celestialobject;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.timofriedl.simulationbox.Simulation;
import com.timofriedl.simulationbox.object.MassObject;
import com.timofriedl.simulationbox.vector.Vector2D;
import com.timofriedl.spaceai.SpaceSimulation;

/**
 * Represents an astronomical object in this {@link SpaceSimulation}.
 * 
 * @author Timo Friedl
 */
public class CelestialObject extends MassObject {

	/**
	 * the texture of this celestial object
	 */
	protected BufferedImage texture;

	/**
	 * Creates a new celestial object.
	 * 
	 * @param simulation    the main simulation reference
	 * @param position      the center position of this object
	 * @param speed         the velocity of this object in units per tick
	 * @param diameter      the diameter of this object
	 * @param rotation      the rotation angle of this object in radians
	 * @param rotationSpeed the rotation speed of this object in radians per tick
	 * @param mass          the mass of this object
	 */
	public CelestialObject(Simulation simulation, Vector2D position, Vector2D speed, double diameter, double rotation,
			double rotationSpeed, double mass, BufferedImage texture) {
		super(simulation, position, speed, new Vector2D(diameter, diameter), rotation, rotationSpeed, mass);

		this.texture = texture;
	}

	@Override
	public void render(Graphics2D g) {
		simulation.getCamera().drawImage(g, texture, position, size, rotation);
	}

	/**
	 * @return the radius of this {@link CelestialObject}
	 */
	public double radius() {
		return size.getX() * 0.5;
	}

}

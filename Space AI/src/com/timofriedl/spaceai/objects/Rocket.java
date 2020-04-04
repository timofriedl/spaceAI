package com.timofriedl.spaceai.objects;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.timofriedl.neuralnetwork.DNN;
import com.timofriedl.neuralnetwork.Layer;
import com.timofriedl.neuralnetwork.Neuron;
import com.timofriedl.simulationbox.object.MassObject;
import com.timofriedl.simulationbox.vector.Vector2D;
import com.timofriedl.spaceai.SpaceSimulation;
import com.timofriedl.spaceai.celestialobject.CelestialObject;

/**
 * A neural network controlled rocket that flies in space.
 * 
 * @author Timo Friedl
 */
public class Rocket extends MassObject implements Comparable<Rocket> {

	/**
	 * the width and height of a rocket
	 */
	private static final Vector2D SIZE = new Vector2D(400.0, 800.0);

	/**
	 * the mass of a rocket
	 */
	private static final double MASS = 30.0;

	/**
	 * the maximal rocket thrust force
	 */
	private static final double MAX_THRUST = 261.0;

	/**
	 * the maximal rocket rotation force
	 */
	private static final double MAX_ROTATION_FORCE = 0.02;

	/**
	 * the number of {@link Neuron}s per {@link Layer} in the {@link DNN}
	 */
	private static final int[] LAYER_SIZES = { 8, 6, 4 };

	/**
	 * the rocket image
	 */
	private final BufferedImage texture;

	/**
	 * the fire image
	 */
	private final BufferedImage fireTexture;

	/**
	 * the current thrust force of this rocket
	 */
	private double thrust = 0.0;

	/**
	 * the current rotation force
	 */
	private double rotationForce;

	/**
	 * the neural network that controls this rocket
	 */
	private DNN net;

	/**
	 * the total score of this rocket
	 */
	private double score = 0;

	/**
	 * has this rocket landed on the moon yet?
	 */
	private boolean landed = false;

	/**
	 * has this rocket crashed the moon?
	 */
	private boolean crashed = false;

	/**
	 * Creates a new rocket.
	 * 
	 * @param simulation the reference to the main simulation instance
	 * @param position   the center position of this rocket
	 * @param rotation   the rotation of this rocket in radians
	 * @param texture    the texture image to render this rocket
	 */
	public Rocket(SpaceSimulation simulation, Vector2D position, double rotation, BufferedImage texture,
			BufferedImage fireTexture) {
		super(simulation, position, Vector2D.ZERO, SIZE, rotation, 0.0, MASS);

		this.texture = texture;
		this.fireTexture = fireTexture;

		net = new DNN(LAYER_SIZES);
	}

	@Override
	public void tick() {
		tickNetwork();

		tickGravityTo(((SpaceSimulation) simulation).getEarth());
		tickGravityTo(((SpaceSimulation) simulation).getMoon());

		speed = speed.add(Vector2D.fromAngle(rotation - Math.PI / 2.0, thrust / mass));
		rotationSpeed += rotationForce / mass;

		tickCollisions();

		super.tick();
		rotation %= Math.PI * 2.0;

		tickScore();
	}

	/**
	 * Calculates the score reward for this particular simulation tick.
	 */
	private void tickScore() {
		final Vector2D md = moonDis();
		final double mdl = md.length();
		final double mr = ((SpaceSimulation) simulation).getMoon().radius();

		// distance to moon
		final double scoreDtm = mdl < mr ? 0.0 : minMax(mdl, mr, 300_000.0, 5.0, 0.0);

		// angle (facing direction - moon)
		final double scoreFdm = minMax(Math.abs(md.angleTo(new Vector2D(0.0, 1.0).rotate(rotation))), 0.0, Math.PI,
				20.0, 0.0);

		// angle (moving direction - moon)
		final double scoreMdm = speed.squareLength() == 0.0 ? 0.0
				: minMax(Math.abs(md.angleTo(speed)), 0.0, Math.PI, 40.0, 0.0);

		// speed
		final double scoreSpd = minMax(speed.length(), 0.0, 10_000.0, 80.0, 0.0);

		// rotation speed
		final double scoreRts = minMax(Math.abs(rotationSpeed), 0.0, 10.0, 40.0, 0.0);

		// Add score values
		if (mdl >= mr)
			score += scoreDtm + scoreMdm + scoreRts;

		if (mdl < 100_000.0 && mdl >= mr)
			score += scoreSpd;

		if (mdl < 20_000.0 && mdl >= mr)
			score += scoreFdm + scoreSpd;
	}

	/**
	 * Handles collisions with {@link CelestialObject}s.
	 */
	private void tickCollisions() {
		if (earthDis().squareLength() < Math.pow(((SpaceSimulation) simulation).getEarth().radius(), 2)) {
			position = SpaceSimulation.ROCKET_START_POS;
			rotation = 0.0;
			rotationSpeed = 0.0;
			speed = Vector2D.ZERO;
		}

		if (moonDis().squareLength() < Math.pow(((SpaceSimulation) simulation).getMoon().radius(), 2)) {
			if (speed.length() > 100.0)
				crashed = true;
			else if (!landed)
				score += 150_000.0 / speed.length();
			landed = true;

			speed = Vector2D.ZERO;
			rotationSpeed = 0.0;
			thrust = 0.0;
		}
	}

	/**
	 * Calculates input values for the {@link DNN} and passes them in the network.
	 */
	private void tickNetwork() {
		final double[] inputs = new double[LAYER_SIZES[0]];

		int pos = 0;

		inputs[pos++] = minMax(speed.length(), 0.0, 10_000.0, 0.0, 1.0);
		inputs[pos++] = minMax(speed.rotation(), -Math.PI, Math.PI, 0.0, 1.0);

		inputs[pos++] = minMax(rotation, 0.0, Math.PI * 2.0, 0.0, 1.0);
		inputs[pos++] = minMax(rotationSpeed, -10.0, 10.0, 0.0, 1.0);

		inputs[pos++] = minMax(moonDis().length(), 0.0, 1_000_000.0, 0.0, 1.0);
		inputs[pos++] = minMax(moonDis().rotation(), -Math.PI, Math.PI, 0.0, 1.0);

		inputs[pos++] = minMax(earthDis().length(), 0.0, 1_000_000.0, 0.0, 1.0);
		inputs[pos++] = minMax(earthDis().rotation(), -Math.PI, Math.PI, 0.0, 1.0);

		final double[] outputs = net.feedForward(inputs);

		this.thrust = outputs[0] < 0.5 ? 0.0
				: outputs[0] < 0.75 ? MAX_THRUST : minMax(outputs[0], 0.75, 1.0, MAX_THRUST, 0.0);

		final double accL = minMax(outputs[1], 0.0, 1.0, 0.0, 1.0);
		final double accR = minMax(outputs[2], 0.0, 1.0, 0.0, 1.0);
		final double brRot = minMax(outputs[3], 0.0, 1.0, 0.0, 1.0);

		if (brRot > 0.5)
			this.rotationForce = minMax(rotationSpeed, -10.0, 10.0, MAX_ROTATION_FORCE / mass,
					-MAX_ROTATION_FORCE / mass);
		else if (accL > accR)
			this.rotationForce = accL * MAX_ROTATION_FORCE;
		else
			this.rotationForce = accR * -MAX_ROTATION_FORCE;
	}

	/**
	 * Min-max helper function to normalize values.
	 * 
	 * @param value  the value to normalize
	 * @param minIn  the lowest allowed input value
	 * @param maxIn  the highest allowed input value
	 * @param minOut the lowest allowed output value
	 * @param maxOut the highest allowed output value
	 * @return the normalized value or min (max) if the value is less than zero
	 *         (greater than one)
	 */
	private double minMax(double value, double minIn, double maxIn, double minOut, double maxOut) {
		if (value < minIn)
			return minOut;
		else if (value > maxIn)
			return maxOut;

		final double relIn = (value - minIn) / (maxIn - minIn);

		return minOut + relIn * (maxOut - minOut);
	}

	/**
	 * @return the current distance from the center of the rocket to the center of
	 *         the moon
	 */
	private Vector2D moonDis() {
		return ((SpaceSimulation) simulation).getMoon().getPosition().subtract(position);
	}

	/**
	 * @return the current distance from the center of the rocket to the center of
	 *         the earth
	 */
	private Vector2D earthDis() {
		return ((SpaceSimulation) simulation).getEarth().getPosition().subtract(position);
	}

	@Override
	public void render(Graphics2D g) {
		if (crashed)
			return;

		final Vector2D firePos = position
				.add(new Vector2D(0.0, 0.7 * size.getY() * (thrust / MAX_THRUST)).rotate(rotation));

		simulation.getCamera().drawImage(g, fireTexture, firePos, size, rotation);
		simulation.getCamera().drawImage(g, texture, position, size, rotation);
	}

	@Override
	public int compareTo(Rocket r) {
		return Double.compare(r.score, score);
	}

	/**
	 * Mutates this {@link Rocket} to a new instance.
	 * 
	 * @return the mutated version of this rocket.
	 */
	public Rocket mutate() {
		final Rocket r = new Rocket((SpaceSimulation) simulation, position, rotation, texture, fireTexture);
		r.setNet(net.mutate(Math.random() * 0.1));
		return r;
	}

	/**
	 * Resets the attributes of this {@link Rocket} to default values.
	 */
	public void reset() {
		position = SpaceSimulation.ROCKET_START_POS;
		speed = Vector2D.ZERO;
		rotation = 0.0;
		rotationSpeed = 0.0;
		score = 0.0;
		landed = false;
		crashed = false;
	}

	/**
	 * @return the {@link DNN} that controls this {@link Rocket}
	 */
	public DNN getNet() {
		return net;
	}

	/**
	 * @param net the new {@link DNN} that controls this {@link Rocket}
	 */
	public void setNet(DNN net) {
		this.net = net;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}

}

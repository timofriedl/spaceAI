package com.timofriedl.spaceai.gravity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import com.timofriedl.simulationbox.Simulation;
import com.timofriedl.simulationbox.object.MassObject;
import com.timofriedl.simulationbox.object.common.Grid;
import com.timofriedl.simulationbox.vector.Vector2D;

/**
 * A grid of arrows that indicate a gravity field.
 */
public class GravityField extends Grid {

	/**
	 * the option to render or hide this field
	 */
	private boolean show = false;

	/**
	 * the position and size of the arrows to render
	 */
	private Vector2D[][][] arrows;

	/**
	 * the objects to calculate the gravity
	 */
	private ArrayList<MassObject> gravityObjects;

	/**
	 * Creates a new gravity field that indicates gravitational forces with arrows.
	 * 
	 * @param simulation     the reference to the main simulation instance.
	 * @param position       the center position of this gravity field
	 * @param gridSize       the number of spaces between arrow lines
	 * @param squareSize     the space between two arrow center positions
	 * @param gravityObjects the objects to calculate the gravity
	 */
	public GravityField(Simulation simulation, Vector2D position, int gridSize, double squareSize,
			ArrayList<MassObject> gravityObjects) {
		super(simulation, position, gridSize, squareSize, null);

		this.gravityObjects = gravityObjects;

		arrows = new Vector2D[gridSize + 1][gridSize + 1][2];
	}

	@Override
	public void tick() {
		if (arrows[0][0][0] != null)
			return;

		final Vector2D ul = position.subtract(size.scale(0.5));

		for (int y = 0; y < arrows.length; y++)
			for (int x = 0; x < arrows[0].length; x++) {
				arrows[y][x][0] = ul.add(new Vector2D(x * squareSize, y * squareSize));
				final Vector2D f = gravityForceAt(new Vector2D(x * squareSize + ul.getX(), y * squareSize + ul.getY()));
				arrows[y][x][1] = f.length() < 1000.0 ? f.scaleTo(1000.0)
						: f.length() > 10_000.0 ? f.scaleTo(10_000.0) : f;

				arrows[y][x][0] = arrows[y][x][0].subtract(arrows[y][x][1].scale(0.5));
			}
	}

	/**
	 * Calculates the gravitational force at a given position
	 * 
	 * @param position the position to calculate the gravity
	 * @return a {@link Vector2D} that represents the gravity force
	 */
	private Vector2D gravityForceAt(Vector2D position) {
		Vector2D force = Vector2D.ZERO;

		for (MassObject o : gravityObjects)
			force = force.add(gravityForceToAt(o, position));

		return force;
	}

	/**
	 * Calculates the gravitational force to one {@link MassObject} at a given
	 * position.
	 * 
	 * @param position the position to calculate the gravity
	 * @return a {@link Vector2D} that represents the gravity force
	 */
	private Vector2D gravityForceToAt(MassObject o, Vector2D position) {
		if (o.getPosition().equals(position))
			return Vector2D.ZERO;

		final Vector2D dis = o.getPosition().subtract(position);

		return dis.scale(1E-8 * o.getMass() / Math.pow(dis.length(), 3));
	}

	@Override
	public void render(Graphics2D g) {
		if (!show)
			return;

		for (int y = 0; y < arrows.length; y++)
			for (int x = 0; x < arrows[0].length; x++) {
				final int v = (int) (0xFF * arrows[y][x][1].length() / 10_000.0);
				g.setColor(new Color(v, 0xFF - v, 0x00, 0x7F));

				simulation.getCamera().fillArrow(g, arrows[y][x][0], arrows[y][x][0].add(arrows[y][x][1]),
						arrows[y][x][1].length(), arrows[y][x][1].length() / 5.0);
			}
	}

	/**
	 * @return the option to render or hide this field
	 */
	public boolean isShow() {
		return show;
	}

	/**
	 * @param show the option to render or hide this field
	 */
	public void setShow(boolean show) {
		this.show = show;
	}

}

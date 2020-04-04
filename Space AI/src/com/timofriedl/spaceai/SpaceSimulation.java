package com.timofriedl.spaceai;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import com.timofriedl.neuralnetwork.visual.DNNVisualizer;
import com.timofriedl.simulationbox.Simulation;
import com.timofriedl.simulationbox.assets.Assets;
import com.timofriedl.simulationbox.camera.Camera;
import com.timofriedl.simulationbox.display.Window;
import com.timofriedl.simulationbox.gameloop.GameLoop;
import com.timofriedl.simulationbox.object.MassObject;
import com.timofriedl.simulationbox.object.common.Grid;
import com.timofriedl.simulationbox.vector.Vector2D;
import com.timofriedl.spaceai.camera.SpaceKeyManager;
import com.timofriedl.spaceai.celestialobject.CelestialObject;
import com.timofriedl.spaceai.gravity.GravityField;
import com.timofriedl.spaceai.objects.Rocket;

/**
 * A 2D space simulation with {@link Rocket}s that learn to land on the moon.
 * 
 * One distance-unit corresponds to 1 kilometre</br>
 * One mass-unit corresponds to one metric ton</br>
 * 
 * (sorry USA)
 * 
 * If you found any mistakes or have suggestions for improvements please
 * consider contacting me:
 * 
 * mail@timofriedl.com
 * 
 * @author Timo Friedl
 */
public class SpaceSimulation extends Simulation {

	/**
	 * the number of {@link Rocket}s in this simulation
	 */
	private static final int POPULATION_SIZE = 1_000;

	/**
	 * the start position of the rockets
	 */
	public static final Vector2D ROCKET_START_POS = new Vector2D(0.0, -0.5 * 12_742.0 - 400.0);

	/**
	 * the earth object
	 */
	private CelestialObject earth;

	/**
	 * the moon object
	 */
	private CelestialObject moon;

	/**
	 * the grid for better map orientation
	 */
	private Grid grid;

	/**
	 * the gravity visualization
	 */
	private GravityField gravityField;

	/**
	 * the pause option
	 */
	private boolean pause = true;

	/**
	 * the option to let the cam move automatically
	 */
	private boolean autoCam = true;

	/**
	 * the option to fasten this simulation
	 */
	private boolean fastForward = false;

	/**
	 * the celestial objects that attract other objects
	 */
	private ArrayList<MassObject> gravityObjects;

	/**
	 * the neural-network-controlled rockets in this simulation
	 */
	private CopyOnWriteArrayList<Rocket> rockets;

	/**
	 * the number of rocket generations
	 */
	private int generation = 0;

	/**
	 * the best rocket of the previous generation
	 */
	private Rocket prevBestRocket = null;

	/**
	 * the highest ever reached score
	 */
	private double highScore = -Double.MAX_VALUE;

	/**
	 * number of ticks in this simulation;
	 */
	private long ticks = 0;

	/**
	 * option to render all rockets or only the best
	 */
	private boolean renderAllRockets = true;

	/**
	 * Creates a new space simulation instance using the "Simulation Box 2D"
	 * library.
	 */
	public SpaceSimulation() {
		super("Space Simulation", Color.BLACK);
	}

	@Override
	public void init() {
		Camera.MIN_ZOOM = 0.0005;
		Camera.MAX_ZOOM = 0.05;
		Camera.ANIMATION_SPEED = 0.1;
		Camera.ROTATION_STEPS = 8;

		grid = new Grid(this, Vector2D.ZERO, 200, 10_000.0, Color.WHITE);
		grid.setLineWidth(50.0);

		initSpaceObjects();
		initPopulation();

		new SpaceKeyManager(this, earth, moon);

		window.getCanvas().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2)
					nextGeneration();
			}
		});
	}

	/**
	 * Starts the next generation with the best rockets of the previous.
	 */
	private void nextGeneration() {
		generation++;
		boolean success = false;
		while (!success)
			try {
				Collections.sort(rockets);
				success = true;
			} catch (Exception e) {
				// try again
			}

		removeBadRockets();
		mutateGoodRockets();

		prevBestRocket = rockets.get(0);

		if (prevBestRocket.getScore() > highScore) {
			highScore = prevBestRocket.getScore();
			System.out.println("Generation " + generation + ": " + highScore);
		}

		rockets.forEach(Rocket::reset);
	}

	/**
	 * Removes some bad {@link Rocket}s in order to make space for mutations of the
	 * good ones.
	 */
	private void removeBadRockets() {
		for (int i = 0; i < POPULATION_SIZE / 3; i++) {
			int index = rockets.size() - 1;

			while (Math.random() > 0.5)
				index--;

			if (index < 0)
				index = rockets.size() - 1;

			rockets.remove(index);
		}
	}

	/**
	 * Fills the <code>rockets</code> {@link ArrayList} with mutations of good
	 * {@link Rocket}s.
	 */
	private void mutateGoodRockets() {
		while (rockets.size() < POPULATION_SIZE) {
			int index = 0;

			while (Math.random() > 0.1)
				index++;

			if (index >= rockets.size())
				index = 0;

			rockets.add(rockets.get(0).mutate());
		}
	}

	/**
	 * Initializes the rocket population.
	 */
	private void initPopulation() {
		try {
			rockets = new CopyOnWriteArrayList<>();

			final BufferedImage rocketImage = Assets.loadImage("res/rocket.png");
			final BufferedImage fireImage = Assets.loadImage("res/fire.png");

			while (rockets.size() < POPULATION_SIZE)
				rockets.add(new Rocket(this, ROCKET_START_POS, 0.0, rocketImage, fireImage));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method for space object initialization.
	 */
	private void initSpaceObjects() {
		try {
			final BufferedImage earthImage = Assets.loadImage("res/earth.png");
			final BufferedImage moonImage = Assets.loadImage("res/moon.png");

			earth = new CelestialObject(this, Vector2D.ZERO, Vector2D.ZERO, 12_742.0, 0.0, 0.0, 5.972E21, earthImage);
			moon = new CelestialObject(this, new Vector2D(0.0, -384_400.0), Vector2D.ZERO, 3474.2, 0.0, 0.0, 7.349E19,
					moonImage);

			gravityObjects = new ArrayList<>();
			gravityObjects.add(earth);
			gravityObjects.add(moon);
			gravityField = new GravityField(this, Vector2D.ZERO, 50, 20_000.0, gravityObjects);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tick() {
		tickAutoCam();
		grid.tick();
		gravityField.tick();

		if (pause)
			return;

		for (int i = 0; i < (fastForward ? 4 : 1); i++)
			tickSimulation();
	}

	/**
	 * Performs calculations for {@link CelestialObject}s and {@link Rocket}s.
	 */
	public void tickSimulation() {
		ticks++;
		earth.tick();
		moon.tick();
		rockets.forEach(Rocket::tick);

		if (ticks % (20 * GameLoop.TPS) == 0)
			nextGeneration();
	}

	/**
	 * Moves the {@link Camera} automatically if <code>autoCam</code> is set.
	 */
	private void tickAutoCam() {
		if (!autoCam)
			return;

		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		if (prevBestRocket != null) {
			minX = Math.min(moon.getPosition().getX(), prevBestRocket.getPosition().getX());
			maxX = Math.max(moon.getPosition().getX(), prevBestRocket.getPosition().getX());
			minY = Math.min(moon.getPosition().getY(), prevBestRocket.getPosition().getY());
			maxY = Math.max(moon.getPosition().getY(), prevBestRocket.getPosition().getY());
		} else {
			for (Rocket r : rockets) {
				final Vector2D p = r.getPosition();

				if (p.getX() < minX)
					minX = p.getX();
				if (p.getX() > maxX)
					maxX = p.getX();
				if (p.getY() < minY)
					minY = p.getY();
				if (p.getY() > maxY)
					maxY = p.getY();
			}
		}

		final Vector2D min = new Vector2D(minX, minY);
		final Vector2D max = new Vector2D(maxX, maxY);
		final Vector2D c = min.add(max).scale(0.5);

		final double zoomX = Window.WIDTH / (maxX - minX);
		final double zoomY = Window.HEIGHT / (maxY - minY);
		final double zoom = Math.max(Camera.MIN_ZOOM, Math.min(Camera.MAX_ZOOM, Math.min(zoomX, zoomY) * 0.7));

		camera.setPositionAim(c);
		camera.setRotationAim(0.0);
		camera.setZoomAim(zoom);
	}

	@Override
	public void render(Graphics2D g) {
		grid.render(g);
		gravityField.render(g);
		earth.render(g);
		moon.render(g);
		if (renderAllRockets)
			rockets.forEach(r -> r.render(g));
		else if (prevBestRocket != null)
			prevBestRocket.render(g);

		focusBestRocket(g);
		renderGenerationText(g);

		if (prevBestRocket != null)
			DNNVisualizer.drawDNN(g, prevBestRocket.getNet(), 30.0, 200.0, 200.0, 300.0);
	}

	/**
	 * Renders the generation text on screen.
	 * 
	 * @param g the {@link Graphics2D} instance to draw on
	 */
	private void renderGenerationText(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 40));
		g.drawString("Generation " + generation, 20, 50);
	}

	/**
	 * Draws circles around the best and previously best rockets.
	 * 
	 * @param g the {@link Graphics2D} instance to draw on
	 */
	private void focusBestRocket(Graphics2D g) {
		Rocket currentBestRocket = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (Rocket r : rockets)
			if (r.getScore() > bestScore) {
				bestScore = r.getScore();
				currentBestRocket = r;
			}

		if (currentBestRocket != null && renderAllRockets) {
			g.setColor(new Color(0x00, 0xFF, 0x00, 0x80));
			camera.drawCircle(g, currentBestRocket.getPosition(), 10_000.0, 500.0);
		}

		if (prevBestRocket != null) {
			g.setColor(new Color(0x00, 0x80, 0xFF, 0x80));
			camera.drawCircle(g, prevBestRocket.getPosition(), 10_000.0, 500.0);
		}
	}

	/**
	 * pauses or resumes the simulation, depending on the current state
	 */
	public void togglePause() {
		pause = !pause;
	}

	/**
	 * toggles the auto cam mode
	 */
	public void toggleAutoCam() {
		autoCam = !autoCam;
	}

	/**
	 * toggles the option to render all rockets or only the best
	 */
	public void toggleRenderAllRockets() {
		renderAllRockets = !renderAllRockets;
	}

	/**
	 * toggles the fast forward mode
	 */
	public void toggleFastForward() {
		fastForward = !fastForward;
	}

	public static void main(String[] args) {
		new SpaceSimulation();
	}

	/**
	 * @return the earth object
	 */
	public CelestialObject getEarth() {
		return earth;
	}

	/**
	 * @return the moon object
	 */
	public CelestialObject getMoon() {
		return moon;
	}

	/**
	 * @return the grid for better map orientation
	 */
	public Grid getGrid() {
		return grid;
	}

	/**
	 * @return the gravityField
	 */
	public GravityField getGravityField() {
		return gravityField;
	}

	/**
	 * @return the pause option
	 */
	public boolean isPause() {
		return pause;
	}

	/**
	 * @param pause the new pause option
	 */
	public void setPause(boolean pause) {
		this.pause = pause;
	}

}

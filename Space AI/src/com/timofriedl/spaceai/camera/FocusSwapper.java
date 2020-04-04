package com.timofriedl.spaceai.camera;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.timofriedl.simulationbox.Simulation;
import com.timofriedl.simulationbox.camera.Camera;
import com.timofriedl.simulationbox.object.SimulationObject;

/**
 * A key listener who changes the focus of the {@link Simulation} {@link Camera}
 * to different {@link SimulationObject}s.
 * 
 * @author Timo Friedl
 */
public class FocusSwapper implements KeyListener {

	/**
	 * the reference to the main simulation instance
	 */
	private final Simulation simulation;

	/**
	 * the objects to focus
	 */
	private final SimulationObject[] focusObjects;

	/**
	 * Creates a new focus swapper who changes the focus of the {@link Simulation}
	 * {@link Camera} to different {@link SimulationObject}s.
	 * 
	 * @param simulation   the reference to the main simulation instance
	 * @param focusObjects the objects to focus
	 */
	public FocusSwapper(Simulation simulation, SimulationObject... focusObjects) {
		this.simulation = simulation;
		this.focusObjects = focusObjects;

		if (focusObjects.length > 9)
			throw new IllegalArgumentException("Focus Swapper must not have more than 9 objects to focus.");

		simulation.getWindow().getCanvas().addKeyListener(this);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// ignore
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() < 49 || e.getKeyCode() >= 49 + focusObjects.length)
			return;

		simulation.getCamera().setPositionAim(focusObjects[e.getKeyCode() - 49].getPosition());
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// ignore
	}

}

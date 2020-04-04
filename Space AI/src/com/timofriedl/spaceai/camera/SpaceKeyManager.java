package com.timofriedl.spaceai.camera;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.timofriedl.simulationbox.camera.Camera;
import com.timofriedl.simulationbox.object.SimulationObject;
import com.timofriedl.spaceai.SpaceSimulation;

/**
 * A key listener for different {@link SpaceSimulation} tasks.
 * 
 * [1-9]: focus space objects</br>
 * [G]: toggle grid</br>
 * [H]: toggle gravity field</br>
 * [+/-]: zoom max / min and reset rotation</br>
 * [SPACE]: pause simulation</br>
 * [F]: toggle auto cam</br>
 * [F1]: fast forward</br>
 * [R]: toggle render all rockets</br>
 * 
 * @author Timo Friedl
 */
public class SpaceKeyManager implements KeyListener {

	/**
	 * the reference to the main simulation instance
	 */
	private final SpaceSimulation simulation;

	/**
	 * the objects to focus
	 */
	private final SimulationObject[] focusObjects;

	/**
	 * Creates a new space key manager.
	 * 
	 * @param simulation   the reference to the main simulation instance
	 * @param focusObjects the objects to focus
	 */
	public SpaceKeyManager(SpaceSimulation simulation, SimulationObject... focusObjects) {
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
		final Camera cam = simulation.getCamera();

		if (e.getKeyCode() >= 49 && e.getKeyCode() < 49 + focusObjects.length)
			cam.setPositionAim(focusObjects[e.getKeyCode() - 49].getPosition());
		else if (e.getKeyCode() == KeyEvent.VK_G)
			simulation.getGrid()
					.setColor(simulation.getGrid().getColor().equals(Color.GRAY) ? new Color(0, 0, 0, 0) : Color.GRAY);
		else if (e.getKeyCode() == KeyEvent.VK_ADD) {
			cam.setZoomAim(Camera.MAX_ZOOM);
			cam.setRotationAim(0.0);
		} else if (e.getKeyChar() == KeyEvent.VK_MINUS) {
			simulation.getCamera().setZoomAim(Camera.MIN_ZOOM);
			cam.setRotationAim(0.0);
		} else if (e.getKeyCode() == KeyEvent.VK_H)
			simulation.getGravityField().setShow(!simulation.getGravityField().isShow());
		else if (e.getKeyCode() == KeyEvent.VK_SPACE)
			simulation.togglePause();
		else if (e.getKeyCode() == KeyEvent.VK_F)
			simulation.toggleAutoCam();
		else if (e.getKeyCode() == KeyEvent.VK_F1)
			simulation.toggleFastForward();
		else if (e.getKeyCode() == KeyEvent.VK_R)
			simulation.toggleRenderAllRockets();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// ignore
	}

}

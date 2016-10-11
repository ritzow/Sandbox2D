package main;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import audio.AudioManager;
import graphics.Background;
import graphics.GraphicsManager;
import input.Controls;
import input.controller.CameraController;
import input.controller.CursorController;
import input.controller.PlayerController;
import input.handler.KeyHandler;
import input.handler.WindowCloseHandler;
import util.ResourceManager;
import util.Synchronizer;
import world.World;
import world.WorldManager;
import world.block.DirtBlock;
import world.block.GrassBlock;
import world.block.RedBlock;
import world.entity.GenericEntity;

public final class GameManager implements Runnable, WindowCloseHandler, KeyHandler {
	private EventManager eventManager;
	private GraphicsManager graphicsManager;
	private AudioManager audioManager;
	private WorldManager worldManager;
	private ClientUpdateManager clientUpdateManager;
	public static CameraController cameraController;
	
	public GameManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	public void run() {
		Synchronizer.waitForSetup(eventManager);
		new Thread(graphicsManager = new GraphicsManager(eventManager.getDisplay()), "Graphics Manager").start();
		new Thread(audioManager = new AudioManager(), "Audio Manager").start();
		eventManager.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
		eventManager.getDisplay().getInputManager().getKeyHandlers().add(this);
		Synchronizer.waitForSetup(graphicsManager);
		setup();
		
		synchronized(eventManager) {
			eventManager.setReadyToDisplay();
			eventManager.notifyAll();
		}
	}
	
	public void setup() {
		World world = new World(200, 200, 0.015f);
		for(int i = 0; i < world.getBlocks().getHeight()/2; i++) {
			for(int j = 0; j < world.getBlocks().getWidth(); j++) {
				if(i == world.getBlocks().getHeight()/2 - 1) {
					world.getBlocks().set(j, i, new GrassBlock());
				} else if(Math.random() < 0.005f) {
					world.getBlocks().set(j, i, new RedBlock());
				} else {
					world.getBlocks().set(j, i, new DirtBlock());
				}
			}
		}
		
		GenericEntity player = new GenericEntity(ResourceManager.getModel("green_face"));
		player.getHitbox().setPriority(0.1f);
		player.position().setX(world.getBlocks().getWidth()/2);
		player.position().setY(world.getBlocks().getHeight());
		world.getEntities().add(player);
		
		PlayerController playerController = new PlayerController(eventManager.getDisplay().getInputManager(), player, world, 0.2f);
		CursorController cursorController = new CursorController(eventManager.getDisplay().getInputManager(), player, world, graphicsManager.getRenderer().getCamera());
		cameraController = new CameraController(eventManager.getDisplay().getInputManager(), graphicsManager.getRenderer().getCamera(), player, 0.005f);
		
		new Thread(worldManager = new WorldManager(world), "World Manager " + world.hashCode()).start();
		new Thread(clientUpdateManager = new ClientUpdateManager(), "Client Updater").start();
		
		clientUpdateManager.getUpdatables().add(playerController);
		clientUpdateManager.getUpdatables().add(cursorController);
		clientUpdateManager.getUpdatables().add(cameraController);
		
		graphicsManager.getRenderables().add(new Background(ResourceManager.getModel("hills_background")));
		graphicsManager.getRenderables().add(world);
	}
	
	private void exit() {
		clientUpdateManager.exit();
		Synchronizer.waitForExit(audioManager);
		Synchronizer.waitForExit(worldManager);
		Synchronizer.waitForExit(graphicsManager);
		eventManager.exit();
	}

	@Override
	public void windowClose() {
		exit();
	}

	@Override
	public void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
			exit();
		}
		
        else if(key == Controls.KEYBIND_FULLSCREEN && action == GLFW_PRESS) {
            eventManager.getDisplay().setFullscreen(!eventManager.getDisplay().getFullscreen());
        }
	}
}

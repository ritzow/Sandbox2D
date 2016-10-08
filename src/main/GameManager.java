package main;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import graphics.ui.DynamicLocation;
import graphics.ui.element.ElementManager;
import graphics.ui.element.button.BlockSwitcherButton;
import input.Controls;
import input.controller.CameraController;
import input.controller.CursorController;
import input.controller.PlayerController;
import input.handler.KeyHandler;
import input.handler.WindowCloseHandler;
import util.ResourceManager;
import util.Synchronizer;
import world.World;
import world.block.Block;
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
		World world = new World(200, 50, 0.015f);
		for(int i = 0; i < world.getBlocks().getHeight()/2; i++) {
			for(int j = 0; j < world.getBlocks().getWidth(); j++) {
				if(i == world.getBlocks().getHeight()/2 - 1) {
					world.getBlocks().set(j, i, new GrassBlock());
				} else if(Math.random() < 0.05f) {
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
		
//		ContainerEntity test = new ContainerEntity();
//		test.position().setX(player.position().getX());
//		test.position().setY(player.position().getY());
//		GenericEntity entity1 = new GenericEntity(ResourceManager.getModel("blue_square"));
//		entity1.position().setX(2);
//		entity1.position().setY(2);
//		GenericEntity entity2 = new GenericEntity(ResourceManager.getModel("red_square"));
//		test.getEntities().add(entity1);
//		test.getEntities().add(entity2);
//		world.getEntities().add(test);
		
		PlayerController playerController = new PlayerController(eventManager.getDisplay().getInputManager(), player, world, 0.2f);
		CursorController cursorController = new CursorController(eventManager.getDisplay().getInputManager(), player, world, graphicsManager.getRenderer().getCamera());
		cameraController = new CameraController(eventManager.getDisplay().getInputManager(), graphicsManager.getRenderer().getCamera(), player, 0.005f);
		
		new Thread(worldManager = new WorldManager(world), "World Manager " + world.hashCode()).start();
		new Thread(clientUpdateManager = new ClientUpdateManager(), "Client Updater").start();
		
		ElementManager elements = new ElementManager();
		eventManager.getDisplay().getInputManager().add(elements);
		
		elements.put(new BlockSwitcherButton(new Block[] {new DirtBlock(), new GrassBlock(), new RedBlock()}), new DynamicLocation(-1f, -1f, 0.25f, 0.25f));
		
		clientUpdateManager.getUpdatables().add(playerController);
		clientUpdateManager.getUpdatables().add(cursorController);
		clientUpdateManager.getUpdatables().add(cameraController);
		clientUpdateManager.getUpdatables().add(elements);
		
		graphicsManager.getRenderables().add(world);
		graphicsManager.getRenderables().add(elements);
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

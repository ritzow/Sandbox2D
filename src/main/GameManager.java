package main;

import audio.AudioSystem;
import graphics.Background;
import graphics.GraphicsManager;
import input.Controls;
import input.EventManager;
import input.InputManager;
import input.controller.CameraController;
import input.controller.InteractionController;
import input.controller.EntityController;
import input.handler.KeyHandler;
import input.handler.WindowCloseHandler;
import resource.Models;
import resource.Sounds;
import ui.ElementManager;
import util.Synchronizer;
import world.World;
import world.WorldManager;
import world.block.DirtBlock;
import world.block.GrassBlock;
import world.block.RedBlock;
import world.entity.Player;

public final class GameManager implements Runnable, WindowCloseHandler, KeyHandler {
	private EventManager eventManager;
	private GraphicsManager graphicsManager;
	private WorldManager worldManager;
	private ClientUpdateManager clientUpdateManager;
	
	public GameManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	public void run() {
		Synchronizer.waitForSetup(eventManager);
		eventManager.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
		eventManager.getDisplay().getInputManager().getKeyHandlers().add(this);
		new Thread(graphicsManager = new GraphicsManager(eventManager.getDisplay()), "Graphics Manager").start();
		
		AudioSystem.start();
		
		Synchronizer.waitForSetup(graphicsManager);
		
		World world = new World(500, 200, 0.015f);
		for(int column = 0; column < world.getForeground().getWidth(); column++) {
			double height = world.getForeground().getHeight()/10;
			height += (Math.sin(column * 0.1f) + 1) * (world.getForeground().getHeight() - height) * 0.05f;
			
			for(int row = 0; row < height; row++) {
				if(Math.random() < 0.005) {
					world.getForeground().set(column, row, new RedBlock());
				} else {
					world.getForeground().set(column, row, new DirtBlock());
				}
				world.getBackground().set(column, row, new DirtBlock());
			}
			world.getForeground().set(column, (int)height, new GrassBlock());
			world.getBackground().set(column, (int)height, new DirtBlock());
		}
		
		Player player = new Player();
		player.setPositionX(world.getForeground().getWidth()/2);
		player.setPositionY(world.getForeground().getHeight());
		world.getEntities().add(player);
		
		EntityController playerController = new EntityController(player, world, 0.2f);
		playerController.link(eventManager.getDisplay().getInputManager());
		
		InteractionController cursorController = new InteractionController(player, world, graphicsManager.getRenderer().getCamera(), 200);
		cursorController.link(eventManager.getDisplay().getInputManager());
		
		CameraController cameraController = new CameraController(graphicsManager.getRenderer().getCamera(), player, 0.005f);
		cameraController.link(eventManager.getDisplay().getInputManager());
		
		ElementManager manager = new ElementManager();
		eventManager.getDisplay().getInputManager().getCursorPosHandlers().add(manager);
		eventManager.getDisplay().getInputManager().getMouseButtonHandlers().add(manager);
		eventManager.getDisplay().getInputManager().getFramebufferSizeHandlers().add(manager);
		//manager.put(new BlockSwitcherButton(new Block[] {new DirtBlock(), new RedBlock(), new GrassBlock()}, cursorController), new DynamicLocation(1f, -1f, 0.25f, 0.25f));
		
		new Thread(worldManager = new WorldManager(world), "World Manager " + world.hashCode()).start();
		new Thread(clientUpdateManager = new ClientUpdateManager(), "Client Updater").start();
		
		clientUpdateManager.getUpdatables().add(playerController);
		clientUpdateManager.getUpdatables().add(cursorController);
		clientUpdateManager.getUpdatables().add(cameraController);
		clientUpdateManager.getUpdatables().add(manager);
		eventManager.getDisplay().getInputManager().getWindowFocusHandlers().add(clientUpdateManager);
		
		graphicsManager.getRenderables().add(new Background(Models.CLOUDS_BACKGROUND));
		graphicsManager.getRenderables().add(world);
		graphicsManager.getRenderables().add(manager);
		
		synchronized(eventManager) {
			eventManager.setReadyToDisplay();
			eventManager.notifyAll();
		}
	}
	
	private void exit() {
		clientUpdateManager.exit();
		Sounds.deleteAll();
		AudioSystem.stop();
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
		if(key == Controls.KEYBIND_QUIT && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
			exit();
		}
		
        else if(key == Controls.KEYBIND_FULLSCREEN && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            eventManager.getDisplay().setFullscreen(!eventManager.getDisplay().getFullscreen());
        }
	}

	@Override
	public void link(InputManager manager) {
		manager.getKeyHandlers().add(this);
	}

	@Override
	public void unlink(InputManager manager) {
		manager.getKeyHandlers().remove(this);
	}
}

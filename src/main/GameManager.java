package main;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import audio.AudioManager;
import graphics.Background;
import graphics.GraphicsManager;
import graphics.ui.DynamicLocation;
import graphics.ui.ElementManager;
import graphics.ui.element.Text;
import input.Controls;
import input.EventManager;
import input.controller.CameraController;
import input.controller.CursorController;
import input.controller.EntityController;
import input.handler.KeyHandler;
import input.handler.WindowCloseHandler;
import util.ModelManager;
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
	
	public GameManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	public void run() {
		Synchronizer.waitForSetup(eventManager);
		eventManager.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
		eventManager.getDisplay().getInputManager().getKeyHandlers().add(this);
		new Thread(graphicsManager = new GraphicsManager(eventManager.getDisplay()), "Graphics Manager").start();
		new Thread(audioManager = new AudioManager(), "Audio Manager").start();
		
		Synchronizer.waitForSetup(graphicsManager);
		
		System.out.print("Creating world... ");
		long time = System.currentTimeMillis();
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
		
		System.out.println("world creation took " + ((System.currentTimeMillis() - time)/1000.0f) + " seconds");
		
		GenericEntity player = new GenericEntity(ModelManager.GREEN_FACE);
		player.setMass(1.0f);
		player.setPositionX(world.getForeground().getWidth()/2);
		player.setPositionY(world.getForeground().getHeight());
		player.setFriction(0.02f);
		world.getEntities().add(player);
		
		EntityController playerController = new EntityController(player, world, 0.2f);
		playerController.link(eventManager.getDisplay().getInputManager());
		
		CursorController cursorController = new CursorController(player, world, graphicsManager.getRenderer().getCamera(), 200);
		cursorController.link(eventManager.getDisplay().getInputManager());
		
		CameraController cameraController = new CameraController(graphicsManager.getRenderer().getCamera(), player, 0.005f);
		cameraController.link(eventManager.getDisplay().getInputManager());
		
		ElementManager manager = new ElementManager();
		eventManager.getDisplay().getInputManager().getCursorPosHandlers().add(manager);
		eventManager.getDisplay().getInputManager().getMouseButtonHandlers().add(manager);
		eventManager.getDisplay().getInputManager().getFramebufferSizeHandlers().add(manager);
		//manager.put(new BlockSwitcherButton(new Block[] {new DirtBlock(), new RedBlock(), new GrassBlock()}, cursorController), new DynamicLocation(-1f, -1f, 0.25f, 0.25f));
		Text text = new Text("www.google.com", 4, 0.0f);
		manager.put(text, new DynamicLocation(-1,-1,0.3f,0.3f));
		
		new Thread(worldManager = new WorldManager(world), "World Manager " + world.hashCode()).start();
		new Thread(clientUpdateManager = new ClientUpdateManager(), "Client Updater").start();
		
		clientUpdateManager.getUpdatables().add(playerController);
		clientUpdateManager.getUpdatables().add(cursorController);
		clientUpdateManager.getUpdatables().add(cameraController);
		clientUpdateManager.getUpdatables().add(manager);
		
		graphicsManager.getRenderables().add(new Background(ModelManager.CLOUDS_BACKGROUND));
		graphicsManager.getRenderables().add(world);
		graphicsManager.getRenderables().add(manager);
		
		synchronized(eventManager) {
			eventManager.setReadyToDisplay();
			eventManager.notifyAll();
		}
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

package main;

import static util.Utility.Synchronizer.waitForExit;
import static util.Utility.Synchronizer.waitForSetup;

import audio.AudioSystem;
import graphics.Background;
import graphics.GraphicsManager;
import input.Controls;
import input.EventManager;
import input.InputManager;
import input.controller.CameraController;
import input.controller.EntityController;
import input.controller.InteractionController;
import input.controller.TrackingCameraController;
import input.handler.KeyHandler;
import input.handler.WindowCloseHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import network.client.Client;
import network.server.Server;
import resource.Fonts;
import resource.Models;
import resource.Sounds;
import ui.DynamicLocation;
import ui.ElementManager;
import ui.element.Text;
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
	
	public static CameraController camC;
	
	private volatile boolean exit;
	
	public GameManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	public void run() {
		
		if(GameEngine2D.PRINT_MEMORY_USAGE) {
			new Thread("Memory Usage Thread") {
				public void run() {
					try {
						while(!exit) {
							System.out.println("Memory Usage: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 0.000001) + " MB");
							Thread.sleep(1000);
						}
					} catch(InterruptedException e) {
						
					}
				}
			}.start();
		}
		
		waitForSetup(eventManager);
		eventManager.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
		eventManager.getDisplay().getInputManager().getKeyHandlers().add(this);
		new Thread(graphicsManager = new GraphicsManager(eventManager.getDisplay()), "Graphics Manager").start();
		
		AudioSystem.start();
		
		waitForSetup(graphicsManager);
		
		World world = new World(500, 200, 0.015f);
		for(int column = 0; column < world.getForeground().getWidth(); column++) {
			double height = world.getForeground().getHeight()/2;
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
		
		TrackingCameraController cameraController = new TrackingCameraController(graphicsManager.getRenderer().getCamera(), player, 0.005f, 0.05f, 0.6f);
		camC = cameraController;
		cameraController.link(eventManager.getDisplay().getInputManager());
		
		ElementManager manager = new ElementManager();
		manager.link(eventManager.getDisplay().getInputManager());
		
		manager.put(new Text("GameEngine2D", Fonts.DEFAULT_FONT, 5, 0), new DynamicLocation(-1, -1, 0.1f, 0.1f));
		
		clientUpdateManager = new ClientUpdateManager();
		
		clientUpdateManager.getUpdatables().add(playerController);
		clientUpdateManager.getUpdatables().add(cursorController);
		clientUpdateManager.getUpdatables().add(cameraController);
		clientUpdateManager.getUpdatables().add(manager);
		clientUpdateManager.link(eventManager.getDisplay().getInputManager());
		
		new Thread(clientUpdateManager, "Client Updater").start();
		new Thread(worldManager = new WorldManager(world), "World Manager " + world.hashCode()).start();
		
		graphicsManager.getRenderables().add(new Background(Models.CLOUDS_BACKGROUND));
		graphicsManager.getRenderables().add(world);
		graphicsManager.getRenderables().add(manager);
		
		try {
			Client client = new Client();
			Server server = new Server();
			new Thread(server, "Game Server").start();
			
			while(true) {
				try {
					if(client.connectToServer(new InetSocketAddress(InetAddress.getLocalHost(), 50000))) {
						System.out.println("connected to server!");
					} else {
						System.out.println("Couldn't connect to server");
					}
					
					break;
				} catch(SocketTimeoutException e) {
					System.err.println("Server didn't response, retrying");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		synchronized(eventManager) {
			eventManager.setReadyToDisplay();
			eventManager.notifyAll();
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.print("Program exiting...");
		 		clientUpdateManager.exit();
				worldManager.exit();
				
				Sounds.deleteAll();
				AudioSystem.stop();
				
				waitForExit(graphicsManager);
				waitForExit(eventManager); //wait for the eventManager to exit before the program clsoes
				System.out.println(" complete!");
			}
		});
		
		try {
			synchronized(this) {
				while(!exit) {
					this.wait();
				}
			}
		} catch(InterruptedException e) {
			
		} finally {
			System.exit(0);
		}
	}

	@Override
	public synchronized void windowClose() {
		this.exit = true;
		this.notifyAll();
	}

	@Override
	public synchronized void keyboardButton(int key, int scancode, int action, int mods) {
		if(key == Controls.KEYBIND_QUIT && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
			synchronized(this) {
				this.exit = true;
				this.notifyAll();
			}
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

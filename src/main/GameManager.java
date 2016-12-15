package main;

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
import java.net.SocketAddress;
import network.client.Client;
import network.server.Server;
import resource.Models;
import util.ByteUtil;
import util.Utility.Synchronizer;
import world.World;
import world.block.DirtBlock;
import world.block.GrassBlock;
import world.block.RedBlock;
import world.entity.ItemEntity;
import world.entity.Player;
import world.item.BlockItem;

/**
 * An instance of this class manages game startup and shutdown.
 * @author Solomon Ritzow
 *
 */
public final class GameManager implements Runnable, WindowCloseHandler, KeyHandler {
	private EventManager eventManager;
	private GraphicsManager graphicsManager;
	private ClientUpdateManager clientUpdateManager;
	
	private volatile boolean exit;
	
	public GameManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	@Override
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
		
		Synchronizer.waitForSetup(eventManager); //wait for the Display, and thus InputManager, to be created.
		eventManager.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
		eventManager.getDisplay().getInputManager().getKeyHandlers().add(this);
		
		//start the graphics manager, which will load all models into OpenGL and setup the OpenGL context.
		new Thread(graphicsManager = new GraphicsManager(eventManager.getDisplay()), "Graphics Manager").start();
		
		//wait for the graphics manager to finish setup.
		Synchronizer.waitForSetup(graphicsManager);
		
		//start OpenAL and load audio files.
		AudioSystem.start();
		
		/* TODO figure out how to implement "air"/refactor block system so that blocks like air (non-physical blocks)...
		 * won't cause weird bugs by refactoring based on the assumption that any given block wont be physical/have a model/collide/etc.
		 * Take context related stuff out of BlockGrid and move it somewhere else (world?) and make it so EntityController/InteractionController
		 * don't have to worry too much about how things work.
		 */
//		for(int column = 0; column < preWorld.getForeground().getWidth(); column++) {
//			for(int row = 0; row < preWorld.getForeground().getHeight(); row++) {
//				if(preWorld.getForeground().get(column, row) == null) {
//					preWorld.getForeground().set(column, row, new AirBlock());
//				}
//			}
//		}
		
		//test the serialization mechanism by serializing the entire world
		World world;
		try {
			File worldFile = new File("data/worlds/testWorld.dat");
			if(worldFile.length() == 0) {
				System.out.print("Creating world... ");
				World preWorld = new World(5000, 1000);
				for(int column = 0; column < preWorld.getForeground().getWidth(); column++) {
					double height = preWorld.getForeground().getHeight()/2;
					height += (Math.sin(column * 0.1f) + 1) * (preWorld.getForeground().getHeight() - height) * 0.05f;
					
					for(int row = 0; row < height; row++) {
						if(Math.random() < 0.007) {
							preWorld.getForeground().set(column, row, new RedBlock());
						} else {
							preWorld.getForeground().set(column, row, new DirtBlock());
						}
						preWorld.getBackground().set(column, row, new DirtBlock());
					}
					preWorld.getForeground().set(column, (int)height, new GrassBlock());
					preWorld.getBackground().set(column, (int)height, new DirtBlock());
				}
				System.out.println("world created.");
				
				System.out.print("Saving world... ");
				byte[] serialized = ByteUtil.serializeCompressed(preWorld);
				FileOutputStream out = new FileOutputStream(worldFile);
				out.write(serialized);
				out.getChannel().truncate(serialized.length);
				out.close();
				System.out.println("world saved to " + serialized.length + " bytes");
			}
			
			FileInputStream in = new FileInputStream(worldFile);
			byte[] fromFile = new byte[(int)worldFile.length()];
			in.read(fromFile);
			in.close();
			System.out.print("Loading world... ");
			world = (World)ByteUtil.deserializeCompressed(fromFile);
			System.out.println("done!");
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			world = null;
			System.exit(1);
		}
		
		//create the player's character
		Player player = new Player();
		player.setPositionX(world.getForeground().getWidth()/2);
		for(int i = 0; i < world.getForeground().getHeight(); i++) {
			if(world.getForeground().get(player.getPositionX(), i) == null) {
				player.setPositionY(i);
				break;
			}
		} world.add(player);
		
		//Create controllers for player input
		EntityController playerController = new EntityController(player, world, 0.2f);
		InteractionController cursorController = new InteractionController(player, world, graphicsManager.getRenderer().getCamera(), 200);
		CameraController cameraController = new TrackingCameraController(graphicsManager.getRenderer().getCamera(), player, 0.005f, 0.05f, 0.6f);
		
		//link controllers with the window's input manager
		playerController.link(eventManager.getDisplay().getInputManager());
		cursorController.link(eventManager.getDisplay().getInputManager());
		cameraController.link(eventManager.getDisplay().getInputManager());
		
		//create the client update manager and register controllers, then link with the input manager.
		clientUpdateManager = new ClientUpdateManager();
		clientUpdateManager.getUpdatables().add(playerController);
		clientUpdateManager.getUpdatables().add(cursorController);
		clientUpdateManager.getUpdatables().add(cameraController);
		clientUpdateManager.link(eventManager.getDisplay().getInputManager());
		
		//Add the background and world to the renderer
		graphicsManager.getRenderables().add(new Background(Models.CLOUDS_BACKGROUND));
		graphicsManager.getRenderables().add(world);
		
		//start the client updater
		new Thread(clientUpdateManager, "Client Updater").start();
		
		//display the window!
		eventManager.setReadyToDisplay();
		
		Client client;
		Server server;
		
		try {
			client = new Client();
			server = new Server(20);
			new Thread(server, "Game Server").start();
			Synchronizer.waitForSetup(server);
			server.startWorld(world);
			SocketAddress serverAddress = server.getSocketAddress();
			if(client.connectToServer(serverAddress, 1, 1000)) {
				System.out.println("Client connected to " + serverAddress);
				new Thread(client, "Game Client").start();
			} else {
				System.out.println("Client failed to connect to " + serverAddress);
			}
		} catch (IOException e) {
			e.printStackTrace();
			client = null;
			server = null;
		}
		
		final World lastWorld = server.getWorld();
		
		//create a shutdown hook so that when the program exits, everything is cleanly stopped
		Runtime.getRuntime().addShutdownHook(new Thread("Shutdown") {
			public void run() {
		 		clientUpdateManager.exit();
				AudioSystem.stop();
				Synchronizer.waitForExit(graphicsManager);
				Synchronizer.waitForExit(eventManager);
				
				try {
					System.out.print("Saving world... ");
					File file = new File("data/worlds/testWorld.dat");
					FileOutputStream out = new FileOutputStream(file);
					byte[] serialized = ByteUtil.serializeCompressed(lastWorld);
					out.write(serialized);
					out.getChannel().truncate(serialized.length);
					out.close();
					System.out.println("done!");
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		});
		
		try {
			synchronized(this) {
				while(!exit) {
					this.wait();
				}
			}
		} catch(InterruptedException e) {
			System.err.println("Game Manager was interrupted");
		} finally {
			if(client != null)
				client.exit();
			if(server != null) {
				server.exit();
			}
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
			this.exit = true;
			this.notifyAll();
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

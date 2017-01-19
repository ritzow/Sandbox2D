package ritzow.solomon.engine.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import ritzow.solomon.engine.audio.AudioSystem;
import ritzow.solomon.engine.graphics.Background;
import ritzow.solomon.engine.graphics.GraphicsManager;
import ritzow.solomon.engine.input.Controls;
import ritzow.solomon.engine.input.EventManager;
import ritzow.solomon.engine.input.InputManager;
import ritzow.solomon.engine.input.controller.Controller;
import ritzow.solomon.engine.input.controller.EntityController;
import ritzow.solomon.engine.input.controller.InteractionController;
import ritzow.solomon.engine.input.controller.TrackingCameraController;
import ritzow.solomon.engine.input.handler.KeyHandler;
import ritzow.solomon.engine.input.handler.WindowCloseHandler;
import ritzow.solomon.engine.network.Client;
import ritzow.solomon.engine.network.Server;
import ritzow.solomon.engine.resource.Models;
import ritzow.solomon.engine.util.ByteUtil;
import ritzow.solomon.engine.util.Utility.Synchronizer;
import ritzow.solomon.engine.world.World;
import ritzow.solomon.engine.world.block.DirtBlock;
import ritzow.solomon.engine.world.block.GrassBlock;
import ritzow.solomon.engine.world.block.RedBlock;
import ritzow.solomon.engine.world.entity.Player;

/**
 * An instance of this class manages game startup and shutdown, and is currently mostly used for testing
 * @author Solomon Ritzow
 *
 */
public final class GameManager implements Runnable, WindowCloseHandler, KeyHandler {
	private final EventManager eventManager;
	
	/** signals that the game should exit **/
	private volatile boolean exit;
	
	public GameManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	@Override
	public void run() {
		EventManager eventManager = this.eventManager;
		
		//start OpenAL and load audio files.
		AudioSystem.start();
		
		/* TODO figure out how to implement "air"/refactor block system so that blocks like air (non-physical blocks)...
		 * won't cause weird bugs by refactoring based on the assumption that any given block wont be physical/have a model/collide/etc.
		 * Take context related stuff out of BlockGrid and move it somewhere else (world?) and make it so EntityController/InteractionController
		 * don't have to worry too much about how things work.
		 */
		
		//the file I'm using to test world serialization
		File saveFile = new File("data/worlds/testWorld.dat");
		World world;
		
		if(saveFile.exists() && saveFile.length() > 0) {
			try(FileInputStream in = new FileInputStream(saveFile)) {
				byte[] data = new byte[(int)saveFile.length()];
				in.read(data);
				System.out.print("Loading world... ");
				world = (World)ByteUtil.deserialize(ByteUtil.decompress(data));
				System.out.println("done!");
			} catch(IOException | ReflectiveOperationException e) {
				e.printStackTrace();
				world = null;
			}
		} else {
			System.out.print("Creating world... ");
			world = new World(500, 200);
			for(int column = 0; column < world.getForeground().getWidth(); column++) {
				double height = world.getForeground().getHeight()/2;
				height += (Math.sin(column * 0.1f) + 1) * (world.getForeground().getHeight() - height) * 0.05f;
				for(int row = 0; row < height; row++) {
					if(Math.random() < 0.007) {
						world.getForeground().set(column, row, new RedBlock());
					} else {
						world.getForeground().set(column, row, new DirtBlock());
					}
					world.getBackground().set(column, row, new DirtBlock());
				}
				world.getForeground().set(column, (int)height, new GrassBlock());
				world.getBackground().set(column, (int)height, new DirtBlock());
			}
			System.out.println("world created.");
		}
		
		//wait for the Display, and thus InputManager, to be created, then link the game manager to the display so that the escape button and x button exit the game
		Synchronizer.waitForSetup(eventManager);
		eventManager.getDisplay().getInputManager().getWindowCloseHandlers().add(this);
		eventManager.getDisplay().getInputManager().getKeyHandlers().add(this);
		
		//start the graphics manager, which will load all models into OpenGL and setup the OpenGL context.
		GraphicsManager graphicsManager = new GraphicsManager(eventManager.getDisplay());
		new Thread(graphicsManager, "Graphics Manager").start();
		
		//wait for the graphics manager to finish setup.
		Synchronizer.waitForSetup(graphicsManager);
		
		//create the client update manager and link it with the window events
		ClientUpdateManager clientUpdateManager = new ClientUpdateManager();
		clientUpdateManager.link(eventManager.getDisplay().getInputManager());
		new Thread(clientUpdateManager, "Client Updater").start();
		
		//Add the background and world to the renderer
		graphicsManager.getRenderables().add(new Background(Models.CLOUDS));
		
		//perform a test of the client and server system (work in progress)
		Client client = null;
		Server server = null;
		
		try {
			client = new Client();
			server = new Server();
			new Thread(client, "Game Client").start();
			new Thread(server, "Game Server").start();
			server.startWorld(world);
			Synchronizer.waitForSetup(server);
			SocketAddress serverAddress = server.getSocketAddress();
			if(client.connectTo(serverAddress, 1000)) {
				System.out.println("Client connected to " + serverAddress);
				
				//create the player's character at the middle top of the world
				Player player = new Player();
				player.setPositionX(world.getForeground().getWidth()/2);
				for(int i = world.getForeground().getHeight() - 2; i > 1; i--) {
					if(world.getForeground().get(player.getPositionX(), i) == null && world.getForeground().get(player.getPositionX(), i + 1) == null &&
						world.getForeground().get(player.getPositionX(), i - 1) != null) {
						player.setPositionY(i);
						break;
					}
				}
				
				//create player controllers so the user can "play the game", for testing purposes
				Controller[] controllers = new Controller[] {
						new EntityController(player, world, 0.2f, false),
						new InteractionController(player, world, graphicsManager.getRenderer().getCamera(), 200, false),
						new TrackingCameraController(graphicsManager.getRenderer().getCamera(), player, 0.005f, 0.05f, 0.6f)
				};
				
				for(Controller c : controllers) {
					c.link(eventManager.getDisplay().getInputManager());
					clientUpdateManager.getUpdatables().add(c);
				}
				
				//TODO server needs to send world to client and client needs to set everything up.
				
			} else {
				System.out.println("Client failed to connect to " + serverAddress);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//display the window!
		eventManager.setReadyToDisplay();
		
		try {
			synchronized(this) {
				while(!exit) { //wait until the game should exit
					this.wait();
				}
			}
		} catch(InterruptedException e) {
			System.err.println("Game Manager was interrupted");
		} finally {
			clientUpdateManager.exit();
			client.exit();
			server.exit();
			AudioSystem.stop();
			
			//wait for the renderer to stop before closing the window
			Synchronizer.waitForExit(graphicsManager);
			eventManager.exit();
			
			//make sure the client and server are closed before saving the world
			Synchronizer.waitUntilFinished(client);
			Synchronizer.waitUntilFinished(server);
			
			//save the world to the file "data/worlds/testWorld.dat"
			try(FileOutputStream out = new FileOutputStream(saveFile)) {
				System.out.print("Saving world... ");
				byte[] serialized = ByteUtil.compress(ByteUtil.serialize(server.getWorld()));
				out.write(serialized);
				out.getChannel().truncate(serialized.length);
				System.out.println("world saved to " + serialized.length + " bytes");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//make sure the client update manager and event manager are closed before exiting the "game" thread
	 		Synchronizer.waitUntilFinished(clientUpdateManager);
			Synchronizer.waitUntilFinished(eventManager);
		}
	}
	
	public synchronized void exit() {
		exit = true;
		notifyAll();
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

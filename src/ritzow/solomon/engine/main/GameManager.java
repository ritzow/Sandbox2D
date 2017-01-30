package ritzow.solomon.engine.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import ritzow.solomon.engine.audio.Audio;
import ritzow.solomon.engine.graphics.Background;
import ritzow.solomon.engine.graphics.GraphicsManager;
import ritzow.solomon.engine.input.Controls;
import ritzow.solomon.engine.input.EventProcessor;
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
import ritzow.solomon.engine.util.ClientUpdater;
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
	
	/** the game's window/OS event processor **/
	private final EventProcessor eventManager;
	
	/** signals that the game should exit **/
	private volatile boolean exit;
	
	public GameManager(EventProcessor eventManager) {
		this.eventManager = eventManager;
	}
	
	@Override
	public void run() {
		try {
			Client client = new Client();
			Server server = new Server();
			new Thread(client, "Game Client").start();
			new Thread(server, "Game Server").start();
			Synchronizer.waitForSetup(server);
			
			//the save file to try to load the world from
			File saveFile = new File("data/worlds/testWorld.dat");
			
			//if a world exists, load it.
			if(saveFile.exists() && saveFile.length() > 1000) {
				try(FileInputStream in = new FileInputStream(saveFile)) {
					byte[] data = new byte[(int)saveFile.length()];
					in.read(data);
					System.out.print("Loading world... ");
					World world = (World)ByteUtil.deserialize(ByteUtil.decompress(data));
					System.out.println("done!");
					server.startWorld(world); //start the world on the server, which will send it to clients that connect
				} catch(IOException | ReflectiveOperationException e) {
					System.err.println("Couldn't load world from file: " + e.getLocalizedMessage() + ": " + e.getCause());
					e.printStackTrace();
					System.exit(1);
				}
			} else { //if no world can be loaded, create a new one.
				System.out.print("Creating world... ");
				World world = new World(500, 200);
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
				server.startWorld(world); //start the world on the server, which will send it to clients that connect
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
			
			//start OpenAL and load audio files (will run on this thread, not the best solution)
			Audio.start();
			
			//the server's address so the client can connect
			SocketAddress serverAddress = server.getSocketAddress();
			
			//connect to the server once the client side graphics and whatnot is finished being set up
			if(client.connectTo(serverAddress, 1000)) {
				System.out.println("Client connected to " + serverAddress);
				
				//wait for client to receive world from server
				client.waitForWorldStart();
				
				//create the client update manager and link it with the window events
				ClientUpdater clientUpdater = new ClientUpdater();
				clientUpdater.link(eventManager.getDisplay().getInputManager());
				
				//Add the background and world to the renderer
				graphicsManager.getRenderables().add(new Background(Models.CLOUDS));
				
				World world = client.getWorld();
				
				if(world == null) {
					System.err.println("world is null");
					System.exit(1);
				}
				
				graphicsManager.getRenderables().add(world);
				
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
				
				world.add(player);
				
				//create player controllers so the user can "play the game", for testing purposes
				Controller[] controllers = new Controller[] {
						new EntityController(player, world, 0.2f, false),
						new InteractionController(player, world, graphicsManager.getRenderer().getCamera(), 200, false),
						new TrackingCameraController(graphicsManager.getRenderer().getCamera(), player, 0.005f, 0.05f, 0.6f)
				};
				
				//add the controllers to the input manager so they receive input events
				for(Controller c : controllers) {
					c.link(eventManager.getDisplay().getInputManager());
					clientUpdater.getUpdatables().add(c);
				}
				
				//start the client updater
				new Thread(clientUpdater, "Client Updater").start();
				
				//display the window now that everything is set up.
				eventManager.setReadyToDisplay();
				
				//wait until the game should exit
				synchronized(this) {
					while(!exit) {
						try {
							this.wait();
						} catch(InterruptedException e) {
							e.printStackTrace(); //the game manager should never be interrupted
						}
					}
				}
				
				//start exiting game
				clientUpdater.exit();
				client.exit();
				server.exit();
				Audio.stop();
				
				//wait for the renderer to stop before closing the window
				Synchronizer.waitForExit(graphicsManager);
				eventManager.exit();
				
				//make sure the client and server are closed before saving the world
				Synchronizer.waitUntilFinished(client);
				Synchronizer.waitUntilFinished(server);
				
				//save the world to the file "data/worlds/testWorld.dat"
				try(FileOutputStream out = new FileOutputStream(saveFile)) {
					System.out.print("Saving world... ");
					byte[] serialized = ByteUtil.compress(ByteUtil.serialize(world));
					out.write(serialized);
					out.getChannel().truncate(serialized.length);
					System.out.println("world saved to " + serialized.length + " bytes");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//make sure the client updater and event manager are closed before exiting the "game" thread
		 		Synchronizer.waitUntilFinished(clientUpdater);
				Synchronizer.waitUntilFinished(eventManager);
				
			} else {
				System.out.println("Client failed to connect to " + serverAddress);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** can be called from any thread to exit the game **/
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
        	//this method should only be called from the event manager thread so it is safe to call OS related methods
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

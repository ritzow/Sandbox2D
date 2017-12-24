package ritzow.sandbox.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import ritzow.sandbox.client.audio.ClientAudioSystem;
import ritzow.sandbox.client.audio.Sound;
import ritzow.sandbox.client.audio.WAVEDecoder;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.input.ControlScheme;
import ritzow.sandbox.client.input.EventProcessor;
import ritzow.sandbox.client.input.InputManager;
import ritzow.sandbox.client.input.controller.CameraController;
import ritzow.sandbox.client.input.controller.Controller;
import ritzow.sandbox.client.input.controller.InteractionController;
import ritzow.sandbox.client.input.controller.PlayerController;
import ritzow.sandbox.client.input.controller.TrackingCameraController;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.util.RepeatUpdater;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;

public final class StartClient {
	public static void main(String... args) throws SocketException, UnknownHostException {
		InetSocketAddress serverAddress = 
				new InetSocketAddress(args.length > 0 ? args[0] : InetAddress.getLocalHost().getHostAddress(), 50000);
		Client client = new Client(new InetSocketAddress(0), serverAddress); //wildcard address, and any port
		System.out.print("Connecting to " + serverAddress + "... ");
		
		if(client.connect()) {
			System.out.println("connected!");
			EventProcessor eventProcessor = new EventProcessor();
			new Thread(new Runnable () { //run code on separate thread
				private boolean exit;
				
				private void exit() {
					synchronized(this) {
						exit = true;
						notifyAll();
					}
				}
				
				private void initGraphics(RenderManager renderManager, World world, CameraController cameraGrip) {
					try {
						int indices = GraphicsUtility.uploadIndexData(0, 1, 2, 0, 2, 3);
						
						int positions = GraphicsUtility.uploadVertexData(
								-0.5f,	 0.5f,
								-0.5f,	-0.5f,
								0.5f,	-0.5f,
								0.5f,	 0.5f
						);
						
						int textureCoordinates = GraphicsUtility.uploadVertexData(
								0f, 1f,
								0f, 0f,
								1f, 0f,
								1f, 1f	
						);
						
						GraphicsUtility.checkErrors();
						
						ModelRenderProgram modelProgram = new ModelRenderProgram(
								new Shader(new FileInputStream("resources/shaders/modelVertexShader"), ShaderType.VERTEX),
								new Shader(new FileInputStream("resources/shaders/modelFragmentShader"), ShaderType.FRAGMENT), 
								cameraGrip.getCamera()
								);
						
						modelProgram.register(Models.GRASS_BLOCK,	new Model(6, indices, positions, textureCoordinates, Textures.GRASS.id));
						modelProgram.register(Models.DIRT_BLOCK,	new Model(6, indices, positions, textureCoordinates, Textures.DIRT.id));
						modelProgram.register(Models.GREEN_FACE,	new Model(6, indices, positions, textureCoordinates, Textures.GREEN_FACE.id));
						modelProgram.register(Models.RED_SQUARE,	new Model(6, indices, positions, textureCoordinates, Textures.RED_SQUARE.id));
						
						LightRenderProgram lightProgram = new LightRenderProgram(
								new Shader(new FileInputStream("resources/shaders/lightVertexShader"), ShaderType.VERTEX),
								new Shader(new FileInputStream("resources/shaders/lightFragmentShader"), ShaderType.FRAGMENT),
								cameraGrip.getCamera()
								);
						renderManager.getRenderers().add(new ClientGameRenderer(modelProgram, lightProgram, world));
					} catch (IOException | OpenGLException e) {
						throw new RuntimeException(e);
					}
				}
				
				public void run() {
					eventProcessor.waitForSetup();
					
					//wait for the client to receive the world and return it
					World world = client.getWorld();
					System.out.println("Received world from server.");
					
					ClientAudioSystem audio = new ClientAudioSystem();
					
					Map<String, Sound> soundFiles = Map.ofEntries(
						Map.entry("dig.wav", Sound.BLOCK_BREAK),
						Map.entry("place.wav", Sound.BLOCK_PLACE),
						Map.entry("pop.wav", Sound.POP),
						Map.entry("throw.wav", Sound.THROW),
						Map.entry("snap.wav", Sound.SNAP)
					);
					
					for(File f : new File("resources/assets/audio").listFiles(f -> f.isFile())) {
						try(FileInputStream input = new FileInputStream(f)) {
							audio.registerSound(soundFiles.get(f.getName()).code(), WAVEDecoder.decode(input));	
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
					
					audio.setVolume(1.0f);
					world.setAudioSystem(audio);
					
					//mute audio in background
					eventProcessor.getDisplay().getInputManager().getWindowFocusHandlers()
						.add(focused -> audio.setVolume(focused ? 1.0f : 0.0f));
					
					ClientPlayerEntity player = client.getPlayer();
					System.out.println("Received player from server.");
					
					CameraController cameraGrip =
							new TrackingCameraController(new Camera(0, 0, 1), audio, player, 0.005f, 0.05f, 0.6f);
					
					//create and link player controllers so the user canControllergame
					Collection<Controller> controllers = List.of(
							new PlayerController(client),
							new InteractionController(client, cameraGrip.getCamera(), 200, 300, 5),
							cameraGrip
					);
					
					controllers.forEach(eventProcessor.getDisplay().getInputManager()::add);
					
					RenderManager renderManager = eventProcessor.getDisplay().getRenderManager();
					RepeatUpdater gameUpdater = new RepeatUpdater(() -> {
						renderManager.initialize();
						initGraphics(renderManager, world, cameraGrip);
					}, renderManager::shutdown);
					
					gameUpdater.getRepeatTasks().add(client.onReceiveMessageTask());
					gameUpdater.getRepeatTasks().addAll(controllers);
					gameUpdater.getRepeatTasks().add(new Runnable() {
						private long previousTime = System.nanoTime();
						
						public void run() { //TODO pause world updates when not focused
							previousTime = Utility.updateWorld(world, previousTime, SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
						}
					});
					gameUpdater.getRepeatTasks().add(renderManager);
					gameUpdater.getRepeatTasks().add(() -> Utility.sleep(1));
					gameUpdater.start("Game updater");
					gameUpdater.waitForSetup();
					
					InputManager input = eventProcessor.getDisplay().getInputManager();
					input.getKeyHandlers().add((key, scancode, action, mods) -> {
						if(key == ControlScheme.KEYBIND_QUIT && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
							exit();
						} else if(key == ControlScheme.KEYBIND_FULLSCREEN && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				            eventProcessor.getDisplay().toggleFullscreen();
				        }
					});

					input.getWindowCloseHandlers().add(this::exit);
					client.onDisconnect(this::exit);
					
					//display the window now that everything is set up.
					eventProcessor.setReadyToDisplay();
					System.out.println("Rendering started.");
					
					//wait until the window is closed or escape key is pressed
					Utility.waitOnCondition(this, () -> exit);
					
					System.out.print("Exiting... ");
					eventProcessor.getDisplay().setVisible(false);
					gameUpdater.stop();
					eventProcessor.waitForExit();
					if(client.isConnected())
						client.disconnect();
					audio.close();
					ClientAudioSystem.shutdown();
					System.out.println("done!");
				}
			}, "Client Manager").start();
			eventProcessor.run(); //run the event processor on this thread
		} else {
			System.out.println("failed to connect.");
		}
	}
}
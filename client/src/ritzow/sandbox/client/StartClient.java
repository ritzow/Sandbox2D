package ritzow.sandbox.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import ritzow.sandbox.client.audio.*;
import ritzow.sandbox.client.core.ClientGameUpdater;
import ritzow.sandbox.client.graphics.Camera;
import ritzow.sandbox.client.graphics.Shader.ShaderType;
import ritzow.sandbox.client.graphics.*;
import ritzow.sandbox.client.input.*;
import ritzow.sandbox.client.input.controller.*;
import ritzow.sandbox.client.util.RunnableGroup;
import ritzow.sandbox.client.world.entity.ClientPlayerEntity;
import ritzow.sandbox.world.World;

public final class StartClient {
	public static void main(String... args) throws SocketException, UnknownHostException {
		InetSocketAddress serverAddress = 
				new InetSocketAddress(args.length > 0 ? args[0] : InetAddress.getLocalHost().getHostAddress(), 50000);
		Client client = new Client(new InetSocketAddress(0), serverAddress); //wildcard address, and any port
		System.out.print("Connecting to " + serverAddress + "... ");
		if(client.connect(1000)) {
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
				
				public void run() {
					eventProcessor.waitForSetup();
					
					//wait for the client to receive the world and return it
					World world = client.getWorld();
					System.out.println("world received");
					
					ClientAudioSystem audio = new ClientAudioSystem();
					
					Map<String, Integer> soundFiles = Map.ofEntries(
						Map.entry("dig.wav", Sounds.BLOCK_BREAK),
						Map.entry("place.wav", Sounds.BLOCK_PLACE),
						Map.entry("pop.wav", Sounds.POP),
						Map.entry("throw.wav", Sounds.THROW),
						Map.entry("snap.wav", Sounds.SNAP)
					);
					
					for(File f : new File("resources/assets/audio").listFiles(f -> f.isFile())) {
						try(FileInputStream input = new FileInputStream(f)) {
							WAVEDecoder decoder = new WAVEDecoder(input);
							decoder.decode();
							audio.registerSound(soundFiles.get(f.getName()), decoder);	
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					audio.setVolume(1.0f);
					world.setAudioSystem(audio);
					
					//only play audio when the windows is active 
					//TODO completely disable audio instead of just muting
					eventProcessor.getDisplay().getInputManager().getWindowFocusHandlers().add(focused -> {
						if(focused) {
							audio.setVolume(1.0f);
						} else {
							audio.setVolume(0);
						}
					});
					
					System.out.println("Audio system setup complete");
					
					ClientPlayerEntity player = client.getPlayer();
					System.out.println("Player downloaded");
					
					CameraController cameraGrip =
							new TrackingCameraController(new Camera(0, 0, 1), audio, player, 0.005f, 0.05f, 0.6f);
					
					//create and link player controllers so the user canControllergame
					Collection<Controller> controllers = Arrays.asList(
							new PlayerController(client),
							new InteractionController(client, cameraGrip.getCamera(), 200, 300, 5),
							cameraGrip
					);
					
					controllers.forEach(controller -> {
						controller.link(eventProcessor.getDisplay().getInputManager());
					});
					
					Collection<Runnable> runnables = new ArrayList<Runnable>();
					runnables.addAll(controllers);
					
					RenderManager renderManager = eventProcessor.getDisplay().getRenderManager();
					renderManager.submitRenderTask(graphics -> {
						try {
							ModelRenderProgram modelProgram = new ModelRenderProgram(
									new Shader(new FileInputStream("resources/shaders/modelVertexShader"), ShaderType.VERTEX),
									new Shader(new FileInputStream("resources/shaders/modelFragmentShader"), ShaderType.FRAGMENT), 
									cameraGrip.getCamera()
							);
							
							LightRenderProgram lightProgram = new LightRenderProgram(
									new Shader(new FileInputStream("resources/shaders/lightVertexShader"), ShaderType.VERTEX),
									new Shader(new FileInputStream("resources/shaders/lightFragmentShader"), ShaderType.FRAGMENT),
									cameraGrip.getCamera()
							);
							
							graphics.getRenderers().add(new ClientGameRenderer(runnables, modelProgram, lightProgram, world));
							
							System.out.println("Renderer started");
						} catch (IOException | OpenGLException e) {
							throw new RuntimeException(e);
						}
					});
					
					ClientGameUpdater gameUpdater = new ClientGameUpdater();
					gameUpdater.start();
					gameUpdater.addRepeatedTask(() -> { //add wait task so the updater doesn't increase CPU usage
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					});
					
					gameUpdater.addRepeatedTask(new RunnableGroup(runnables));
					
					//TODO migrate rendering and world updating to Runnable subclasses and integrate with game updater
					
					renderManager.start();
					renderManager.waitForSetup();
					System.out.println("Render manager setup complete");
					
					InputManager input = eventProcessor.getDisplay().getInputManager();
					input.getKeyHandlers().add((key, scancode, action, mods) -> {
						if(key == Controls.KEYBIND_QUIT && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
							exit();
						} else if(key == Controls.KEYBIND_FULLSCREEN && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
				            eventProcessor.getDisplay().toggleFullscreen();
				        }
					});

					input.getWindowCloseHandlers().add(() -> {
						exit();
					});
					
					//display the window now that everything is set up.
					eventProcessor.setReadyToDisplay();
					System.out.println("Window displayed");
					
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
					
					//TODO need to fix ordering of these things
					System.out.print("Exiting... ");
					eventProcessor.getDisplay().setVisible(false);
					gameUpdater.stop();
					renderManager.waitForExit();
					eventProcessor.waitForExit();
					client.disconnect(true);
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
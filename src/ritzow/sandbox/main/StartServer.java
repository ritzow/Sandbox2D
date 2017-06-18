package ritzow.sandbox.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.server.Server;
import ritzow.sandbox.server.ServerAudioSystem;
import ritzow.sandbox.server.ServerWorld;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.block.RedBlock;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartServer {
	public static void main(String... args) throws SocketException, UnknownHostException {
		Thread.currentThread().setName("Server Setup");
		Server server = new Server(new InetSocketAddress(50000));
		
		//the save file to try to load the world from
		final File saveFile = new File("data/worlds/testWorld.dat");
		
		ServerAudioSystem audio = new ServerAudioSystem();
		ServerWorld world = saveFile.exists() ? loadWorld(saveFile) : generateWorld(audio, server);
		world.setAudioSystem(audio);
		
		server.start();
		server.waitUntilStarted();
		server.startWorld(world);
		
		System.out.println("Startup Complete.");
		System.out.println("Type 'exit' to stop server or anything else to broadcast a message:");
		
		try(Scanner scanner = new Scanner(System.in)) {
			String line;
			while(!(line = scanner.nextLine()).equalsIgnoreCase("exit")) {
				server.broadcastMessage(line);
				System.out.println("Sent message '" + line + "' to " + server.clientCount() + " connected client(s).");
			}
		}
		
		server.stop();
		server.waitUntilStopped();
		
		System.out.println("Shutdown Complete.");
		
		try {
			if(!saveFile.exists()) {
				saveFile.createNewFile();
			}
			
			try(FileOutputStream out = new FileOutputStream(saveFile)) {
				System.out.print("Saving world... ");
				byte[] serialized = ByteUtil.compress(world.getBytes(e -> {
					return !(e instanceof PlayerEntity); //dont save player entites when saving world to file
				}));
				out.write(serialized);
				out.getChannel().truncate(serialized.length);
				System.out.println("world saved to " + serialized.length + " bytes.");
			} catch (IOException e) {
				System.out.println("Error while saving world to file: " + e.getLocalizedMessage());
			}
		} catch (IOException e) {
			System.out.println("Could not find or create file to save world: " + e.getLocalizedMessage());
		}
	}
	
	public static ServerWorld loadWorld(File file) {
		try(FileInputStream in = new FileInputStream(file)) {
			byte[] data = new byte[(int)file.length()];
			in.read(data);
			return new ServerWorld(ByteUtil.decompress(data));
		} catch(IOException | ReflectiveOperationException e) {
			System.out.println("Error loading world from file");
			return null;
		}
	}
	
	public static ServerWorld generateWorld(AudioSystem audio, Server server) {
		ServerWorld world = new ServerWorld(audio, server, 300, 200, 0.016f);
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
		world.setServer(server);
		return world;
	}
}

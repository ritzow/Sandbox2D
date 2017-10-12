package ritzow.sandbox.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import ritzow.sandbox.audio.AudioSystem;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.Deserializer;
import ritzow.sandbox.server.Server.ClientState;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartServer {
	public static void main(String... args) throws SocketException, UnknownHostException {
		Thread.currentThread().setName("Server Setup");
		Server server = new Server(new InetSocketAddress(50000));
		
		//the save file to try to load the world from
		final File saveFile = new File("data/worlds/testWorld.dat");
		
		ServerAudioSystem audio = new ServerAudioSystem();
		World world = saveFile.exists() ? 
				loadWorld(saveFile, audio, SerializationProvider.getProvider()) : generateWorld(100, 100, audio, server);
		//world.add(new ItemEntity<Item>(world.nextEntityID(), new BlockItem(new DirtBlock()),50,120));
				server.setWorld(world);
		server.start();
		
		System.out.println("Startup Complete.");
		System.out.println("Type 'exit' to stop server or 'list' to list connected clients");
		try(Scanner scanner = new Scanner(System.in)) {
			boolean exit = false;
			String next;
			while(!exit) {
				switch(next = scanner.nextLine()) {
				case "exit":
				case "quit":
				case "stop":
					exit = true;
					break;
				case "list":
					System.out.println("Connected clients:");
					for(ClientState client : server.listClients()) {
						System.out.print("\t");
						System.out.println(client);
					}
					break;
				default:
					server.broadcastMessage(next);
					System.out.println("Sent message '" + next + "' to " + server.clientCount() + " connected client(s).");
				}
			}
		}
		
		server.stop();
		
		try {
			if(!saveFile.exists()) {
				saveFile.createNewFile();
			}
			
			try(FileOutputStream out = new FileOutputStream(saveFile)) {
				System.out.print("Saving world... ");
				world.removeIf(e -> e instanceof PlayerEntity); //remove players before saving to file
				byte[] serialized = ByteUtil.compress(SerializationProvider.getProvider().serialize(world));
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
	
	public static World loadWorld(File file, AudioSystem audio, Deserializer des) {
		try(FileInputStream in = new FileInputStream(file)) {
			byte[] data = new byte[(int)file.length()];
			in.read(data);
			World world = des.deserialize(ByteUtil.decompress(data));
			world.setAudioSystem(audio);
			return world;
		} catch(IOException e) {
			System.out.println("Error loading world from file " + e);
			return null;
		}
	}
	
	public static World generateWorld(int width, int height, AudioSystem audio, Server server) {
		World world = new World(audio, width, height, 0.016f);
		for(int column = 0; column < world.getForeground().getWidth(); column++) {
			double halfheight = world.getForeground().getHeight()/2;
			halfheight += (Math.sin(column * 0.1f) + 1) * (world.getForeground().getHeight() - halfheight) * 0.05f;
			for(int row = 0; row < halfheight; row++) {
				world.getForeground().set(column, row, new DirtBlock());
				world.getBackground().set(column, row, new DirtBlock());
			}
			world.getForeground().set(column, (int)halfheight, new GrassBlock());
			world.getBackground().set(column, (int)halfheight, new DirtBlock());
		}
		return world;
	}
}

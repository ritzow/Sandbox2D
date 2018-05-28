package ritzow.sandbox.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import ritzow.sandbox.data.ByteUtil;
import ritzow.sandbox.data.Deserializer;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.Server.ClientState;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartServer {
	private static final boolean SAVE_WORLD = true;
	
	public static void main(String... args) {
		Thread.currentThread().setName("Console Input Main");
		
		try {
			Server server = Server.open(new InetSocketAddress(Protocol.DEFAULT_SERVER_UDP_PORT));

			//the save file to try to load the world from
			Path saveFile = Paths.get(args.length > 0 ? args[0] : "data/worlds/world.dat");
			
			//if a save file exists, load it, otherwise generate a world
			World world = Files.exists(saveFile) ? 
					loadWorld(saveFile, SerializationProvider.getProvider()) : generateWorld(100, 100);
			server.start(world);
			
			//read user input commands
			System.out.println("Startup Complete.");
			System.out.println("Type 'exit' to stop server or 'list' to list connected clients");
			try(Scanner scanner = new Scanner(System.in)) {
				String next;
				reader: while(true) {
					switch(next = scanner.nextLine()) {
					case "exit":
					case "quit":
					case "stop":
						break reader;
					case "list":
						if(server.getConnectedClients() == 0) {
							System.out.println("No connected clients.");
						} else {
							System.out.println("Connected clients:");
							for(ClientState client : server.listClients()) {
								System.out.print("\t - ");
								System.out.println(client);
							}
						}
						break;
					case "disconnect":
						server.disconnectAll("server manual disconnect");
						break;
					default:
						server.broadcastConsoleMessage(next);
						System.out.println("Sent message '" + next + "' to " + 
						server.getConnectedClients() + " connected client(s).");
					}
				}
			}
			
			server.stop();
			
			//save world to file if enabled
			if(SAVE_WORLD) {
				try {
					if(!Files.exists(saveFile))
						Files.createFile(saveFile);
					saveWorld(world, saveFile);
				} catch (IOException e) {
					System.out.println("Could not find or create file to save world: " + e.getLocalizedMessage());
				}
			} else {
				System.out.println("Server stopped.");
			}
		} catch(BindException e) {
			System.out.println("Could not start server: '" + e.getMessage() + "'");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveWorld(World world, Path file) {
		try {
			System.out.print("Saving world... ");
			world.removeIf(e -> e instanceof PlayerEntity); //remove players before saving to file
			byte[] serialized = ByteUtil.compress(SerializationProvider.getProvider().serialize(world));
			Files.write(file, serialized);
			System.out.println("world saved to " + serialized.length + " bytes.");
		} catch (IOException e) {
			System.out.println("Error while saving world to file: " + e.getMessage());
		}
	}
	
	public static World loadWorld(Path file, Deserializer des) throws IOException {
		return des.deserialize(ByteUtil.decompress(Files.readAllBytes(file)));
	}
	
	public static World generateWorld(int width, int height) {
		World world = new World(width, height, 0.016f);
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

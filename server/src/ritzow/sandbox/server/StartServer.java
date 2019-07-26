package ritzow.sandbox.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.network.Server;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartServer {
	private static Server server;
	private static boolean save = true;
	private static final Path saveFile = Path.of("world.dat");

	private static final int WORLD_WIDTH = 200, WORLD_BASE_HEIGHT = 20, TERRAIN_AMPLITUDE = 20, SKY_HEIGHT = 20;
	private static final float WORLD_FREQUENCY = 0.15f;

	public static void main(String[] args) throws SocketException, IOException {
		Thread.currentThread().setName("Server Main");
		if(args.length > 0) {
			run(switch(args[0]) {
				case "local" -> new InetSocketAddress(
						NetworkUtility.getLoopbackAddress(), Protocol.DEFAULT_SERVER_PORT_UDP);
				case "public" -> new InetSocketAddress(
						NetworkUtility.getPrimaryAddress(true), Protocol.DEFAULT_SERVER_PORT_UDP);
				default -> throw new UnsupportedOperationException("unknown address type");
			});
		} else {
			run(new InetSocketAddress(
					NetworkUtility.getLoopbackAddress(), Protocol.DEFAULT_SERVER_PORT_UDP));
		}
	}

	public static void run(InetSocketAddress bindAddress) throws IOException {
		try {
			server = Server.start(bindAddress);
			System.out.println("Started server on " + NetworkUtility.formatAddress(server.getAddress()) + ".");
			long time = System.nanoTime();
			System.out.print((Files.exists(saveFile) ? "Loading" : "Generating") + " world... ");
			World world = Files.exists(saveFile) ? loadWorld(saveFile) : generateWorld();
			System.out.println("took " + Utility.formatTime(Utility.nanosSince(time)) + ".");
			CommandParser parser = new CommandParser();
			registerCommands(parser);
			new Thread(parser::run, "Command Parser").start();
			server.setCurrentWorld(world);
			try {
				while(server.isOpen()) {
					parser.update();
					server.update();
					Utility.sleepMillis(1);
				}
			} catch(IOException e) {
				e.printStackTrace();
			} finally {
				if(save) {
					saveWorld(world, saveFile);
				} else {
					System.out.println("Server stopped without saving to file.");
				}
				
				System.exit(0); //kill the console thread
			}
		} catch(BindException e) {
			System.out.println("Couldn't start server on address " + NetworkUtility.formatAddress(bindAddress));
		}
	}

	public static World loadWorld(Path file) throws IOException {
		return SerializationProvider.getProvider().deserialize(Bytes.decompress(Files.readAllBytes(file)));
	}

	private static World generateWorld() {
		World world = new World(WORLD_WIDTH, WORLD_BASE_HEIGHT + TERRAIN_AMPLITUDE + SKY_HEIGHT, 0.016f);
		for(int column = 0; column < WORLD_WIDTH; ++column) {
			int amplitude = WORLD_BASE_HEIGHT + TERRAIN_AMPLITUDE/2
					+ Math.round(TERRAIN_AMPLITUDE/2 * (float)Math.sin(column * WORLD_FREQUENCY)) - 1;
			for(int row = 0; row < amplitude; ++row) {
				world.getForeground().set(column, row, new DirtBlock());
				world.getBackground().set(column, row, new DirtBlock());
			}
			world.getForeground().set(column, amplitude, new GrassBlock());
			world.getBackground().set(column, amplitude, new DirtBlock());
		}
		return world;
	}

	private static void saveWorld(World world, Path file) {
		try {
			System.out.print("Saving world... ");
			world.removeIf(e -> e instanceof PlayerEntity); //remove players before saving to file
			byte[] serialized = Bytes.compress(SerializationProvider.getProvider().serialize(world));
			Files.write(file, serialized);
			System.out.println("world saved to " + Utility.formatSize(serialized.length) + ".");
		} catch (IOException e) {
			System.out.println("Error while saving world to file '" + saveFile + "':" 
					+ e.getClass().getTypeName() + ":" + e.getMessage());
		}
	}

	private static void registerCommands(CommandParser runner) {
		runner.register("stop", 		StartServer::stopCommand);
		runner.register("abort", 		StartServer::abortCommand);
		runner.register("list", 		StartServer::listCommmand);
		runner.register("say", 			StartServer::sayCommand);
		runner.register("reset", 		StartServer::resetCommand);
		runner.register("debug",		StartServer::debugCommand);
		runner.register("kill", 		StartServer::killCommand);
		runner.register("printworld", 	StartServer::printworldCommand);
		runner.register("killitems", 	StartServer::killItemsCommand);
	}
	
	private static void killItemsCommand(String args) {
		server.world().removeIf(e -> e instanceof ItemEntity);
	}
	
	private static void printworldCommand(String args) {
		System.out.println(server.world());
	}
	
	private static void debugCommand(String args) {
		server.printDebug(System.out);
	}

	private static void resetCommand(String args) {
		try {
			if(Files.deleteIfExists(saveFile))
				System.out.println("Deleted saved world.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		abortCommand(args);
	}

	private static void stopCommand(String args) {
		server.startShutdown();
	}
	
	private static void abortCommand(String args) {
		save = false;
		server.startShutdown();
	}
	
	private static void killCommand(String args) {
		save = false;
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void sayCommand(String args) {
		server.broadcastConsoleMessage(args);
		System.out.println("Message '" + args + "' sent to " + server.getClientCount() + " client(s).");
	}

	private static void listCommmand(String args) {
		if(server.getClientCount() == 0) {
			System.out.println("No connected clients.");
		} else {
			System.out.println("Connected clients:");
			for(var string : server.listClients()) {
				System.out.print("    - ");
				System.out.println(string);
			}
		}
	}
}

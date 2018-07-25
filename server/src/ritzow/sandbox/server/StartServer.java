package ritzow.sandbox.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.network.GameServer;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartServer {
	private static GameServer server;
	private static volatile boolean save = true;
	private static final Path saveFile = Path.of("data/worlds/world.dat");
	private static final Map<String, Consumer<String>> commands = new HashMap<>();
	private static final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
	
	public static void main(String[] args) throws IOException {
		try {
			Thread.currentThread().setName("Server Main");
			server = GameServer.start(Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_UDP_PORT));
			System.out.println("Started server on " + Utility.formatAddress(server.getAddress()) + ".");
			System.out.print("Loading world... ");
			long time = System.nanoTime();
			World world = getWorld();
			System.out.println("took " + Utility.formatTime(Utility.nanosSince(time)) + ".");
			server.setCurrentWorld(world);
			
			new Thread(StartServer::runCommandParser, "Command Parser").start();
			
			long lastWorldUpdateTime = System.nanoTime();
			while(server.isOpen()) {
				server.updateServer();
				lastWorldUpdateTime = Utility.updateWorld(world, lastWorldUpdateTime, 
						Protocol.MAX_UPDATE_TIMESTEP, Protocol.TIME_SCALE_NANOSECONDS);
				server.updateClients();
				while(!commandQueue.isEmpty()) {
					commandQueue.remove().run();
				}
				Utility.sleep(1);
			}

			if(save) {
				saveWorld(world, saveFile);
			} else {
				System.out.println("Server stopped without saving to file.");
			}
			
			System.exit(0);
		} catch(BindException e) {
			System.out.println("Could not start server: '" + e.getMessage() + "'");
		}
	
	}
	
	public static World getWorld() throws IOException {
		return Files.exists(saveFile) ? loadWorld(saveFile) : generateWorld(100, 100);
	}
	
	public static World loadWorld(Path file) throws IOException {
		return SerializationProvider.getProvider().deserialize(Bytes.decompress(Files.readAllBytes(file)));
	}
	
	private static void saveWorld(World world, Path file) {
		try {
			System.out.print("Saving world... ");
			world.removeIf(e -> e instanceof PlayerEntity); //remove players before saving to file
			byte[] serialized = Bytes.compress(SerializationProvider.getProvider().serialize(world));
			Files.write(file, serialized);
			System.out.println("world saved to " + Utility.formatSize(serialized.length) + ".");
		} catch (IOException e) {
			System.out.println("Error while saving world to file '" + saveFile + "':" + e.getClass().getTypeName() + ":" + e.getMessage());
		}
	}
	
	private static World generateWorld(int width, int height) {
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
	
	private static void registerCommands() {
		register("stop", 	StartServer::stopCommand);
		register("abort", 	StartServer::abortCommand);
		register("list", 	StartServer::listCommmand);
		register("say", 	StartServer::sayCommand);
		register("reset", 	StartServer::resetCommand);
		register("debug",	StartServer::debugCommand);
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
	
	private static void abortCommand(String args) {
		save = false;
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void listCommmand(String args) {
		if(server.getClientCount() == 0) {
			System.out.println("No connected clients.");
		} else {
			System.out.println("Connected clients:");
			for(var address : server.listClients()) {
				System.out.print("    - ");
				System.out.println(Utility.formatAddress(address));
			}
		}
	}
	
	private static void unknownCommand(String args) {
		System.out.println("Unknown command.");
	}
	
	private static void register(String name, Consumer<String> action) {
		if(commands.putIfAbsent(name, action) != null)
			throw new IllegalArgumentException(name + " already registered");	
	}
	
	private static void runCommandParser() { //TODO fix command parser thread staying alive after server exit
		registerCommands();
		System.out.println("Enter commands (" + commands.keySet().stream().collect(Collectors.joining(", ")) + "): ");
		try(var scan = new Scanner(System.in)) {
			while(server.isOpen()) {
				var command = getCommand(scan.next().toLowerCase());
				String args = scan.nextLine().stripLeading();
				commandQueue.add(() -> command.accept(args));
			}
		}
	}
	
	private static Consumer<String> getCommand(String name) {
		return commands.getOrDefault(name, StartServer::unknownCommand);
	}
}

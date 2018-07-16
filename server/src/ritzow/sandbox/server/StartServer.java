package ritzow.sandbox.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.network.GameServer;
import ritzow.sandbox.server.network.Server;
import ritzow.sandbox.server.network.Server.ClientState;
import ritzow.sandbox.util.SharedConstants;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.Entity;
import ritzow.sandbox.world.entity.PlayerEntity;

public final class StartServer {
	private static volatile boolean save = true;
	private static final Path saveFile = Path.of("data/worlds/world.dat");
	
	private static final long NETWORK_SEND_INTERVAL_NANOSECONDS = Utility.millisToNanos(50);
	
	public static void main(String[] args) throws IOException {
		runNewServer(args);
	}
	
	public static void runNewServer(String[] args) throws IOException {
		Thread.currentThread().setName("Server Main");
		GameServer server = GameServer.start(Utility.getAddressOrDefault(args, 0, InetAddress.getLocalHost(), Protocol.DEFAULT_SERVER_UDP_PORT));
		System.out.println("Started server on " + Utility.formatAddress(server.getAddress()) + ".");
		World world = getWorld();
		server.setCurrentWorld(world);
		
		byte[] update = new byte[2 + 4 + 4 + 4 + 4 + 4]; //protocol, id, posX, posY, velX, velY
		Bytes.putShort(update, 0, Protocol.SERVER_ENTITY_UPDATE);
		long lastWorldUpdateTime = System.nanoTime(), lastEntitySendTime = 0;
		while(server.isOpen()) {
			server.receive();
			lastWorldUpdateTime = Utility.updateWorld(world, lastWorldUpdateTime, 
					SharedConstants.MAX_TIMESTEP, SharedConstants.TIME_SCALE_NANOSECONDS);
			if(Utility.nanosSince(lastEntitySendTime) > NETWORK_SEND_INTERVAL_NANOSECONDS) {
				for(Entity e : world) {
					Bytes.putInteger(update, 2, e.getID());
					Bytes.putFloat(update, 6, e.getPositionX());
					Bytes.putFloat(update, 10, e.getPositionY());
					Bytes.putFloat(update, 14, e.getVelocityX());
					Bytes.putFloat(update, 18, e.getVelocityY());
					server.broadcastUnreliable(update);
				}
				server.broadcastPing();
				lastEntitySendTime = System.nanoTime();
			}
			Utility.sleep(1);
		}
		server.close();
	}
	
	public static void runServer(String[] args) throws IOException {
		try {
			Thread.currentThread().setName("Update and Process");
			var hostAddress = Utility.getAddressOrDefault(args, 0, Inet4Address.getByName("0.0.0.0"), Protocol.DEFAULT_SERVER_UDP_PORT);
			Server server = Server.open(hostAddress);

			//if a save file exists, load it, otherwise generate a world
			World world = getWorld();
			server.start(world);
			
			//read user input commands
			System.out.println("Started server on " + Utility.formatAddress(server.getBindAddress()) + ".");
			new Thread(() -> runCommandParser(server, System.in)).start();
			
			while(server.isRunning()) {
				server.update();
				Utility.sleep(1);
			}
			
			if(save) saveWorld(world, saveFile);
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
	
	public static void saveWorld(World world, Path file) {
		try {
			System.out.print("Saving world... ");
			world.removeIf(e -> e instanceof PlayerEntity); //remove players before saving to file
			byte[] serialized = Bytes.compress(SerializationProvider.getProvider().serialize(world));
			Files.write(file, serialized);
			System.out.println("world saved to " + Utility.formatSize(serialized.length) + ".");
		} catch (IOException e) {
			System.out.println("Error while saving world to file: " + e.getMessage());
		}
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
	
	private static void registerCommands(Server server) {
		register("stop", 		StartServer::stopCommand);
		register("abort", 		StartServer::abortCommand);
		register("list", 		StartServer::listCommmand);
		register("disconnect", 	StartServer::disconnectCommand);
		register("say", 		StartServer::sayCommand);
		register("reset", 		StartServer::resetCommand);
	}
	
	private static void resetCommand(Server server, String args) {
		try {
			if(Files.deleteIfExists(saveFile))
				System.out.println("Deleted saved world.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		abortCommand(server, args);
	}
	
	private static void stopCommand(Server server, String args) {
		server.stop();
	}

	private static void sayCommand(Server server, String args) {
		String message = String.join(" ", args);
		server.broadcastConsoleMessage(message);
		System.out.println("Message '" + message + "' sent to " + server.getConnectedClients() + " client(s).");
	}
	
	private static void abortCommand(Server server, String args) {
		save = false;
		server.stop();
		System.out.println("Server stopped without saving to file.");
	}
	
	private static void listCommmand(Server server, String args) {
		if(server.getConnectedClients() == 0) {
			System.out.println("No connected clients.");
		} else {
			System.out.println("Connected clients:");
			for(ClientState client : server.listClients()) {
				System.out.print("\t - ");
				System.out.println(client);
			}
		}
	}
	
	private static void disconnectCommand(Server server, String args) {
		System.out.println("Disconencted all " + server.disconnectAll(String.join(" ", args)) + " connected clients.");
	}
	
	private static Map<String, BiConsumer<Server, String>> commands = new HashMap<>();
	
	private static void register(String name, BiConsumer<Server, String> action) {
		if(commands.putIfAbsent(name, action) != null)
			throw new IllegalArgumentException(name + " already registered");	
	}
	
	private static void runCommandParser(Server server, InputStream in) {
		registerCommands(server);
		System.out.println("Enter commands (" + commands.keySet().stream().collect(Collectors.joining(", ")) + "): ");
		try(Scanner scan = new Scanner(in)) {
			while(server.isRunning()) {
				commands.getOrDefault(scan.next(), StartServer::unknownCommand).accept(server, scan.nextLine().trim());
			}
		}
	}
	
	private static void unknownCommand(Server server, String args) {
		System.out.println("Unknown command.");
	}
}

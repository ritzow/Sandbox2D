package ritzow.sandbox.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.network.Server;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.entity.ItemEntity;

@SuppressWarnings("unused")
public final class StartServer {
	private static final Path
			SAVE_FILE = Path.of("world.dat");
	private static final int
			WORLD_WIDTH = 1500,
			WORLD_BASE_HEIGHT = 150,
			TERRAIN_AMPLITUDE = 10,
			SKY_HEIGHT = 20;
	private static final float
			WORLD_FREQUENCY = 0.05f;

	private static Server server;
	private static boolean save = true;

	public static void main(String[] args) throws IOException {
		InetSocketAddress bind = args.length > 0 ?
			NetworkUtility.parseSocket(args[0], Protocol.DEFAULT_SERVER_PORT) :
			new InetSocketAddress(NetworkUtility.getPrimaryAddress(true), Protocol.DEFAULT_SERVER_PORT);
		try {
			startServer(bind);
			CommandParser parser = new CommandParser();
			registerCommands(parser);
			new Thread(parser, "Command Parser").start();

			while(server.isOpen()) {
				parser.update();
				server.update();
				Utility.sleepMillis(1);
			}

			saveWorld(server.world(), SAVE_FILE);
		} catch(BindException e) {
			System.out.println("Couldn't start server on address " + NetworkUtility.formatAddress(bind));
		}
	}

	public static void startServer(InetSocketAddress bind) throws IOException {
		server = Server.start(bind);
		System.out.println("Started server on " + NetworkUtility.formatAddress(server.getAddress()) + ".");
		long time = System.nanoTime();
		boolean loadFromFile = Files.exists(SAVE_FILE);
		System.out.print((loadFromFile ? "Loading" : "Generating") + " world... ");
		server.setCurrentWorld(loadFromFile ? loadWorld(SAVE_FILE) : generateWorld());
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(time)) + ".");
	}

	public static World loadWorld(Path file) throws IOException {
		try(var in = Files.newByteChannel(file, StandardOpenOption.READ)) {
			if(in.size() > Integer.MAX_VALUE)
				throw new RuntimeException("file too large to read");
			ByteBuffer buffer = ByteBuffer.allocate((int)in.size());
			in.read(buffer);
			return SerializationProvider.getProvider().deserialize(Bytes.decompress(buffer.flip()));
		}
	}

	private static World generateWorld() {
		World world = new World(WORLD_WIDTH, WORLD_BASE_HEIGHT + TERRAIN_AMPLITUDE + SKY_HEIGHT);
		BlockGrid fg = world.getForeground();
		BlockGrid bg = world.getBackground();
		int midpoint = TERRAIN_AMPLITUDE/2;
		int base = WORLD_BASE_HEIGHT - 1;
		fg.fill(DirtBlock.INSTANCE, 0, 0, fg.getWidth(), WORLD_BASE_HEIGHT);
		bg.fill(DirtBlock.INSTANCE, 0, 0, bg.getWidth(), WORLD_BASE_HEIGHT);
		for(int column = 0; column < WORLD_WIDTH; ++column) {
			int max = base + midpoint + Math.round(midpoint * (float)Math.sin(column * WORLD_FREQUENCY));
			for(int row = base; row <= max; ++row) {
				fg.set(column, row, DirtBlock.INSTANCE);
				bg.set(column, row, DirtBlock.INSTANCE);
			}
			fg.set(column, max, GrassBlock.INSTANCE);
			bg.set(column, max, DirtBlock.INSTANCE);
		}
		return world;
	}

	private static void saveWorld(World world, Path file) {
		if(save) {
			try {
				System.out.print("Saving world... ");
				//shouldnt have any players once server is shut down
				//world.removeIf(e -> e instanceof PlayerEntity); //remove players before saving to file
				byte[] serialized = Bytes.compress(SerializationProvider.getProvider().serialize(world));
				Files.write(file, serialized);
				System.out.println("world saved to " + Utility.formatSize(serialized.length) + ".");
			} catch(IOException e) {
				System.out.println("Error while saving world to file '" + file + "':"
					+ e.getClass().getTypeName() + ":" + e.getMessage());
			}
		} else {
			System.out.println("Server stopped without saving to file.");
		}
	}

	private static void registerCommands(CommandParser runner) {
		runner	.register("stop", 		StartServer::stopCommand, true)
				.register("abort", 		StartServer::abortCommand, true)
				.register("reset", 		StartServer::resetCommand, true)
				.register("kill", 		StartServer::killCommand, true)
				.register("list", 		StartServer::listCommmand, false)
				.register("say", 		StartServer::sayCommand, false)
				.register("debug",		StartServer::debugCommand, false)
				.register("printworld", StartServer::printworldCommand, false)
				.register("killitems", 	StartServer::killItemsCommand, false);
	}

	private static void killItemsCommand(String args) {
		int entities = server.world().entities();
		server.world().removeIf(e -> e instanceof ItemEntity);
		System.out.println("Removed " + (entities - server.world().entities()) + " items from the world.");
	}

	private static void printworldCommand(String args) {
		System.out.println(server.world());
	}

	private static void debugCommand(String args) {
		server.printDebug(System.out);
	}

	private static void resetCommand(String args) {
		abortCommand(args);
		try {
			if(Files.deleteIfExists(SAVE_FILE))
				System.out.println("Deleted world " + SAVE_FILE + ".");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
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
		if(server.getClientCount() > 0) {
			System.out.println("Connected clients:");
			for(var string : server.listClients()) {
				System.out.print("  - ");
				System.out.println(string);
			}
		} else {
			System.out.println("No connected clients.");
		}
	}
}

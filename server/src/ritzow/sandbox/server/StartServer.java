package ritzow.sandbox.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.network.Server;
import ritzow.sandbox.util.Utility;
import ritzow.sandbox.world.World;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.generator.SinusoidWorldGenerator;

class StartServer {
	private static final Path SAVE_FILE = Path.of("world.dat");
	private static final long FRAME_TIME_LIMIT = Utility.frameRateToFrameTimeNanos(30);
	private static final int WIDTH = 100, HEIGHT = 200;

	private static Server server;
	private static boolean save = true;

	public static void main(String[] args) throws IOException {
		InetSocketAddress bind = args.length > 0 ?
			NetworkUtility.parseSocket(args[0], Protocol.DEFAULT_SERVER_PORT) : NetworkUtility.getPublicSocket(Protocol.DEFAULT_SERVER_PORT);
		try {
			CommandParser parser = createParser();
			new Thread(parser, "Command Parser").start();
			startServer(bind);
			while(server.isOpen()) {
				long start = System.nanoTime();
				parser.update();
				server.update();
				Utility.limitFramerate(start, FRAME_TIME_LIMIT);
			}
			saveWorld(server.world());
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
		server.setCurrentWorld(loadFromFile ? loadWorld(SAVE_FILE) : SinusoidWorldGenerator.builder()
			.width(WIDTH)
			.baseHeight(HEIGHT)
			.generate());
		System.out.println("took " + Utility.formatTime(Utility.nanosSince(time)) + ".");
	}

	public static World loadWorld(Path file) throws IOException {
		return SerializationProvider.getProvider().deserialize(Utility.loadCompressedFile(file));
	}

	private static void saveWorld(World world) {
		if(save) {
			try {
				System.out.print("Saving world... ");
				byte[] serialized = Bytes.compress(SerializationProvider.getProvider().serialize(world));
				Files.write(SAVE_FILE, serialized);
				System.out.println("world saved to " + Utility.formatSize(serialized.length) + ".");
			} catch(IOException e) {
				System.out.println("Error while saving world to file '" + SAVE_FILE + "':"
					+ e.getClass().getTypeName() + ":" + e.getMessage());
			}
		} else {
			System.out.println("Server stopped without saving to file.");
		}
	}

	private static CommandParser createParser() {
		return new CommandParser()
			.register("stop", 		StartServer::stopCommand, true)
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
		System.out.println(server.getDebugInfo());
	}

	private static void resetCommand(String args) {
		abortCommand(args);
		try {
			if(Files.deleteIfExists(SAVE_FILE))
				System.out.println("Deleted world " + SAVE_FILE + ".");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void stopCommand(String args) {
		server.shutdown();
	}

	private static void abortCommand(String args) {
		save = false;
		server.shutdown();
	}

	private static void killCommand(String args) {
		try {
			save = false;
			server.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void sayCommand(String args) {
		server.broadcastConsoleMessage(args);
		System.out.println("Message '" + args + "' sent to " + server.getClientCount() + " client(s).");
	}

	private static void listCommmand(String args) {
		if(server.getClientCount() > 0) {
			System.out.print(server.clientNames()
				.collect(Collectors.joining("\n  - ", "Connected clients:\n  - ", "\n")));
		} else {
			System.out.println("No connected clients.");
		}
	}
}

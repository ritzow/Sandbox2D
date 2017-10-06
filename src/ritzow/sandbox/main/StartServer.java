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
import ritzow.sandbox.protocol.WorldObjectIdentifiers;
import ritzow.sandbox.server.Server;
import ritzow.sandbox.server.Server.ClientState;
import ritzow.sandbox.server.ServerAudioSystem;
import ritzow.sandbox.server.ServerWorld;
import ritzow.sandbox.util.ByteUtil;
import ritzow.sandbox.util.Deserializer;
import ritzow.sandbox.util.SerializerReaderWriter;
import ritzow.sandbox.world.BlockGrid;
import ritzow.sandbox.world.block.DirtBlock;
import ritzow.sandbox.world.block.GrassBlock;
import ritzow.sandbox.world.component.Inventory;
import ritzow.sandbox.world.entity.ItemEntity;
import ritzow.sandbox.world.entity.ParticleEntity;
import ritzow.sandbox.world.entity.PlayerEntity;
import ritzow.sandbox.world.item.BlockItem;

public final class StartServer {
	public static void main(String... args) throws SocketException, UnknownHostException {
		Thread.currentThread().setName("Server Setup");
		Server server = new Server(new InetSocketAddress(50000));
		
		//the save file to try to load the world from
		final File saveFile = new File("data/worlds/testWorld.dat");
		
		ServerAudioSystem audio = new ServerAudioSystem();
		ServerWorld world = saveFile.exists() ? loadWorld(saveFile, createSerializer()) : generateWorld(100, 100, audio, server);
		world.setAudioSystem(audio);
		world.setServer(server);
		server.setWorld(world);
		
		server.start();
		
		System.out.println("Startup Complete.");
		System.out.println("Type 'exit' to stop server or anything else to broadcast a message:");
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
		
		System.out.println("Shutdown Complete.");
		
		try {
			if(!saveFile.exists()) {
				saveFile.createNewFile();
			}
			
			try(FileOutputStream out = new FileOutputStream(saveFile)) {
				System.out.print("Saving world... ");
				//will save players, etc. if not removed beforehand
				byte[] serialized = ByteUtil.compress(createSerializer().serialize(world));
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
	
	public static SerializerReaderWriter createSerializer() {
		SerializerReaderWriter s = new SerializerReaderWriter();
		s.register(WorldObjectIdentifiers.WORLD, ServerWorld.class, ServerWorld::new);
		s.register(WorldObjectIdentifiers.BLOCK_GRID, BlockGrid.class, BlockGrid::new);
		s.register(WorldObjectIdentifiers.BLOCK_ITEM, BlockItem.class, BlockItem::new);
		s.register(WorldObjectIdentifiers.DIRT_BLOCK, DirtBlock.class, DirtBlock::new);
		s.register(WorldObjectIdentifiers.GRASS_BLOCK, GrassBlock.class, GrassBlock::new);
		s.register(WorldObjectIdentifiers.PLAYER_ENTITY, PlayerEntity.class, PlayerEntity::new);
		s.register(WorldObjectIdentifiers.INVENTORY, Inventory.class, Inventory::new);
		s.register(WorldObjectIdentifiers.ITEM_ENTITY, ItemEntity.class, ItemEntity::new);
		s.register(WorldObjectIdentifiers.PARTICLE_ENTITY, ParticleEntity.class, ParticleEntity::new);
		return s;
	}
	
	public static ServerWorld loadWorld(File file, Deserializer des) {
		try(FileInputStream in = new FileInputStream(file)) {
			byte[] data = new byte[(int)file.length()];
			in.read(data);
			return des.deserialize(ByteUtil.decompress(data));
		} catch(IOException e) {
			System.out.println("Error loading world from file " + e);
			return null;
		}
	}
	
	public static ServerWorld generateWorld(int width, int height, AudioSystem audio, Server server) {
		ServerWorld world = new ServerWorld(audio, server, width, height, 0.016f);
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
		world.setServer(server);
		return world;
	}
}

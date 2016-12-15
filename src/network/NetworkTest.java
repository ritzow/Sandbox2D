package network;

import java.io.IOException;
import util.ByteUtil;
import world.World;
import world.block.DirtBlock;
import world.block.GrassBlock;
import world.block.RedBlock;

/**
 * @author Solomon Ritzow
 *
 */
public class NetworkTest {
	public static void main(String[] args) throws IOException {
		World world = new World(500, 10000);
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
		
		try {
			byte[] serialized = ByteUtil.serializeCompressed(world);
			System.out.println("Serialized size: " + serialized.length + " bytes");
			World grid = (World)ByteUtil.deserializeCompressed(serialized);
			System.out.println(grid);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
//		try {
//			byte[] serialized = ByteUtil.serializeCompressed(new ItemEntity(new BlockItem(new RedBlock())));
//			System.out.println("Serialized size: " + serialized.length + " bytes");
//			ItemEntity item = (ItemEntity)ByteUtil.deserializeCompressed(serialized);
//			System.out.println(item);
//		} catch (IOException | ClassNotFoundException e) {
//			e.printStackTrace();
//		}
		
//		Client client = new Client();
//		Server server = new Server(100);
//
//		new Thread(server).start();
//		new Thread(client).start();
//		
//		SocketAddress serverAddress = server.getSocketAddress();
//		
//		if(client.connectToServer(serverAddress, 1, 1000)) {
//			System.out.println("Client connected to " + serverAddress);
//		} else {
//			System.out.println("Client failed to connect to " + serverAddress);
//		}
//		
//		client.exit();
//		server.exit();
	}
}

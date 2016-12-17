package main;

import java.io.IOException;
import util.ByteUtil;
import world.World;
import world.block.DirtBlock;
import world.block.GrassBlock;
import world.block.RedBlock;

public class Tester {
	public static void main(String[] args) throws ReflectiveOperationException {
		
		System.out.print("Creating world... ");
		World world = new World(5000, 10);
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
		System.out.println("world created.");
		
		try {
			for(int i = 10; i >= 0; i--) {
				byte[] serializedWorld = ByteUtil.compress(ByteUtil.serialize(world), i);
				System.out.println("Compressed " + i + " times: " + serializedWorld.length + " bytes");
				ByteUtil.deserialize(ByteUtil.decompress(serializedWorld, i));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			
		}
	}
}

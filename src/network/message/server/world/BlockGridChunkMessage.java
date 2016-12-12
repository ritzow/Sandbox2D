package network.message.server.world;

import java.util.Arrays;
import network.message.InvalidMessageException;
import network.message.Message;
import world.BlockGrid;
import world.block.Block;

/**
 * @author Solomon Ritzow
 *
 */
public class BlockGridChunkMessage implements Message {
	
	protected String name;
	protected Block[][] blocks;
	
	public BlockGridChunkMessage(String name, BlockGrid grid, int startX, int startY, int width, int height) {
		this.name = name;
		//this.blocks = grid.toArray(startX, startY, width, height);
		throw new UnsupportedOperationException("Class not implemented");
	}
	
	public BlockGridChunkMessage(byte[] data) throws InvalidMessageException {
//		try {
//			//Object object = ByteUtil.deserialize(data, 0, data.length);
//			
//			if(object instanceof Block[][]) {
//				this.blocks = (Block[][])object;
//			} else {
//				throw new InvalidMessageException("Deserialized object is of incorrect type");
//			}
//		} catch (ClassNotFoundException | IOException e) {
//			throw new InvalidMessageException(e);
//		}
	}

	@Override
	public byte[] getBytes() {
//		try {//TODO implement naming system or some other more effective system for foreground/background etc.
//			byte[] object = ByteUtil.serialize(blocks);
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
		return null;
	}

	@Override
	public String toString() {
		return Arrays.deepToString(blocks);
	}

}

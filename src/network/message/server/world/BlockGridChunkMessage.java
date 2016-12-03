package network.message.server.world;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import network.message.InvalidMessageException;
import network.message.Message;
import network.message.Protocol;
import util.ByteUtil;
import world.BlockGrid;
import world.block.Block;

/**
 * @author Solomon Ritzow
 *
 */
public class BlockGridChunkMessage extends Message {
	
	protected Block[][] blocks;
	
	public BlockGridChunkMessage(BlockGrid grid, int startX, int startY, int width, int height) {
		this.blocks = grid.toArray(startX, startY, width, height);
	}
	
	public BlockGridChunkMessage(DatagramPacket packet) throws InvalidMessageException {
		
		if(ByteUtil.getShort(packet.getData(), 0) != Protocol.WORLD_CHUNK_MESSAGE)
			throw new InvalidMessageException("incorrect protocol ID");
		
		try {
			Object object = ByteUtil.deserialize(packet.getData(), packet.getOffset() + 2, packet.getLength());
			
			if(object instanceof Block[][]) {
				this.blocks = (Block[][])object;
			} else {
				throw new InvalidMessageException("Deserialized object is of incorrect type");
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte[] getBytes() {
		try {
			return ByteUtil.serialize(blocks);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String toString() {
		return Arrays.deepToString(blocks);
	}

}

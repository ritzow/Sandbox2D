package ritzow.sandbox.client.audio;

public final class Sounds {
	public static final int 
			BLOCK_BREAK = 0,
			BLOCK_PLACE = 1,
			ITEM_PICKUP = 2,
			THROW = 3,
			SNAP = 4;
	
	public static int forFile(String fileName) {
		switch(fileName) {
			case "dig.wav":
				return Sounds.BLOCK_BREAK;
			case "place.wav":
				return Sounds.BLOCK_PLACE;
			case "pop.wav":
				return Sounds.ITEM_PICKUP;
			case "throw.wav":
				return Sounds.THROW;
			case "snap.wav":
				return Sounds.SNAP;
			default:
				throw new IllegalArgumentException("unknown sound file");
		}
	}
}

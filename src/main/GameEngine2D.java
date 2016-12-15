package main;

import input.EventManager;

public class GameEngine2D {
	
	static final boolean PRINT_MEMORY_USAGE = false;
	
	public static void main(String[] args) throws InterruptedException, ReflectiveOperationException {
//		Custom serialization test:
//		long time = System.nanoTime();
//		byte[] serialized = ByteUtil.serializeTransportable(new SerializationTestEntity(3, 209890));
//		Transportable t = ByteUtil.deserializeTransportable(serialized);
//		System.out.println((System.nanoTime() - time) * 0.000001);
//		System.out.println(t);
//		System.exit(0);
		
		EventManager eventManager = new EventManager();
		new Thread(new GameManager(eventManager), "Game Manager").start();
		eventManager.run();
	}
}
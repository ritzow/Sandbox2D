package network;

import java.util.LinkedList;
import network.message.*;
import networkutils.InvalidMessageException;
import networkutils.UnknownMessageException;
import util.Exitable;

public class ClientMessageHandler implements MessageHandler, Runnable, Exitable {

	protected volatile boolean exit;
	protected volatile boolean finished;
	protected LinkedList<byte[]> unprocessedPackets;

	public ClientMessageHandler() {
		this.unprocessedPackets = new LinkedList<byte[]>();
	}
	
	@Override
	public void run() {
		
		MessageConstructorRunner constructor = new MessageConstructorRunner();
		
		while(!exit) {
			if(unprocessedPackets.pollFirst() != null) {
				try {
					constructor.constructMessage(unprocessedPackets.getFirst(), this);
					unprocessedPackets.removeFirst();
				} catch (UnknownMessageException e) {
					e.printStackTrace();
				} catch (InvalidMessageException e) {
					e.printStackTrace();
				}
			}
		}

		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}

	public synchronized void add(byte[] packet) {
		unprocessedPackets.addLast(packet);
	}

	@Override
	public void exit() {
		this.exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void handleMessage(ClientInfoMessage message) {
		
	}

	@Override
	public void handleMessage(EntityUpdateMessage message) {
		//TODO implement method 'handleMessage' in type 'MessageHandler'
		
	}

	@Override
	public void handleMessage(ServerConnectAcknowledgement message) {
		System.out.println(message.toString());
	}

	@Override
	public void handleMessage(ServerConnectRequest messsage) {
		//TODO implement method 'handleMessage' in type 'MessageHandler'
		
	}

	@Override
	public void handleMessage(ServerInfoMessage message) {
		//TODO implement method 'handleMessage' in type 'MessageHandler'
		
	}

	@Override
	public void handleMessage(ServerInfoRequest message) {
		//TODO implement method 'handleMessage' in type 'MessageHandler'
		
	}
}

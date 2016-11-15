package network.client;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import network.MessageDispatcher;
import network.MessageHandler;
import network.message.*;
import networkutils.InvalidMessageException;
import networkutils.UnknownMessageException;

public class Client {

	protected DatagramSocket socket;

	protected volatile boolean exit;
	protected volatile boolean finished;
	
	public Client() throws IOException {
		this.socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
	
//	public synchronized ServerInfoMessage getServerInfo(InetSocketAddress address, long timeout) throws IOException, InvalidMessageException {
//		byte[] request = new ServerInfoRequest().getBytes();
//		socket.send(new DatagramPacket(request, request.length, address));
//		long start = System.currentTimeMillis();
//		DatagramPacket buffer = new DatagramPacket(new byte[100], 100);
//		while(System.currentTimeMillis() - start < timeout) {
//			socket.receive(buffer);
//		}
//		
//		return null;
//	}
	
	public synchronized boolean connectToServer(SocketAddress address) throws IOException, SocketTimeoutException {
		byte[] request = new ServerConnectRequest().getBytes();
		socket.send(new DatagramPacket(request, request.length, address));
		
		try {
			socket.setSoTimeout(1000);
			DatagramPacket response = new DatagramPacket(new byte[100], 100);
			socket.receive(response);
			
			if(new ServerConnectAcknowledgement(response.getData()).isAccepted()) {
				return true;
			}
			
			else {
				return false;
			}
			
		} catch (InvalidMessageException e) {
			return false;
		}
	}
	
	public synchronized void disconnectFromServer() {
		
	}
	
	protected class ClientLogicHandler implements MessageHandler, Runnable {
		protected LinkedList<byte[]> unprocessedPackets;
		
		protected InetSocketAddress serverAddress;

		public ClientLogicHandler() {
			this.unprocessedPackets = new LinkedList<byte[]>();
		}
		
		@Override
		public void run() {
			MessageDispatcher dispatcher = new MessageDispatcher();
			
			try {
				while(!exit) {
					if(unprocessedPackets.pollFirst() != null) {
						try {
							dispatcher.process(unprocessedPackets.getFirst(), this);
							unprocessedPackets.removeFirst();
						} catch (UnknownMessageException e) {
							e.printStackTrace();
						} catch (InvalidMessageException e) {
							e.printStackTrace();
						}
					} else {
						synchronized(this) {
							while(!exit && unprocessedPackets.pollFirst() == null) {
								this.wait();
							}
						}
					}
					Thread.sleep(1);
				}
			} catch(InterruptedException e) {
				
			}

			synchronized(this) {
				finished = true;
				this.notifyAll();
			}
		}

		public synchronized void process(byte[] packet) {
			unprocessedPackets.addLast(packet);
			this.notifyAll();
		}

		@Override
		public void handleMessage(ClientInfoMessage message) {
			
		}

		@Override
		public void handleMessage(EntityUpdateMessage message) {
			
		}

		@Override
		public void handleMessage(ServerConnectAcknowledgement message) {
			if(message.isAccepted()) {
				
			} else {
				
			}
		}

		@Override
		public void handleMessage(ServerConnectRequest messsage) {
			
		}

		@Override
		public void handleMessage(ServerInfoMessage message) {
			
		}

		@Override
		public void handleMessage(ServerInfoRequest message) {
			
		}
	}

}

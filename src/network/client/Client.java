package network.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import network.MessageHandler;
import network.message.*;

public final class Client implements Closeable {

	protected final DatagramSocket socket;

	protected volatile boolean exit;
	protected volatile boolean finished;
	
	public Client() throws IOException {
		this.socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
	
	/**
	 * Connect to a specified SocketAddress using a ServerConnectRequest
	 * @param address the socket address of the server
	 * @param attempts number of times to resend the connection request
	 * @param timeout the total amount of time in milliseconds to wait for the server to respond
	 * @return whether or not the server responded and accepted the client's connection
	 * @throws IOException if the socket throws an IOException
	 */
	public synchronized boolean connectToServer(SocketAddress address, int attempts, int timeout) throws IOException {
		if(timeout != 0 && timeout < attempts)
			throw new UnsupportedOperationException("timeout too small");
		socket.setSoTimeout(timeout/attempts);
		DatagramPacket response = new DatagramPacket(new byte[100], 100);
		byte[] packet = new ServerConnectRequest().getBytes();
		DatagramPacket request = new DatagramPacket(packet, packet.length, address);
		socket.send(request);
		long startTime = System.currentTimeMillis();
		int attemptsRemaining = attempts;
		while(attemptsRemaining >= 0 && (System.currentTimeMillis() - startTime < timeout || timeout == 0)) {
			try {
				socket.receive(response);
				if(response.getSocketAddress().equals(address)) {
					return (new ServerConnectAcknowledgment(response).isAccepted());
				} else {
					continue;
				}
			} catch(InvalidMessageException e) {
				continue;
			} catch(SocketTimeoutException e) {
				if(--attemptsRemaining > 0)
					socket.send(request);
				continue;
			} catch(PortUnreachableException e) {
				return false;
			} catch(SocketException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
		this.exit = true;
	}
	
	public void run() {
		
	}
	
	protected class ClientLogicHandler implements MessageHandler, Runnable {
		protected LinkedList<byte[]> unprocessedPackets;
		protected InetSocketAddress serverAddress;

		public ClientLogicHandler() {
			this.unprocessedPackets = new LinkedList<byte[]>();
		}
		
		@Override
		public void run() {
			try {
				while(!exit) {
					if(unprocessedPackets.pollFirst() != null) {
						unprocessedPackets.removeFirst();
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
	}

}

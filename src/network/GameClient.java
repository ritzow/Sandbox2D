package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import networkutils.DatagramInputStream;
import networkutils.DatagramOutputStream;
import util.Exitable;

public class GameClient implements Runnable, Exitable {

	protected DatagramSocket socket;
	protected SocketAddress serverAddress;

	protected volatile boolean exit;
	protected volatile boolean finished;

	public GameClient(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
			DatagramInputStream input = new DatagramInputStream(socket);
			DatagramOutputStream output = new DatagramOutputStream(socket, serverAddress);
			ClientMessageHandler messageHandler = new ClientMessageHandler();
			new Thread(messageHandler, "Client Message Handler").start();
			
			while(!exit) {
				messageHandler.add(input.readPacket());
			}

			input.close();
			output.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		synchronized(this) {
			finished = true;
			this.notifyAll();
		}
	}

	@Override
	public void exit() {
		this.exit = true;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

}

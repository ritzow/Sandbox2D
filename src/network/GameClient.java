package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import network.message.*;
import networkutils.*;
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
			output.write(new ServerInfoRequest().getBytes());
			Message message = MessageParser.getMessage(input.readPacket());
			if(message instanceof ServerInfoMessage) {
				System.out.println("Server Info: " + ((ServerInfoMessage)message).getPlayerCount() + "/" + ((ServerInfoMessage)message).getPlayerCapacity() + " players");
			}
			
			input.close();
			output.close();
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnknownMessageException e) {
			e.printStackTrace();
		} catch (InvalidMessageException e) {
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
	
	public void connectToServer() {
		
	}
	
}

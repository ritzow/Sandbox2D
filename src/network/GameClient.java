package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import network.message.*;
import networkutils.DatagramInputStream;
import networkutils.DatagramOutputStream;
import networkutils.InvalidMessageException;
import networkutils.Message;
import networkutils.UnknownMessageException;
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
		//TODO continue creating game client
		try {
			socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
			DatagramInputStream input = new DatagramInputStream(socket);
			DatagramOutputStream output = new DatagramOutputStream(socket, serverAddress);
			output.write(new ServerInfoRequest().getBytes());
			output.write(new ClientInfoMessage("blobjim").getBytes());
			
			while(!exit) {
				try {
					Message message = MessageParser.getMessage(input.readPacket());
					
					if(message instanceof ServerInfoMessage) {
						System.out.println("Server Info: " + message.toString());
					}
					
					else if(message instanceof ServerConnectAcknowledgement) {
						if(((ServerConnectAcknowledgement)message).isAccepted()) {
							System.out.println("The server accepted the connetion request");
						} else {
							System.out.println("The server denied the connection request");
						}
					}
					
					else {
						System.out.println("Message received from server: " + message.toString());
					}
				} catch (UnknownMessageException e) {
					continue;
				} catch (InvalidMessageException e) {
					continue;
				} catch(SocketException e) {
					break;
				} catch (IOException e) {
					System.err.println(e.getMessage());
					continue;
				}
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

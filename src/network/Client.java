package network;
import java.io.*;
import java.net.*;

public class Client {
	
	Socket client;
	
	public Client(InetAddress address, int port) throws IOException {
		this.client = new Socket(address, port);
	}
	
	public boolean sendMessage(String message) {
		try {
			client.getOutputStream().write((message + '\u0000').getBytes());
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean sendObject(Object o) {
		try {
			new ObjectOutputStream(client.getOutputStream()).writeObject(o);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public String getMessage() {
		StringBuilder s = new StringBuilder();
		char buffer;
		
		try {
			while((buffer = (char)client.getInputStream().read()) != '\u0000') {
				s.append(buffer);
			}
		} 
		
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return s.toString();
	}
	
	public Object getObject() {
		try {
			return (new ObjectInputStream(client.getInputStream())).readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

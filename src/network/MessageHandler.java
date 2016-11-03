package network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import util.Exitable;

public class MessageHandler implements Runnable, Exitable {
	
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	
	private volatile boolean exit;
	private volatile boolean finished;
	
	public MessageHandler(Socket socket) {
		this.socket = socket;
		try {
			this.input = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			new Thread() {
				public void run() {
					while(!exit) { //receive data from input stream
						System.out.println("hello");
					}
					
					MessageHandler.this.notifyAll();
				}
			}.start();
			
			new Thread() {
				public void run() {
					try {
						while(!exit) { //send stuff to output stream
							input.read();
						}
					} catch(IOException e) {
						
					}
					
					MessageHandler.this.notifyAll();
				}
			}.start();
			
			try {
				while(!exit) {
					this.wait();
				}
			} catch (InterruptedException e) {

			}
			
			input.close();
			output.close();
			socket.close();
			
		} catch(SocketException e) {
			
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

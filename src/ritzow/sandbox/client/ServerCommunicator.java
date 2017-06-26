package ritzow.sandbox.client;

import ritzow.sandbox.protocol.Message;

//TODO perhaps in the future create a simple interface for communicating with a server that is abstracted and doesnt use networking
public interface ServerCommunicator {
	public void send(Message message);
	public void setOnReceiveMessage(); //TODO finish this thing
}

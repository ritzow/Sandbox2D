package ritzow.sandbox.server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.ReceivePacket;
import ritzow.sandbox.network.SendPacket;

import static ritzow.sandbox.network.Protocol.*;

/** The server manages connected game clients, sends game updates,
 * receives client input, and broadcasts information for clients. */
public class Server<T extends ClientNetworkInfo> {

	private final DatagramChannel channel;
	private final ByteBuffer receiveBuffer, sendBuffer;
	private final Map<InetSocketAddress, T> clients;

	public Server(InetSocketAddress bind) throws IOException {
		channel = DatagramChannel.open(NetworkUtility.protocolOf(bind.getAddress())).bind(bind);
		channel.configureBlocking(false);
		clients = new HashMap<>();
		sendBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
		receiveBuffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
	}

	public Collection<T> clients() {
		return clients.values();
	}

	public void close() throws IOException {
		channel.close();
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public InetSocketAddress getAddress() throws IOException {
		return (InetSocketAddress)channel.getLocalAddress();
	}

	public void receive(Function<InetSocketAddress, T> init, BiConsumer<T, ByteBuffer> messageProcessor) throws IOException {
		InetSocketAddress sender;
		while((sender = (InetSocketAddress)channel.receive(receiveBuffer)) != null) {
			processPacket(sender, init, messageProcessor);
			receiveBuffer.clear();
		}
	}

	private void sendResponse(InetSocketAddress recipient, ByteBuffer sendBuffer, int receivedMessageID) throws IOException {
		channel.send(sendBuffer.put(RESPONSE_TYPE).putInt(receivedMessageID).flip(), recipient);
		sendBuffer.clear();
	}

	//use min heap (PriorityQueue) to keep messages in order while queued for processing
	//if message received is next message, don't bother putting it in queue
	private void processPacket(InetSocketAddress sender, Function<InetSocketAddress, T> init, BiConsumer<T, ByteBuffer> messageProcessor) throws IOException {
		if(receiveBuffer.flip().limit() >= MIN_PACKET_SIZE) { //check that packet is large enough
			T client = clients.computeIfAbsent(sender, init);
			client.lastMessageReceiveTime = System.nanoTime();
			byte type = receiveBuffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = receiveBuffer.getInt();
			switch(type) {
				case RESPONSE_TYPE -> {
					//remove packet from send queue once acknowledged
					Iterator<SendPacket> packets = client.sendQueue.iterator();
					while(packets.hasNext()) {
						SendPacket packet = packets.next();
						if(packet.messageID < messageID) {
							break;
						} else if(packet.messageID == messageID) {
							packets.remove();
							break;
						}
					}
				}

				//predecessorID needs to be replaced with messageID in some of this, fix
				case RELIABLE_TYPE -> {
					sendResponse(client.address, sendBuffer, messageID);
					if(messageID > client.headProcessedID) {
						int predecessorID = receiveBuffer.getInt();
						if(predecessorID <= client.headProcessedID) {
							//no need to add to the queue, this is the next message in the stream.
							sendResponse(client.address, sendBuffer, messageID);
							process(client, messageID, receiveBuffer, messageProcessor);
						} else if(predecessorID > client.headProcessedID) {
							//predecessorID > headProcessedID
							//this will also happen if the message was already received
							//this wastes space in case of
							queueReceived(client, messageID, predecessorID, true, receiveBuffer);
						} //else error in packet data
					}
				}

				case UNRELIABLE_TYPE -> {
					//only process messages that aren't older than already processed messages
					//in order to keep all message processing in order
					if(messageID > client.headProcessedID) {
						int predecessorID = receiveBuffer.getInt();
						if(predecessorID > client.headProcessedID) {
							queueReceived(client, messageID, predecessorID, false, receiveBuffer);
						} else {
							//don't need to queue something that can be processed immediately
							process(client, messageID, receiveBuffer, messageProcessor);
						}
					}
				}
			}
		}
	}

	private void queueReceived(ClientNetworkInfo client, int messageID, int predecessorID, boolean reliable, ByteBuffer data) {
		byte[] copy = new byte[data.remaining()];
		receiveBuffer.get(copy);
		client.receiveQueue.add(new ReceivePacket(messageID, predecessorID, reliable, copy));
	}

	private void process(T client, int messageID, ByteBuffer receiveBuffer, BiConsumer<T, ByteBuffer> messageProcessor) {
		messageProcessor.accept(client, receiveBuffer);
		client.headProcessedID = messageID;
		ReceivePacket packet = client.receiveQueue.peek();
		while(packet != null && packet.predecessorReliableID() <= client.headProcessedID) {
			client.receiveQueue.poll();
			if(packet.messageID() > client.headProcessedID) {
				messageProcessor.accept(client, ByteBuffer.wrap(packet.data()));
				client.headProcessedID = packet.messageID();
			} //else was a duplicate
			packet = client.receiveQueue.peek();
		}
	}

	public void sendQueued() throws IOException {
		//TODO maybe separate reliable messages into a different datastructure and use queue only for unreliables?
		for(ClientNetworkInfo client : clients.values()) {
			Iterator<SendPacket> packets = client.sendQueue.iterator();
			while(packets.hasNext()) {
				SendPacket packet = packets.next();
				if(packet.reliable) {
					long time = System.nanoTime();
					if(packet.lastSendTime == -1 || time - packet.lastSendTime > RESEND_INTERVAL) {
						sendBuffer(client, RELIABLE_TYPE, packet);
						packet.lastSendTime = time;
					}
				} else {
					//always remove unreliable messages, they will never be re-sent
					packets.remove();
					sendBuffer(client, UNRELIABLE_TYPE, packet);
				}
			}
		}
	}

	private void sendBuffer(ClientNetworkInfo client, byte type, SendPacket packet) throws IOException {
		channel.send(sendBuffer.put(type).putInt(packet.messageID).putInt(packet.lastReliableID).put(packet.data).flip(), client.address);
		sendBuffer.clear();
	}
}

package ritzow.sandbox.client.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.function.Consumer;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.network.ReceivePacket;
import ritzow.sandbox.network.SendPacket;
import ritzow.sandbox.util.Utility;

/**
 * Protocol representation:
 * Communication happens as a single bi-directional message stream, which contains
 * reliable messages and unreliable messages, all in order.
 *
 * Receiving: Unreliable messages will always be processed after their
 * preceding reliable message. Received messages that are not the next message will
 * be kept in a queue until they or a subsequent reliable message is acknowledged. If
 * an unreliable message
 *
 * Sending: All currently queued messages will be sent (ie they haven't been acked and removed), any reliable message that
 * insn't acknowledged will be re-sent after the resend interval.
 *
 * If unreliable packets are received
 *
 * 	TODO multiplex this entire protocol so that concurrent downloads can happen while gameplay is occuring
 * 	ie loading the next map while playing on the current one (no need for next map messages to not be interlaced).
 *
 * If the received messages buffer begins to reach its size limit, unreliable messages
 * may be purged from it.
 */
public class Client implements AutoCloseable {

	//sender/receiver state
	private final DatagramChannel channel;
	/** sendQueue contains unsent reliable and unreliable messages and unacknowledged reliable messages **/
	private final Queue<SendPacket> sendQueue;
	private final PriorityQueue<ReceivePacket> received;
	private final Map<Integer, Runnable> messageSentActions;
	private int sendMessageID = 0, lastSendReliableID = -1, headProcessedID = -1;
	private long lastMessageReceive;
	private boolean isUp;
	private long ping; //rount trip time in nanoseconds

	public interface MessageProcessor {
		//todo make this take one argument

		/**
		 * Called by the Client class when a received message can be processed
		 * @param data the data received
		 * @return true if processing should continue
		 */
		boolean process(ByteBuffer data);
	}

	private Runnable onTimeout;
	private Consumer<IOException> onException;

	public Client setOnTimeout(Runnable action) {
		this.onTimeout = action;
		return this;
	}

	public Client setOnException(Consumer<IOException> action) {
		this.onException = action;
		return this;
	}

	/**
	 * Creates a client bound to the provided address.
	 * @param bindAddress the local address to bind to.
	 * @param serverAddress the address of the game server to connect to.
	 * @return a new game client to communicate with the server at {@code serverAddress}.
	 * @throws IOException if an internal I/O error occurrs.
	 * @throws SocketException if the local address could not be bound to.
	 */
	public static Client create(InetSocketAddress bindAddress, InetSocketAddress serverAddress) throws IOException {
		return new Client(bindAddress, serverAddress);
	}

	public Client beginConnect() {
		lastMessageReceive = System.nanoTime();
		sendReliable(Bytes.of(Protocol.TYPE_CLIENT_CONNECT_REQUEST));
		return this;
	}

	private Client(InetSocketAddress bindAddress,
			InetSocketAddress serverAddress) throws IOException {
		this.channel = DatagramChannel
				.open(NetworkUtility.protocolOf(bindAddress.getAddress()))
				.bind(bindAddress)
				.connect(serverAddress);
		this.channel.configureBlocking(false);
		this.sendQueue = new ArrayDeque<>();
		this.received = new PriorityQueue<>();
		this.messageSentActions = new HashMap<>();
		this.isUp = true;
	}

	public InetSocketAddress getLocalAddress() {
		try {
			return (InetSocketAddress)channel.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public InetSocketAddress getServerAddress() {
		try {
			return (InetSocketAddress)channel.getRemoteAddress();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return the round trip time for the last
	 * reliable message sent to the server, in nanoseconds.
	 */
	public long getPing() {
		return ping;
	}

	/** Queues an unreliable message
	 * @param data the packet message, including message type, to send **/
	public void sendUnreliable(byte[] data) {
		send(data, false);
	}

	/** Queues a reliable message
	 * @param data the packet message, including message type, to send **/
	public void sendReliable(byte[] data) {
		send(data, true);
	}

	/** Queues a reliable message, and runs an action after the message is received by the server
	 * @param data the packet message, including message type, to send
	 * @param action an action to run when the message is acknowledged by the server **/
	public void sendReliable(byte[] data, Runnable action) {
		messageSentActions.put(sendMessageID, action);
		send(data, true);
	}

	private void send(byte[] data, boolean reliable) {
		SendPacket packet = new SendPacket(Arrays.copyOf(data, data.length), sendMessageID, lastSendReliableID, reliable, -1);
		if(reliable) lastSendReliableID = sendMessageID;
		sendMessageID++;
		sendQueue.add(packet);
	}

	/**
	 * Abruptly close the client without notifying a connected server
	 * @throws IOException if an exception occurs when closing the networking socket
	 */
	@Override
	public void close() throws IOException {
		isUp = false;
		channel.close();
	}

	public void startClose() { //TODO deal with this and close and onTimeout in a cleaner way
		isUp = false;
	}

	/** Buffers for sending packets, receiving packets, and sending acknowledgements **/
	private final ByteBuffer
		sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE),
		receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE);

//	public void update(MessageProcessor processor) {
//		update(Integer.MAX_VALUE, processor);
//	}

	public void update(MessageProcessor processor) {
		try {
			//already connected to server so no need to check SocketAddress
			boolean cont = true;
			while(isUp && cont && channel.receive(receiveBuffer) != null) {
				lastMessageReceive = System.nanoTime();
				receiveBuffer.flip(); //flip to set limit and prepare to read packet data
				cont = processReceived(processor); //process messages from the server
				receiveBuffer.clear(); //clear to prepare for next receive
			}

			if(Utility.nanosSince(lastMessageReceive) > Protocol.TIMEOUT_DISCONNECT) {
				onTimeout.run();
			} else if(isUp) {
				sendQueued();
			}
		} catch(IOException e) {
			isUp = false;
			onException.accept(e);
		}
	}

	private void sendQueued() throws IOException {
		//TODO maybe separate reliable messages into a different datastructure and use queue only for unreliables?
		//TODO possible to do in reverse to process oldest first (the ones holding the rest up)?
		Iterator<SendPacket> packets = sendQueue.iterator();
		while(packets.hasNext()) {
			SendPacket packet = packets.next();
			if(packet.reliable) {
				long time = System.nanoTime();
				if(packet.lastSendTime == -1 || time - packet.lastSendTime > Protocol.RESEND_INTERVAL) {
					sendBuffer(Protocol.RELIABLE_TYPE, packet.messageID, packet.lastReliableID, packet.data);
					packet.lastSendTime = time;
				}
			} else {
				//always remove unreliable messages, they will never be re-sent
				packets.remove();
				sendBuffer(Protocol.UNRELIABLE_TYPE, packet.messageID, packet.lastReliableID, packet.data);
			}
		}
	}

	private void sendBuffer(byte type, int messageID, int lastSendReliableID, byte[] data) throws IOException {
		channel.write(sendBuffer.put(type).putInt(messageID).putInt(lastSendReliableID).put(data).flip());
		sendBuffer.clear();
	}

	private void sendResponse(int receivedMessageID) throws IOException {
		channel.write(sendBuffer.put(Protocol.RESPONSE_TYPE).putInt(receivedMessageID).flip());
		sendBuffer.clear();
	}

	private boolean processReceived(MessageProcessor processor) throws IOException {
		if(receiveBuffer.limit() >= Protocol.MIN_PACKET_SIZE) {
			byte type = receiveBuffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = receiveBuffer.getInt(); //received ID or messageID for ack.
			switch(type) {
				case Protocol.RESPONSE_TYPE -> {
					//remove packet from send queue once acknowledged
					Iterator<SendPacket> packets = sendQueue.iterator();
					while(packets.hasNext()) { //always decreasing in ID
						SendPacket packet = packets.next();
						if(packet.messageID < messageID) {
							break;
						} else if(packet.messageID == messageID) {
							//can run multiple times for duplicates
							packets.remove();
							var action = messageSentActions.get(packet.messageID);
							if(action != null) action.run();
						}
					}
				}

				//predecessorID needs to be replaced with messageID in some of this, fix
				case Protocol.RELIABLE_TYPE -> {
					sendResponse(messageID);
					if(messageID > headProcessedID) {
						int predecessorID = receiveBuffer.getInt();
						if(predecessorID <= headProcessedID) {
							//no need to add to the queue, this is the next message in the stream.
							sendResponse(messageID);
							return process(processor, messageID, receiveBuffer);
						} else if(predecessorID > headProcessedID) {
							//predecessorID > headProcessedID
							//this will also happen if the message was already received
							//this wastes space in case of
							queueReceived(messageID, predecessorID, true, receiveBuffer);
						}
					}
				}

				case Protocol.UNRELIABLE_TYPE -> {
					//only process messages that aren't older than already processed messages
					//in order to keep all message processing in order
					if(messageID > headProcessedID) {
						int predecessorID = receiveBuffer.getInt();
						if(predecessorID > headProcessedID) {
							queueReceived(messageID, predecessorID, false, receiveBuffer);
						} else {
							//don't need to queue something that can be processed immediately
							return process(processor, messageID, receiveBuffer);
						}
					}
				}
			}
		}
		return true; //if a message is not processed, always continue processing
	}

	private void queueReceived(int messageID, int predecessorID, boolean reliable, ByteBuffer data) {
		byte[] copy = new byte[data.remaining()];
		receiveBuffer.get(copy);
		received.add(new ReceivePacket(messageID, predecessorID, reliable, copy));
	}

	private boolean process(MessageProcessor processor, int messageID, ByteBuffer receiveBuffer) {
		boolean cont = processor.process(receiveBuffer);
		headProcessedID = messageID;
		ReceivePacket packet = received.peek();
		while(cont && packet != null && packet.predecessorReliableID() <= headProcessedID) {
			received.poll();
			if(packet.messageID() > headProcessedID) {
				cont = processor.process(ByteBuffer.wrap(packet.data()));
				headProcessedID = packet.messageID();
			} //else was a duplicate
			packet = received.peek();
		}

		return cont;
	}
}

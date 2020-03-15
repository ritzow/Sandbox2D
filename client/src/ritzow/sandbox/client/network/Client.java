package ritzow.sandbox.client.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import ritzow.sandbox.data.Bytes;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;

public class Client {

	//sender/receiver state
	private final DatagramChannel channel;
	private final Queue<SendPacket> sendQueue;
	private final Map<SendPacket, Runnable> messageSentActions;
	private int receiveReliableID, receiveUnreliableID, sendReliableID, sendUnreliableID;

	private boolean isUp;
	private long ping; //rount trip time in nanoseconds

	//reliable message send state
	private int sendAttempts;
	private long sendTime;

	public interface MessageProcessor {
		void process(short messageType, ByteBuffer data);
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
	public static Client create(InetSocketAddress bindAddress,
			InetSocketAddress serverAddress) throws IOException {
		return new Client(bindAddress, serverAddress);
	}

	public Client beginConnect() {
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

	/** Queues a reliable message
	 * @param data the packet message, including message type, to send **/
	public void sendReliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), true));
	}

	/** Queues a reliable message, and runs an action after the message is received by the server
	 * @param data the packet message, including message type, to send
	 * @param action an action to run when the message is acknowledged by the server **/
	public void sendReliable(byte[] data, Runnable action) {
		SendPacket packet = new SendPacket(data.clone(), true);
		sendQueue.add(packet);
		messageSentActions.put(packet, action);
	}

	/** Queues an unreliable message
	 * @param data the packet message, including message type, to send **/
	public void sendUnreliable(byte[] data) {
		sendQueue.add(new SendPacket(data.clone(), false));
	}

	/**
	 * Abruptly close the client without notifying a connected server
	 * @throws IOException if an exception occurs when closing the networking socket
	 */
	public void close() throws IOException {
		isUp = false;
		channel.close();
	}

	public void startClose() {
		isUp = false;
	}

	private static record SendPacket(byte[] data, boolean reliable) {
		@Override
		public String toString() {
			return "SendPacket[" + (reliable ? "reliable" : "unreliable") + ", size:" + data.length + "]";
		}
	}

	/** Buffers for sending packets, receiving packets, and sending acknowledgements **/
	private final ByteBuffer
		sendBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE),
		receiveBuffer = ByteBuffer.allocateDirect(Protocol.MAX_PACKET_SIZE),
		responseBuffer = ByteBuffer.allocateDirect(Protocol.HEADER_SIZE);

	public void update(MessageProcessor processor) {
		update(Integer.MAX_VALUE, processor);
	}

	public void update(int maxReceive, MessageProcessor processor) {
		try {
			//already connected to server so no need to check SocketAddress
			while(isUp && maxReceive > 0 && channel.receive(receiveBuffer) != null) {
				receiveBuffer.flip(); //flip to set limit and prepare to read packet data
				processReceived(processor); //process messages from the server
				receiveBuffer.clear(); //clear to prepare for next receive
				--maxReceive;
			}

			//TODO create interface type for SendPackets that writes directly to send buffer
			if(isUp) {
				//send unreliable messages
				while(!sendQueue.isEmpty() && !sendQueue.peek().reliable) {
					setupSendBuffer(sendBuffer, Protocol.UNRELIABLE_TYPE,
						sendUnreliableID++, sendQueue.poll().data);
					channel.write(sendBuffer);
					sendBuffer.clear();
				}

				//start send/continue current reliable message
				if(!sendQueue.isEmpty()) {
					if(sendAttempts == 0) {
						setupSendBuffer(sendBuffer, Protocol.RELIABLE_TYPE,
							sendReliableID, sendQueue.peek().data);
						sendReliableInternal(sendBuffer);
						sendTime = System.nanoTime();
					} else if(Utility.resendIntervalElapsed(sendTime, sendAttempts)) {
						if(sendAttempts <= Protocol.RESEND_COUNT) {
							sendReliableInternal(sendBuffer);
						} else {
							isUp = false;
							onTimeout.run();
						}
					} //else waiting for a response
				}
			}
		} catch(IOException e) {
			isUp = false;
			onException.accept(e);
		}
	}

	private static void setupSendBuffer(ByteBuffer sendBuffer, byte type, int id, byte[] data) {
		sendBuffer.put(type).putInt(id).put(data).flip();
	}

	private void sendReliableInternal(ByteBuffer message) throws IOException {
		channel.write(message);
		sendBuffer.rewind();
		sendAttempts++;
	}

	private void sendResponse(int receivedMessageID) throws IOException {
		channel.write(responseBuffer.put(Protocol.RESPONSE_TYPE).putInt(receivedMessageID).flip());
		responseBuffer.clear();
	}

	private void processReceived(MessageProcessor processor) throws IOException {
		if(receiveBuffer.limit() >= Protocol.HEADER_SIZE) {
			byte type = receiveBuffer.get(); //type of message (RESPONSE, RELIABLE, UNRELIABLE)
			int messageID = receiveBuffer.getInt(); //received ID or messageID for ack.
			switch(type) {
				case Protocol.RESPONSE_TYPE -> {
					if(sendReliableID == messageID) {
						ping = Utility.nanosSince(sendTime);
						sendAttempts = 0;
						sendReliableID++;
						sendBuffer.clear();
						//remove the message from the send queue and execute any associated action
						var action = messageSentActions.get(sendQueue.poll());
						if(action != null) action.run();
					} //else drop the response
				}

				case Protocol.RELIABLE_TYPE -> {
					if(messageID == receiveReliableID) {
						//if the message is the next one, process it and update last message
						sendResponse(messageID);
						receiveReliableID++;
						processor.process(receiveBuffer.position(Protocol.HEADER_SIZE).getShort(), receiveBuffer);
					} else if(messageID < receiveReliableID) { //message already received
						sendResponse(messageID);
					} //else: message received too early
				}

				case Protocol.UNRELIABLE_TYPE -> {
					if(messageID >= receiveUnreliableID) {
						receiveUnreliableID = messageID + 1;
						processor.process(receiveBuffer.position(Protocol.HEADER_SIZE).getShort(), receiveBuffer);
					} //else: message is outdated
				}
			}
		}
	}
}

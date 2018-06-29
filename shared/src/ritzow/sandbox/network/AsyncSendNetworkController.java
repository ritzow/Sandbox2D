package ritzow.sandbox.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncSendNetworkController extends NetworkController {
	private BlockingQueue<Runnable> sendQueue;
	private final Thread sendThread;
	private volatile boolean exit;

	public AsyncSendNetworkController(InetSocketAddress bindAddress, MessageProcessor processor) throws IOException {
		super(bindAddress, processor);
		this.sendQueue = new LinkedBlockingQueue<>();
		(sendThread = new Thread(() -> {
			while(!exit)
				try {
					sendQueue.take().run();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}, AsyncSendNetworkController.class.getTypeName() + " Packet Sender")).start();
	}
	
	@Override
	public void stop() {
		super.stop();
		exit = true;
		try {
			sendThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void sendReliable(InetSocketAddress recipient, byte[] data, int attempts, int resendInterval) throws IOException {
		sendQueue.add(() -> {
			try {
				super.sendReliable(recipient, data, attempts, resendInterval);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

}

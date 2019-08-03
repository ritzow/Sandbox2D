package ritzow.sandbox.client.data;

import java.net.InetSocketAddress;
import java.net.SocketException;
import ritzow.sandbox.client.util.ClientUtility;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;

public class StandardClientOptions extends ClientOptions {
	
	//Rendering
	public static final boolean USE_OPENGL_4_6 = get("use_new_opengl", false, Boolean::parseBoolean);
	public static final boolean PRINT_FPS = get("fps_print", false, Boolean::parseBoolean);
	public static final long FRAME_TIME_LIMIT =
			ClientUtility.frameRateToFrameTimeNanos(get("fps_limit", -1, Integer::parseUnsignedInt));
	public static final boolean	LIMIT_FPS = FRAME_TIME_LIMIT > 0;
	
	//Networking
	public static final InetSocketAddress LAST_SERVER_ADDRESS =
			get("address_server", defaultAddress(Protocol.DEFAULT_SERVER_PORT), 
					string -> NetworkUtility.parseSocket(string, Protocol.DEFAULT_SERVER_PORT));
	public static final InetSocketAddress LAST_LOCAL_ADDRESS =
			get("address_local", defaultAddress(0), string -> NetworkUtility.parseSocket(string, 0));
	
	//Input
	public static final double SELECT_SENSITIVITY = get("scroll_sensitivity", 1.0, Double::parseDouble);
	
	private static InetSocketAddress defaultAddress(int port) {
		try {
			return new InetSocketAddress(NetworkUtility.getLoopbackAddress(), port);
		} catch(SocketException e) {
			e.printStackTrace();
			return null;
		}
	}
	

	
//	private static void configureAddresses(String[] args) throws Exception {
//	InetSocketAddress localAddress, serverAddress;
//	if(args.length > 0) {
//		switch(args[0]) {
//			case "local" -> {
//				localAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 0);
//				serverAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 
//						Protocol.DEFAULT_SERVER_PORT_UDP);
//			}
//			
//			case "public" -> {
//				serverAddress = new InetSocketAddress(InetAddress.getByName(args[1]), 
//						Protocol.DEFAULT_SERVER_PORT_UDP);
//				if(args.length > 1) {
//					localAddress = new InetSocketAddress(args[2], 0);
//				} else {
//					localAddress = NetworkUtility.socketAddress(
//							NetworkUtility.getPrimaryAddress(
//							NetworkUtility.isIPv6(serverAddress.getAddress())));
//				}
//			}
//			
//			default -> throw new UnsupportedOperationException("Unknown address type");
//		}
//	} else {
//		localAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 0);
//		serverAddress = new InetSocketAddress(NetworkUtility.getLoopbackAddress(), 
//				Protocol.DEFAULT_SERVER_PORT_UDP);
//	}
//	main(localAddress, serverAddress);
//}

}

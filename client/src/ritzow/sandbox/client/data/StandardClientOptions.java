package ritzow.sandbox.client.data;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	private static final Pattern 
		ADDRESS_PARSER = Pattern.compile("(?!\\[).+(?=\\])"),
		PORT_PARSER = Pattern.compile("(?<=\\]\\:)[0-9]+");
	
	public static final InetSocketAddress LAST_SERVER_ADDRESS =
			get("address_server", defaultAddress(Protocol.DEFAULT_SERVER_PORT_UDP), 
					string -> parseAddress(string, Protocol.DEFAULT_SERVER_PORT_UDP));
	public static final InetSocketAddress LAST_LOCAL_ADDRESS =
			get("address_local", defaultAddress(0), string -> parseAddress(string, 0));
	
	private static InetSocketAddress defaultAddress(int port) {
		try {
			return new InetSocketAddress(NetworkUtility.getLoopbackAddress(), port);
		} catch(SocketException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static InetSocketAddress parseAddress(String option, int defaultPort) {
		Matcher addressMatcher = ADDRESS_PARSER.matcher(option);
		Matcher portMatcher = PORT_PARSER.matcher(option);
		try {
			return new InetSocketAddress(
				addressMatcher.find() ? InetAddress.getByName(addressMatcher.group()) : null,
				portMatcher.find() 	  ? Integer.parseUnsignedInt(portMatcher.group()) : defaultPort
			);
		} catch(UnknownHostException e) {
			throw new RuntimeException("Illegal adddress", e);
		} catch(NumberFormatException e) {
			throw new RuntimeException("Unreadable port number", e);
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

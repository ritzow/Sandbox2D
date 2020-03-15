package ritzow.sandbox.client.data;

import java.net.InetSocketAddress;
import java.net.SocketException;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.util.Utility;

import static ritzow.sandbox.client.data.ClientOptions.get;
import static ritzow.sandbox.network.NetworkUtility.parseSocket;

public class StandardClientOptions {

	//Rendering
	public static final boolean USE_OPENGL_4_6 = get("use_new_opengl", false, Boolean::parseBoolean);
	public static final boolean PRINT_FPS = get("fps_print", false, Boolean::parseBoolean);
	public static final long FRAME_TIME_LIMIT =
		get("fps_limit", 0, StandardClientOptions::frameTimeLimit).longValue();
	public static final boolean	LIMIT_FPS = FRAME_TIME_LIMIT > 0;
	public static final boolean USE_INTERNET = get("use_internet", true, Boolean::parseBoolean);

	private static long frameTimeLimit(String value) {
		return Utility.frameRateToFrameTimeNanos(Long.parseUnsignedLong(value));
	}

	//Input
	public static final double SELECT_SENSITIVITY = get("scroll_sensitivity", 1.0, Double::parseDouble);

	//Network
	private static InetSocketAddress LOCAL_ADDRESS, SERVER_ADDRESS;

	public static InetSocketAddress getServerAddress() {
		return SERVER_ADDRESS == null ? SERVER_ADDRESS =
			get("address_server", defaultAddress(Protocol.DEFAULT_SERVER_PORT),
			string -> parseSocket(string, Protocol.DEFAULT_SERVER_PORT)) : SERVER_ADDRESS;
	}

	public static InetSocketAddress getLocalAddress() {
		return LOCAL_ADDRESS == null ? LOCAL_ADDRESS =
			get("address_local", defaultAddress(NetworkUtility.ANY_PORT),
			string -> parseSocket(string, 0)) : LOCAL_ADDRESS;
	}

	private static InetSocketAddress defaultAddress(int port) {
		try {
			return USE_INTERNET ? NetworkUtility.createPublicSocket() :
				new InetSocketAddress(NetworkUtility.getLoopbackAddress(), port);
		} catch(SocketException e) {
			throw new RuntimeException(e);
		}
	}
}

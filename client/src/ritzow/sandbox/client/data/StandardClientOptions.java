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
}

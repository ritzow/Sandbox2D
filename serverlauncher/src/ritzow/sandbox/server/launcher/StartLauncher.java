package ritzow.sandbox.server.launcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javafx.application.Platform;
import ritzow.sandbox.server.StartServer;

public final class StartLauncher {

	public static void main(String[] args) {
		Platform.startup(LauncherController::new);
	}

	public static void runServer(InetAddress ip, int port) throws IOException {
		StartServer.startServer(new InetSocketAddress(ip, port));
	}

}

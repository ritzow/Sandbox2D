package ritzow.solomon.engine.main;

public class StartClientServer {
	public static void main(String[] args) throws java.io.IOException {
		StartServer.main(args);
		StartClient.main(args);
	}
}

package network;

public class DedicatedServer {
	public static void main(String[] args) {
		
		if(args.length > 0) {
			try {
				Integer port = new Integer(args[0]);	
				Server server = new Server(port);
				server.run();
				
			} catch(NumberFormatException e) {
				throw new UnsupportedOperationException("The first argument must be the port number");
			}
		}
		
		else {
			throw new UnsupportedOperationException("Usage: <port>");
		}
	}
}

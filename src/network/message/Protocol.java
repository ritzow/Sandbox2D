package network.message;

/**
 * Network message ID constants
 * @author Solomon Ritzow
 *
 */
public class Protocol {
	/**
	 * Client/server message protocol ID
	 */
	public static final short
		SERVER_INFO_REQUEST = 0,
		SERVER_INFO = 1,
		SERVER_CONNECT_REQUEST = 2,
		SERVER_CONNECT_ACKNOWLEDGMENT = 3,
		CLIENT_INFO = 4,
		ENTITY_UPDATE = 5;
}
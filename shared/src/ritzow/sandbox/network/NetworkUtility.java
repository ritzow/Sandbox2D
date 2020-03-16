package ritzow.sandbox.network;

import java.net.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NetworkUtility {

	public static final int ANY_PORT = 0;

	private static final Pattern
		ADDRESS_PARSER = Pattern.compile("(?!\\[).+(?=\\])"),
		PORT_PARSER = Pattern.compile("(?<=\\]\\:)[0-9]+");

	public static InetSocketAddress parseSocket(String address, int defaultPort) {
		try { //TODO handle cases: IPv4 with no brackets with/without port, IPv6 with no brackets
			Matcher addressMatcher = ADDRESS_PARSER.matcher(address);
			if(address.equalsIgnoreCase("localhost")) {
				return new InetSocketAddress(InetAddress.getLocalHost(), defaultPort);
			} else if(addressMatcher.find()) {
				Matcher portMatcher = PORT_PARSER.matcher(address);
				return new InetSocketAddress(
					InetAddress.getByName(addressMatcher.group()),
					portMatcher.find() ? Integer.parseUnsignedInt(portMatcher.group()) : defaultPort
				);
			} else {
				int colonIndex = address.indexOf(':');
				if(address.lastIndexOf(':') == colonIndex) { //IPv4 address with or without port
					if(colonIndex > -1) {
						return new InetSocketAddress(
							InetAddress.getByName(address.substring(0, colonIndex)),
							Integer.parseInt(address.substring(colonIndex + 1))
						);
					} else {
						return new InetSocketAddress(InetAddress.getByName(address), defaultPort);
					}
				} else { //IPv6 address no port
					return new InetSocketAddress(InetAddress.getByName(address), defaultPort);
				}
			}
		} catch(UnknownHostException e) {
			throw new RuntimeException("Illegal adddress", e);
		} catch(NumberFormatException e) {
			throw new RuntimeException("Unreadable port number", e);
		}
	}

	public static String formatAddress(InetSocketAddress address) {
		return "[" + address.getAddress().getHostAddress() + "]:" +
				(address.getPort() == 0 ? "any" : address.getPort());
	}

	public static ProtocolFamily protocolOf(InetAddress address) {
		if(address instanceof Inet6Address) {
			return StandardProtocolFamily.INET6;
		} else if(address instanceof Inet4Address) {
			return StandardProtocolFamily.INET;
		} else {
			throw new IllegalArgumentException("InetAddress of unknown protocol");
		}
	}

	public static boolean isIPv6(InetAddress address) {
		return address instanceof Inet6Address;
	}

	public static InetAddress getLocalAreaAddress() throws SocketException {
		return getBestAddress(NetworkUtility::filterPublicInterfaces, NetworkUtility::prioritizeLAN);
	}

	public static InetSocketAddress createLoopbackSocket() throws SocketException {
		return new InetSocketAddress(getLoopbackAddress(), ANY_PORT);
	}

	/** Returns the computer's loopback address (localhost) for same-machine communication.
	 * @return The loopback address or null.
	 * @throws SocketException if there is an error querying the loopback address. **/
	public static InetAddress getLoopbackAddress() throws SocketException {
		return getBestAddress(NetworkUtility::isLoopback, NetworkUtility::compareProtocols);
	}

	/** Returns a suitable IP address and port for communicating over the Internet.
	 * @return An internet-facing socket on any port.
	 * @throws SocketException if there is an error querying the IP address. **/
	public static InetSocketAddress createPublicSocket() throws SocketException {
		return new InetSocketAddress(getPrimaryAddress(), ANY_PORT);
	}

	/** Returns a suitable IP address if available and null if not available
	 * @return The primary network-facing IP address of this computer.
	 * @throws SocketException if there is an error querying the IP address. **/
	public static InetAddress getPrimaryAddress() throws SocketException {
		return getBestAddress(NetworkUtility::filterPublicInterfaces, NetworkUtility::compareAddresses);
	}

	private static InetAddress getBestAddress(
		Predicate<NetworkInterface> filter, Comparator<InterfaceAddress> comparator) throws SocketException {
		//get main network interface
		//get interface's addresses
		//filter out special addresses
		//get best address
		return NetworkInterface.networkInterfaces()
			.filter(filter)
			.flatMap(i -> i.getInterfaceAddresses().stream())
			.filter(NetworkUtility::isNotLinkLocal) //filter out computer-router addresses
			.min(comparator)//get the best address
			.orElseThrow()
			.getAddress();
	}

	public static Collection<InetAddress> getAllAddresses() throws SocketException {
		return NetworkInterface.networkInterfaces()
				.flatMap(NetworkInterface::inetAddresses)
				.collect(Collectors.toList());
	}

	private static int compareProtocols(InterfaceAddress a, InterfaceAddress b) {
		boolean aIs6 = isIPv6(a.getAddress()), bIs6 = isIPv6(b.getAddress());
		if(aIs6 == bIs6) {
			return 0;
		} else if(aIs6) {
			return -1;
		} else {
			return 1;
		}
	}

	private static int prioritizeLAN(InterfaceAddress a, InterfaceAddress b) {
		throw new UnsupportedOperationException("LAN not implemented");
	}

	private static boolean isNotLinkLocal(InterfaceAddress a) {
		return !a.getAddress().isLinkLocalAddress();
	}

	//prioritize IPv6 over address scope
	private static int compareAddresses(InterfaceAddress a, InterfaceAddress b) {
		if(a.getNetworkPrefixLength() < b.getNetworkPrefixLength()) {
			return -1;
		} else if(a.getNetworkPrefixLength() > b.getNetworkPrefixLength()) {
			return 1;
		} else return compareProtocols(a, b);
	}

	private static boolean filterPublicInterfaces(NetworkInterface network) {
		try {
			return network.isUp() &&
				!(network.isLoopback() || network.isVirtual() || network.isPointToPoint());
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isLoopback(NetworkInterface network) {
		try {
			return network.isUp() && network.isLoopback();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
}

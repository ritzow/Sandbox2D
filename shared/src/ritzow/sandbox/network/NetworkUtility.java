package ritzow.sandbox.network;

import java.net.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
		Matcher addressMatcher = ADDRESS_PARSER.matcher(address);
		Matcher portMatcher = PORT_PARSER.matcher(address);
		try { //TODO handle cases: IPv4 with no brackets with/without port, IPv6 with no brackets
			if(addressMatcher.find()) {
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

	/** Returns the computer's loopback address for same-machine communication
	 * @return The loopback address or null.
	 * @throws SocketException if there is an error querying the loopback address. **/
	public static InetAddress getLoopbackAddress() throws SocketException {
		return getBestAddress(NetworkUtility::isLoopback, NetworkUtility::compareAddresses);
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
		//put best addresses first
		return NetworkInterface.networkInterfaces()
			.filter(filter)
			.map(NetworkInterface::getInterfaceAddresses)
			.flatMap(List::stream)
			.filter(address -> !address.getAddress().isLinkLocalAddress())
			.min(comparator) //get the best address
			.map(InterfaceAddress::getAddress)
			.orElseThrow();
	}

	public static Collection<InetAddress> getAllAddresses() throws SocketException {
		return NetworkInterface.networkInterfaces()
				.flatMap(NetworkInterface::inetAddresses)
				.collect(Collectors.toList());
	}

	private static int compareAddresses(InterfaceAddress a, InterfaceAddress b) {
		boolean aIs6 = isIPv6(a.getAddress());
		if(aIs6 && isIPv6(b.getAddress())) {
			//put the address with larger scope first
			return b.getNetworkPrefixLength() - a.getNetworkPrefixLength();
		} else return aIs6 ? -1 : 1;
	}

	private static boolean filterPublicInterfaces(NetworkInterface network) {
		try {
			return network.isUp() && !network.isLoopback() && !network.isVirtual();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isLoopback(NetworkInterface network) {
		try {
			return network.isLoopback();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
}

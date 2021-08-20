package ritzow.sandbox.network;

import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ritzow.sandbox.data.Bytes;

public class NetworkUtility {

	public static final int ANY_PORT = 0;

	private static final Pattern
		ADDRESS_PARSER = Pattern.compile("(?!\\[).+(?=\\])"),
		PORT_PARSER = Pattern.compile("(?<=\\]\\:)[0-9]+");

	public static InetSocketAddress parseSocket(String address, int defaultPort) {
		try { //TODO handle cases: IPv4 with no brackets with/without port, IPv6 with no brackets
			Matcher addressMatcher = ADDRESS_PARSER.matcher(address);
			if(address.equalsIgnoreCase("localhost")) {
				return new InetSocketAddress(getLoopbackAddress(), defaultPort);
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
							Integer.parseUnsignedInt(address.substring(colonIndex + 1))
						);
					} else {
						return new InetSocketAddress(InetAddress.getByName(address), defaultPort);
					}
				} else { //IPv6 address no port
					return new InetSocketAddress(InetAddress.getByName(address), defaultPort);
				}
			}
		} catch(UnknownHostException e) {
			throw new RuntimeException("Couldn't resolve adddress: " + e.getMessage(), e);
		} catch(NumberFormatException e) {
			throw new RuntimeException("Unreadable port number: " + e.getMessage(), e);
		} catch(SocketException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns whether the address is in a global unicast address range or can be used for public networking.
	 * Based on data from the IANA.
	 * IPv6: https://www.iana.org/assignments/ipv6-address-space/ipv6-address-space.xhtml
	 * IPv4: https://www.iana.org/assignments/ipv4-address-space/ipv4-address-space.xhtml
	 * @param address The address to check.
	 * @return whether the address is a global unicast IPv4 or IPv6 address.
	 */
	public static boolean isGlobalUnicastOrExternal(InetAddress address) {
		if(isIPv6(address)) {
			short high = Bytes.getShort(address.getAddress(), 0);
			return high >= 0x2000 && high <= 0x3fff;
		} else {
			short high = Bytes.getUnsignedByte(address.getAddress(), 0);
			return high < 224 && switch(high) {
				default -> true;
				case 0, 10, 127 -> false;
			};
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

		/*
		* return switch(address) {
			case Inet4Address v4 -> StandardProtocolFamily.INET;
			case Inet6Address v6 -> StandardProtocolFamily.INET6;
			default -> throw new IllegalArgumentException("InetAddress of unknown protocol");
		}; */
	}

	public static boolean isIPv6(InetAddress address) {
		return address instanceof Inet6Address;
	}

	/**
	 * Returns a suitable IP address and port for communicating over the Internet.
	 * @return An internet-facing socket on on port {@code port}.
	 **/
	public static InetSocketAddress getPublicSocket(int port) throws UnknownHostException {
		return new InetSocketAddress(port);
		//return new InetSocketAddress(getPrimaryAddress(), port);
	}

	/**
	 * Returns a suitable IP address and port for communicating over the Internet.
	 * @return An internet-facing socket on any port.
	 **/
	public static InetSocketAddress getPublicSocket() {
		return new InetSocketAddress(ANY_PORT);
		//return getPublicSocket(ANY_PORT);
	}

	/**
	 * Returns a suitable IP address if available and null if not available
	 * @return The primary network-facing IP address of this computer.
	 **/
	public static InetAddress getPrimaryAddress() throws UnknownHostException {
		return InetAddress.getByAddress(new byte[16]);
//		return NetworkInterface.networkInterfaces()
//			.flatMap(i -> i.getInterfaceAddresses().stream())
//			.filter(addr -> isGlobalUnicastOrExternal(addr.getAddress()))
//			.min(NetworkUtility::compare)
//			.map(InterfaceAddress::getAddress)
//			.orElseThrow();
	}

	private static float getScopeRatio(short prefixLength, boolean IPv6) {
		return prefixLength / (IPv6 ? 128f : 32f);
	}

	private static int compare(InterfaceAddress a, InterfaceAddress b) {
		boolean aIs6 = isIPv6(a.getAddress()), bIs6 = isIPv6(b.getAddress());
		if(aIs6 == bIs6) {
			//TODO comparing network prefix length is silly and doesn't get better results
			return Float.compare(getScopeRatio(b.getNetworkPrefixLength(), bIs6), getScopeRatio(a.getNetworkPrefixLength(), aIs6));
		} else if(aIs6) {
			return -1;
		} else {
			return 1;
		}
	}

	public static InetSocketAddress getLoopbackSocket(int port) throws SocketException {
		return new InetSocketAddress(getLoopbackAddress(), port);
	}

	public static InetSocketAddress getLoopbackSocket() throws SocketException {
		return getLoopbackSocket(ANY_PORT);
	}

	/**
	 * Returns the computer's loopback address (localhost) for same-machine communication.
	 * @return The loopback address or null.
	 * @throws SocketException if there is an error querying the loopback address.
	 **/
	public static InetAddress getLoopbackAddress() throws SocketException {
		return NetworkInterface.networkInterfaces()
			.flatMap(NetworkInterface::inetAddresses)
			.filter(InetAddress::isLoopbackAddress)
			.min((a, b) -> isIPv6(a) ? isIPv6(b) ? 0 : -1 : 1)
			.orElseThrow();
	}

	public static InetAddress getLanAddress() {
		throw new UnsupportedOperationException("LAN not implemented");
	}

}

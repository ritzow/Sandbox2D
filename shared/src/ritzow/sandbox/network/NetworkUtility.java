package ritzow.sandbox.network;

import java.net.*;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NetworkUtility {
	
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

	public static InetSocketAddress getAddressFromProgramArgumentsOrDefault(String[] args, int index, InetAddress defaultAddress, int defaultPort) throws NumberFormatException, UnknownHostException {
		return new InetSocketAddress((args.length > index && !args[index].isEmpty()) ? InetAddress.getByName(args[index]) : defaultAddress,
				args.length > 1 ? Integer.parseInt(args[index + 1]) : defaultPort);
	}
	
	public static InetSocketAddress socketAddress(InetAddress address) {
		return new InetSocketAddress(address, 0);
	}
	
	public static boolean isIPv6(InetAddress address) {
		return address instanceof Inet6Address;
	}

	/** Returns the computer's loopback address for same-machine communication 
	 * @return The loopback address or null.
	 * @throws SocketException if there is an error querying the loopback address. **/
	public static InetAddress getLoopbackAddress() throws SocketException {
		return NetworkInterface.networkInterfaces()
				.filter(adapter -> {
					try {
						return adapter.isLoopback();
					} catch (SocketException e) {
						throw new RuntimeException(e);
					}
				}).findAny() //get main network interface
				.map(network -> network.getInterfaceAddresses())
				.orElseGet(Collections::emptyList).stream() //get interface's addresses
				.filter(NetworkUtility::filterAdresses) //filter out special addresses
				.sorted((a, b) -> compareAddresses(true, a, b)) //put best addresses first
				.findFirst().map(address -> address.getAddress()).get(); //get the best address
	}

	/** Returns a suitable IP address if available and null if not available 
	 * @param preferIPv6 if IPv6 should be used when possible.
	 * @return The primary network-facing IP address of this computer.
	 * @throws SocketException if there is an error querying the IP address. **/
	public static InetAddress getPrimaryAddress(boolean preferIPv6) throws SocketException {
		return NetworkInterface.networkInterfaces()
			.filter(NetworkUtility::filterInterfaces).findAny() //get main network interface
			.map(network -> network.getInterfaceAddresses())
			.orElseGet(Collections::emptyList).stream() //get interface's addresses
			.filter(NetworkUtility::filterAdresses) //filter out special addresses
			.sorted((a, b) -> compareAddresses(preferIPv6, a, b)) //put best addresses first
			.findFirst().map(address -> address.getAddress()).orElseThrow(); //get the best address
	}
	
	public static Collection<InetAddress> getAllAddresses() throws SocketException {
		return NetworkInterface.networkInterfaces()
				.flatMap(NetworkInterface::inetAddresses)
				.collect(Collectors.toList());
	}

	private static int compareAddresses(boolean preferIPv6, InterfaceAddress a, InterfaceAddress b) {
		boolean aIs6 = a.getAddress() instanceof Inet6Address, bIs6 = b.getAddress() instanceof Inet6Address;
		if(aIs6 && bIs6) {
			return b.getNetworkPrefixLength() - a.getNetworkPrefixLength(); //put the address with larger scope first
		} else if(preferIPv6 && aIs6) {
			return -1;
		} else {
			return 1;
		}
	}

	private static boolean filterInterfaces(NetworkInterface network) {
		try {
			return network.isUp() && !network.isLoopback() && !network.isVirtual();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean filterAdresses(InterfaceAddress address) {
		return !address.getAddress().isLinkLocalAddress();
	}
}

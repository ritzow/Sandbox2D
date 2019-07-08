package ritzow.sandbox.network;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

public class NetworkUtility {

	public static InetSocketAddress getAddressFromProgramArgumentsOrDefault(String[] args, int index, InetAddress defaultAddress, int defaultPort) throws NumberFormatException, UnknownHostException {
		return new InetSocketAddress((args.length > index && !args[index].isEmpty()) ? InetAddress.getByName(args[index]) : defaultAddress,
				args.length > 1 ? Integer.parseInt(args[index + 1]) : defaultPort);
	}
	
	public static boolean isIPv6(InetAddress address) {
		return address instanceof Inet6Address;
	}

	/** Returns the computer's loopback address for same-machine communication **/
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

	/** Returns a suitable IP address if available and null if not available **/
	public static InetAddress getPrimaryAddress(boolean preferIPv6) throws SocketException {
		return NetworkInterface.networkInterfaces()
			.filter(NetworkUtility::filterInterfaces).findAny() //get main network interface
			.map(network -> network.getInterfaceAddresses())
			.orElseGet(Collections::emptyList).stream() //get interface's addresses
			.filter(NetworkUtility::filterAdresses) //filter out special addresses
			.sorted((a, b) -> compareAddresses(preferIPv6, a, b)) //put best addresses first
			.findFirst().map(address -> address.getAddress()).orElseThrow(); //get the best address
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

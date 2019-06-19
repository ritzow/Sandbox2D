package ritzow.sandbox.network;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class NetworkUtility {

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
				.sorted(NetworkUtility::compareAddresses) //put best addresses first
				.findFirst().map(address -> address.getAddress()).get(); //get the best address
	}

	/** Returns a suitable IP address if available and null if not available **/
	public static InetAddress getPrimaryAddress() throws SocketException {
		return NetworkInterface.networkInterfaces()
			.filter(NetworkUtility::filterInterfaces).findAny() //get main network interface
			.map(network -> network.getInterfaceAddresses())
			.orElseGet(Collections::emptyList).stream() //get interface's addresses
			.filter(NetworkUtility::filterAdresses) //filter out special addresses
			.sorted(NetworkUtility::compareAddresses) //put best addresses first
			.findFirst().map(address -> address.getAddress()).get(); //get the best address
	}

	private static int compareAddresses(InterfaceAddress a, InterfaceAddress b) {
		boolean aIs6 = a.getAddress() instanceof Inet6Address, bIs6 = b.getAddress() instanceof Inet6Address;
		if(aIs6 && bIs6) {
			return b.getNetworkPrefixLength() - a.getNetworkPrefixLength(); //put the address with larger scope first
		} else if(aIs6) {
			return -1;
		} else {
			return 1;
		}
	}

	private static boolean filterInterfaces(NetworkInterface network) {
		try {
			return network.isUp() && !network.isLoopback() && !network.isVirtual();
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean filterAdresses(InterfaceAddress address) {
		return !address.getAddress().isLinkLocalAddress();
	}
}

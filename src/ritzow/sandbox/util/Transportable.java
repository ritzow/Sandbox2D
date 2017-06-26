package ritzow.sandbox.util;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Instances of Transportable can be passed to ByteUtil.serialize to convert them into byte array packages containing the data from getBytes and header information
 * such as class name and length of data. Transportable objects should also implement a public constructor that takes a byte[] as its sole argument, in order to
 * deserialize the data returned by getBytes.
 * @author Solomon Ritzow
 *
 */
public interface Transportable {
	/** @return a byte array representing the object, which can be restored exactly by calling the byte array constructor of the object **/
	public byte[] getBytes();
	
	public default void serialize(DataOutput out) throws IOException {
		out.write(getBytes());
	}
}

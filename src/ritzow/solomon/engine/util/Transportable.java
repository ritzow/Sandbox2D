package ritzow.solomon.engine.util;

/**
 * Instances of Transportable can be passed to ByteUtil.serialize to convert them into byte array packages containing the data from getBytes and header information
 * such as class name and length of data. Transportable objects should also implement a public constructor that takes a byte[] as its sole argument, in order to
 * deserialize the data returned by getBytes.
 * @author Solomon Ritzow
 *
 */
public interface Transportable {
	public byte[] getBytes();
}

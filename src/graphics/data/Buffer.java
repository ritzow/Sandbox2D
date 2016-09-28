package graphics.data;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

public abstract class Buffer {
	protected int objectID;
	public abstract void bind();
	public abstract void unbind();
	public abstract void delete();
	public abstract void buffer();
	
	protected static final IntBuffer storeArrayInBuffer(int[] array) {
		IntBuffer buffer = BufferUtils.createIntBuffer(array.length);
		
		for(int i : array) {
			buffer.put(i);
		}
		
		buffer.position(0);
		return buffer;
	}
	
	protected static final FloatBuffer storeArrayInBuffer(float[] array) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(array.length);
		
		for(float i : array) {
			buffer.put(i);
		}
		
		buffer.position(0);
		return buffer;
	}
}

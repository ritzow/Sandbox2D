package ritzow.sandbox.client.graphics;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ritzow.sandbox.util.Utility;
import java.nio.Buffer;

import org.json.*;

/** Implements a subset of https://github.com/KhronosGroup/glTF/tree/master/specification/2.0 **/
public class GLTFModelLoader {
	private static final int MAGIC_NUMBER = 0x46546C67;
	private static final int 
		CHUNK_TYPE_JSON = 0x4E4F534A,
		CHUNK_TYPE_BINARY = 0x004E4942;
	
	public static class GLTFModelData {
		public Map<String, Buffer> buffers;
	}
	
	public static class GLTFException extends IOException {
		public GLTFException() {
			super();
		}

		public GLTFException(String message, Throwable cause) {
			super(message, cause);
		}

		public GLTFException(String message) {
			super(message);
		}

		public GLTFException(Throwable cause) {
			super(cause);
		}
	}
	
	public static GLTFModelData load(Path file, PrintStream info) throws IOException {
		info.println("Loading binary glTF file " + file);
		FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
		ByteBuffer data = ByteBuffer.allocate((int)channel.size()).order(ByteOrder.LITTLE_ENDIAN);
		channel.read(data);
		data.flip();
		checkMagic(file, data.getInt());
		int version = data.getInt();
		info.println("Binary glTF version " + version);
		long length = Integer.toUnsignedLong(data.getInt());
		info.println("Total length: " + Utility.formatSize(length));
		while(data.position() < length) {
			readChunk(data, info);
		}
		return new GLTFModelData();
	}
	
	private static void checkMagic(Path file, int magicNumber) throws GLTFException {
		if(magicNumber !=  MAGIC_NUMBER)
			throw new GLTFException(file + " is not a glb file, magic number " + 
					Integer.toHexString(magicNumber) + " does not match.");
	}
	
	private static void readChunk(ByteBuffer data, PrintStream info) throws GLTFException {
		long length = Integer.toUnsignedLong(data.getInt());
		int type = data.getInt();
		switch(type) {
			case CHUNK_TYPE_JSON -> {
				info.println("JSON Chunk");
				info.println("Size: " + Utility.formatSize(length));
				if(length > Integer.MAX_VALUE)
					throw new GLTFException("chunk too large to read " + Utility.formatSize(length));
				parseJson(data, (int)length, info);
			}
			
			case CHUNK_TYPE_BINARY -> {
				info.println("Binary Chunk");
				info.println("Size: " + Utility.formatSize(length));
				long end = data.position() + length;
				while(data.position() < end) data.get();
			}
			
			default -> {
				info.println("Uknown chunk type");
				long end = data.position() + length;
				while(data.position() < end) data.get();
			}
		}
	}
	
	private static void parseJson(ByteBuffer data, int length, PrintStream info) throws GLTFException {
		byte[] json = new byte[length];
		data.get(json);
		String str = new String(json, StandardCharsets.UTF_8);
		json = null;
		var jsonData = new JSONObject(str);
		var assetInfo = jsonData.getJSONObject("asset");
		if(assetInfo == null)
			throw new GLTFException("no 'asset' section of JSON");
		info.println(assetInfo);
		List<JSONObject> buffers = getArrayContents(jsonData, "buffers");
		if(buffers.get(0).has("uri") || buffers.size() > 1)
			throw new GLTFException("external and non-binary buffers not supported");
		List<JSONObject> bufferViewInfo = getArrayContents(jsonData, "bufferViews");
		for(JSONObject bInfo : bufferViewInfo) {
			JSONObject buffer = buffers.get(bInfo.getInt("buffer"));
			int byteOffset = bInfo.getInt("byteOffset");
			int byteLength = bInfo.getInt("byteLength");
			int byteStride = bInfo.has("byteStride") ? bInfo.getInt("byteStride") : 0;
			info.println(buffer + ", byteOffset " + byteOffset + ", byteLength " + byteLength + ", stride " + byteStride);
		}
	}
	
	private static List<JSONObject> getArrayContents(JSONObject jsonData, String key) {
		List<JSONObject> contents = new ArrayList<JSONObject>();
		JSONArray array = jsonData.getJSONArray(key);
		for(int i = 0; i < array.length(); i++) {
			contents.add(array.getJSONObject(i));
		}
		return contents;
	}
}

package ritzow.sandbox.protocol;

import java.io.DataInput;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import ritzow.sandbox.util.Transportable;

public class SerialManager {
	private final Map<Short, Class<? extends Transportable>> lookup;
	private final Map<Class<? extends Transportable>, Short> idLookup;
	
	public SerialManager() {
		this.lookup = new HashMap<>();
		this.idLookup = new HashMap<>();
	}
	
	public void register(short identifier, Class<? extends Transportable> type) {
		lookup.put(identifier, type);
		idLookup.put(type, identifier);
	}
	
	public byte[] serialize(Transportable object) {
//		Objects.requireNonNull(object);
//		Short val = idLookup.get(object.getClass());
//		if(val == null)
//			throw new ClassNotRegisteredException("Could not serialize object because its class was not registered");
//		
//		if(object == null)
//			return new byte[4]; //return a name length of 0, which is interpreted by deserialize as null
//		
//		ByteArrayDataOutput data = new ByteArrayDataOutput();
//		object.serialize(data);
//		
//		//TODO implement serialization
		
		return null;
	}
	
	public Object deserialize(byte[] object) throws ReflectiveOperationException { //TODO implement actual deserialization procedure
		Class<?> clazz = lookup.get((short)((object[0] << 8) | (object[1] & 0xff)));
		if(clazz == null)
			throw new ClassNotRegisteredException("Cannot deserialize unregistered class");
		return clazz.getConstructor(DataInput.class).newInstance(new ByteArrayInput(object));
	}
}

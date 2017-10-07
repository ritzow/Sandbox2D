package ritzow.sandbox.client.input;

public class UserControl { //TODO should this be abstract instead? and use subclasses instead of type constant
	protected final int type;
	protected final int descriptor;
	
	public UserControl(int type, int descriptor) {
		super();
		this.type = type;
		this.descriptor = descriptor;
	}
}


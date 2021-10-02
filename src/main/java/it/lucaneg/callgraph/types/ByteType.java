package it.lucaneg.callgraph.types;

public class ByteType implements PrimitiveType {

	public static final ByteType INSTANCE = new ByteType();
	
	private ByteType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}
	
	@Override
	public String toString() {
		return "byte";
	}
	
	@Override
	public String toJVM() {
		return "B";
	}
}

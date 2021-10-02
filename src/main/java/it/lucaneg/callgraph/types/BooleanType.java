package it.lucaneg.callgraph.types;

public class BooleanType implements PrimitiveType {

	public static final BooleanType INSTANCE = new BooleanType();

	private BooleanType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "boolean";
	}
	
	@Override
	public String toJVM() {
		return "Z";
	}
}

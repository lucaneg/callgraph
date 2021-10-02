package it.lucaneg.callgraph.types;

public class FloatType implements PrimitiveType {

	public static final FloatType INSTANCE = new FloatType();

	private FloatType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "float";
	}

	@Override
	public String toJVM() {
		return "F";
	}
}

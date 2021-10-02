package it.lucaneg.callgraph.types;

public class IntType implements PrimitiveType {

	public static final IntType INSTANCE = new IntType();

	private IntType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "int";
	}

	@Override
	public String toJVM() {
		return "I";
	}
}

package it.lucaneg.callgraph.types;

public class VoidType implements PrimitiveType {

	public static final VoidType INSTANCE = new VoidType();

	private VoidType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "void";
	}

	@Override
	public String toJVM() {
		return "V";
	}
}

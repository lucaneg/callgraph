package it.lucaneg.callgraph.types;

public class LongType implements PrimitiveType {

	public static final LongType INSTANCE = new LongType();

	private LongType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "long";
	}

	@Override
	public String toJVM() {
		return "J";
	}
}

package it.lucaneg.callgraph.types;

public class ShortType implements PrimitiveType {

	public static final ShortType INSTANCE = new ShortType();

	private ShortType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "short";
	}

	@Override
	public String toJVM() {
		return "S";
	}
}

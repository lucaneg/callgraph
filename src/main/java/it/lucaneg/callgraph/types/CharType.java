package it.lucaneg.callgraph.types;

public class CharType implements PrimitiveType {

	public static final CharType INSTANCE = new CharType();

	private CharType() {
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}

	@Override
	public String toString() {
		return "char";
	}

	@Override
	public String toJVM() {
		return "C";
	}
}

package it.lucaneg.callgraph.types;

public class DoubleType implements PrimitiveType {

	public static final DoubleType INSTANCE = new DoubleType();
	
	private DoubleType() {
	}
	
	@Override
	public boolean isAssignableTo(Type other) {
		return other == this;
	}
	
	@Override
	public String toString() {
		return "double";
	}
	
	@Override
	public String toJVM() {
		return "D";
	}
}

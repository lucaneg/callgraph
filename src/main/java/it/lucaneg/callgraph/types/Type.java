package it.lucaneg.callgraph.types;

public interface Type {
	boolean isAssignableTo(Type other);
	
	String toJVM();
}

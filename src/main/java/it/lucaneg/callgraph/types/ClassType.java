package it.lucaneg.callgraph.types;

import java.util.HashMap;
import java.util.Map;

import it.lucaneg.callgraph.model.ClassMetadata;

public class ClassType implements ReferenceType {

	private static final Map<String, ClassType> types = new HashMap<>();
	
	public static ClassType lookup(String name, ClassMetadata clazz) {
		return types.computeIfAbsent(name, x -> new ClassType(name, clazz));
	}
	
	private final String name;
	private final ClassMetadata clazz;

	private ClassType(String name, ClassMetadata clazz) {
		this.name = name;
		this.clazz = clazz;
	}
	
	public String getName() {
		return name;
	}
	
	public ClassMetadata getUnderlyingClass() {
		return clazz;
	}
	
	@Override
	public boolean isAssignableTo(Type other) {
		return other instanceof ClassType && subclass((ClassType) other);
	}

	private boolean subclass(ClassType other) {
		return this == other || clazz.getInstances().contains(other.clazz);
	}
	

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public String toJVM() {
		return "L" + name.replace('.', '/') + ";";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClassType other = (ClassType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		return true;
	}
}

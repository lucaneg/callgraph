package it.lucaneg.callgraph.types;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class ArrayType implements ReferenceType {

	private static final Map<Pair<Type, Integer>, ArrayType> types = new HashMap<>();

	public static ArrayType lookup(Type base, int dimensions) {
		return types.computeIfAbsent(Pair.of(base, dimensions), x -> new ArrayType(base, dimensions));
	}

	private final Type base;

	private final int dimensions;

	private ArrayType(Type base, int dimensions) {
		this.base = base;
		this.dimensions = dimensions;
	}

	@Override
	public boolean isAssignableTo(Type other) {
		return other instanceof ArrayType && getInnerType().isAssignableTo(((ArrayType) other).getInnerType());
	}

	@Override
	public String toString() {
		return base + "[]".repeat(dimensions);
	}
	
	@Override
	public String toChoppedString() {
		return base.toChoppedString() + "[]".repeat(dimensions);
	}
	
	@Override
	public String toJVM() {
		return "[".repeat(dimensions) + base.toJVM();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + dimensions;
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
		ArrayType other = (ArrayType) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (dimensions != other.dimensions)
			return false;
		return true;
	}

	public Type getInnerType() {
		if (dimensions == 0)
			return base;
		return lookup(base, dimensions - 1);
	}

	public Type getBaseType() {
		return base;
	}
}

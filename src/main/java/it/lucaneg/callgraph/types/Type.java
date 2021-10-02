package it.lucaneg.callgraph.types;

import java.util.Map;

import it.lucaneg.callgraph.model.ClassMetadata;

public interface Type {
	boolean isAssignableTo(Type other);
	
	String toJVM();
	
	default String toChoppedString() {
		return toString();
	}
	
	public static Type convert(org.apache.bcel.generic.Type bcel, Map<String, ClassMetadata> availableClasses) {
		if (bcel == org.apache.bcel.generic.Type.BOOLEAN)
			return BooleanType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.BYTE)
			return ByteType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.CHAR)
			return CharType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.DOUBLE)
			return DoubleType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.FLOAT)
			return FloatType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.INT)
			return IntType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.LONG)
			return LongType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.SHORT)
			return ShortType.INSTANCE;
		else if (bcel == org.apache.bcel.generic.Type.VOID)
			return VoidType.INSTANCE;
		else if (bcel instanceof org.apache.bcel.generic.ArrayType) {
			org.apache.bcel.generic.ArrayType arr = (org.apache.bcel.generic.ArrayType) bcel;
			return ArrayType.lookup(convert(arr.getBasicType(), availableClasses), arr.getDimensions());
		} else if (bcel instanceof org.apache.bcel.generic.ObjectType) {
			org.apache.bcel.generic.ObjectType obj = (org.apache.bcel.generic.ObjectType) bcel;
			return ClassType.lookup(obj.getClassName(), availableClasses.get(obj.getClassName()));
		} else
			throw new IllegalArgumentException("No known type for " + bcel);
	}
}

package it.lucaneg.callgraph.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import it.lucaneg.callgraph.types.Type;

public class MethodMetadata implements Comparable<MethodMetadata> {

	private final String signature;
	private final String name;
	private final Type ret;
	private final Type[] params;
	private final boolean unresolved;
	private final Collection<MethodMetadata> callers;
	private final Collection<MethodMetadata> callees;
	private final Collection<MethodMetadata> unresolvedCallees;
	private final ClassMetadata clazz;
	private final boolean canBeOverridden;

	public MethodMetadata(ClassMetadata clazz, String signature, String name, Type ret, Type[] params,
			boolean canBeOverridden) {
		this(clazz, signature, name, ret, params, canBeOverridden, false);
	}

	public MethodMetadata(ClassMetadata clazz, String signature, String name, Type ret, Type[] params,
			boolean canBeOverridden, boolean unresolved) {
		this.clazz = clazz;
		this.signature = signature;
		this.name = name;
		this.ret = ret;
		this.params = params;
		this.unresolved = unresolved;
		this.callees = new HashSet<>();
		this.callers = new HashSet<>();
		this.unresolvedCallees = new HashSet<>();
		this.canBeOverridden = canBeOverridden;
	}

	public String getSignature() {
		return signature;
	}

	public ClassMetadata getDefiningClass() {
		return clazz;
	}

	public String getClassName(boolean chop) {
		if (chop && clazz.getName().contains("."))
			return clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
		return clazz.getName();
	}

	public String getName() {
		return name;
	}

	public String getReadableName() {
		return name.equals("<init>") ? getClassName(true)
				: name.equals("<clinit>") ? "clinit-" + getClassName(true) : name;
	}

	public String getRet(boolean chop) {
		if (chop)
			return ret.toChoppedString();
		return ret.toString();
	}

	public String[] getParams(boolean chop) {
		String[] chopped = new String[params.length];
		for (int i = 0; i < chopped.length; i++)
		if (chop) 
			chopped[i] = params[i].toChoppedString();
		else 
			chopped[i] = params[i].toString();
		return chopped;
	}

	public boolean isConstructor() {
		return name.equals("<init>");
	}

	public boolean isClassConstructor() {
		return name.equals("<clinit>");
	}

	public String getReadableSignature(boolean chop) {
		if (isConstructor() || isClassConstructor())
			return clazz.getName() + "." + getReadableName() + "(" + String.join(", ", getParams(chop)) + ")";
		return getRet(chop) + " " + clazz.getName() + "." + getReadableName() + "(" + String.join(", ", getParams(chop))
				+ ")";
	}

	public String getReadableSignatureWithNoClassName(boolean chop) {
		if (isConstructor() || isClassConstructor())
			return getReadableName() + "(" + String.join(", ", getParams(chop)) + ")";
		return getRet(chop) + " " + getReadableName() + "(" + String.join(", ", getParams(chop)) + ")";
	}

	public boolean isUnresolved() {
		return unresolved;
	}

	public Collection<MethodMetadata> getCallees() {
		return callees;
	}

	public Collection<MethodMetadata> getCallers() {
		return callers;
	}

	public Collection<MethodMetadata> getUnresolvedCallees() {
		return unresolvedCallees;
	}

	public Collection<MethodMetadata> getAllCallees() {
		Collection<MethodMetadata> all = new HashSet<>(callees);
		all.addAll(unresolvedCallees);
		return all;
	}

	public boolean canBeOverridden() {
		return canBeOverridden;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((signature == null) ? 0 : signature.hashCode());
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
		MethodMetadata other = (MethodMetadata) obj;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return (isUnresolved() ? "[open] " : "") + signature;
	}

	@Override
	public int compareTo(MethodMetadata o) {
		return signature.compareTo(o.signature);
	}

	private boolean hasCompatibleSignatureForOverride(MethodMetadata other) {
		if (!name.equals(other.name) || params.length != other.params.length)
			return false;
		
		if (!other.ret.isAssignableTo(ret))
			return false;
		
		for (int i = 0; i < params.length; i++)
			if (!params[i].equals(other.params[i]))
				return false;
		
		return true;
	}

	public Set<MethodMetadata> getOverriders() {
		Set<MethodMetadata> result = new HashSet<>();
		for (ClassMetadata instance : clazz.getInstances())
			for (MethodMetadata method : instance.getMethods())
				if (hasCompatibleSignatureForOverride(method))
					result.add(method);
		return result;
	}
}

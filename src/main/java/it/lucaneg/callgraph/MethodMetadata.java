package it.lucaneg.callgraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class MethodMetadata {
	private final String signature;
	private final String className;
	private final String name;
	private final String ret;
	private final String[] params;
	private final boolean unresolved;
	private final Collection<MethodMetadata> callers;
	private final Collection<MethodMetadata> callees;
	private final Collection<MethodMetadata> unresolvedCallees;

	public MethodMetadata(String signature, String className, String name, String ret, String[] params) {
		this(signature, className, name, ret, params, false);
	}

	public MethodMetadata(String signature, String className, String name, String ret, String[] params,
			boolean unresolved) {
		this.signature = signature;
		this.className = className;
		this.name = name;
		this.ret = ret;
		this.params = params;
		this.unresolved = unresolved;
		this.callees = new HashSet<>();
		this.callers = new HashSet<>();
		this.unresolvedCallees = new HashSet<>();
	}

	public String getSignature() {
		return signature;
	}

	public String getClassName() {
		return className;
	}

	public String getName() {
		return name.equals("<init>") ? "constructor" : name.equals("<clinit>") ? "class constructor" : name;
	}

	public String getRet(boolean chop) {
		if (chop && ret.contains("."))
			return ret.substring(ret.lastIndexOf('.') + 1);
		return ret;
	}

	public String[] getParams(boolean chop) {
		if (chop) {
			String[] chopped = new String[params.length];
			for (int i = 0; i < chopped.length; i++)
				chopped[i] = params[i].contains(".") ? params[i].substring(params[i].lastIndexOf('.') + 1) : params[i];

			return chopped;
		}
		return params;
	}

	public boolean isConstructor() {
		return name.equals("<init>");
	}

	public boolean isClassConstructor() {
		return name.equals("<clinit>");
	}

	public String getReadableSignature(boolean chop) {
		if (isConstructor() || isClassConstructor())
			return className + "." + getName() + "(" + String.join(", ", getParams(chop)) + ")";
		return getRet(chop) + " " + className + "." + getName() + "(" + String.join(", ", getParams(chop)) + ")";
	}

	public String getReadableSignatureWithNoClassName(boolean chop) {
		if (isConstructor() || isClassConstructor())
			return getName() + "(" + String.join(", ", getParams(chop)) + ")";
		return getRet(chop) + " " + getName() + "(" + String.join(", ", getParams(chop)) + ")";
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(params);
		result = prime * result + ((ret == null) ? 0 : ret.hashCode());
		result = prime * result + ((signature == null) ? 0 : signature.hashCode());
		result = prime * result + (unresolved ? 1231 : 1237);
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
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(params, other.params))
			return false;
		if (ret == null) {
			if (other.ret != null)
				return false;
		} else if (!ret.equals(other.ret))
			return false;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		if (unresolved != other.unresolved)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return (isUnresolved() ? "[open] " : "") + signature
				+ " [callers: {" + Arrays.toString(callers.stream().map(c -> c.getSignature()).toArray(String[]::new))
				+ "}, callees: {" + Arrays.toString(callees.stream().map(c -> c.getSignature()).toArray(String[]::new))
				+ "}, unresolved callees: {"
				+ Arrays.toString(unresolvedCallees.stream().map(c -> c.getSignature()).toArray(String[]::new)) + "}]";
	}
}

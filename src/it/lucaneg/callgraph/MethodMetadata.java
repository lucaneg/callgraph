package it.lucaneg.callgraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.bcel.classfile.Utility;

public class MethodMetadata {
	private final String signature;
	private final boolean unresolved;
	private final Collection<MethodMetadata> callers;
	private final Collection<MethodMetadata> callees;
	private final Collection<MethodMetadata> unresolvedCallees;
	
	public MethodMetadata(String signature) {
		this(signature, false);
	}
	
	public MethodMetadata(String signature, boolean unresolved) {
		this.signature = signature;
		this.unresolved = unresolved;
		this.callees = new HashSet<>();
		this.callers = new HashSet<>();
		this.unresolvedCallees = new HashSet<>();
	}

	public String getSignature() {
		return signature;
	}
	
	public String getReadableSignature() {
		int par = signature.indexOf('(');
		String name = signature.substring(0, par);
		String sig = signature.substring(par);
		return Utility.methodSignatureReturnType(sig) + " " + name + "(" + String.join(", ", Utility.methodSignatureArgumentTypes(sig)) + ")";
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
		return signature 
				+ " [callers: {" + Arrays.toString(callers.stream().map(c -> c.getSignature()).toArray(String[]::new)) + "}"
				+ ", callees: {" + Arrays.toString(callees.stream().map(c -> c.getSignature()).toArray(String[]::new)) + "}"
				+ ", unresolved callees: {" + Arrays.toString(unresolvedCallees.stream().map(c -> c.getSignature()).toArray(String[]::new)) + "}]";
	}
}

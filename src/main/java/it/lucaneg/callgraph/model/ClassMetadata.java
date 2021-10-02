package it.lucaneg.callgraph.model;

import java.util.HashSet;
import java.util.Set;

public class ClassMetadata {

	private final String name;

	private ClassMetadata superclass;

	private final Set<ClassMetadata> interfaces;

	private final Set<MethodMetadata> methods;
	
	private final Set<ClassMetadata> instances;

	public ClassMetadata(String name) {
		this.name = name;
		this.methods = new HashSet<>();
		this.interfaces = new HashSet<>();
		this.instances = new HashSet<>();
	}

	public ClassMetadata getSuperclass() {
		return superclass;
	}

	public void setSuperclass(ClassMetadata superclass) {
		this.superclass = superclass;
	}

	public String getName() {
		return name;
	}

	public Set<ClassMetadata> getInterfaces() {
		return interfaces;
	}

	public void addInterface(ClassMetadata _interface) {
		interfaces.add(_interface);
	}

	public Set<MethodMetadata> getMethods() {
		return methods;
	}

	public void addMethod(MethodMetadata method) {
		methods.add(method);
	}

	public void closeHierarchy() {
		instances.add(this);
		
		if (superclass != null)
			superclass.instances.add(this);
		
		for (ClassMetadata iface : interfaces)
			iface.instances.add(this);
	}
	
	public Set<ClassMetadata> getInstances() {
		return instances;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ClassMetadata other = (ClassMetadata) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name + (superclass != null ? " extends " + superclass : "");
	}
}

package com.megawin.embcdt;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.variableresolvers.PathVariableResolver;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

public class TestVariableResolver extends PathVariableResolver implements IDynamicVariableResolver {

	public TestVariableResolver() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getValue(String variable, IResource resource) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

}

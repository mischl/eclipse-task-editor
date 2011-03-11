/*******************************************************************************
 * Copyright (c) 2011 Sebastian Benz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Benz - initial API and implementation
 *******************************************************************************/
/*
* generated by Xtext
*/
package de.sebastianbenz.task;

import org.eclipse.xtext.junit4.IInjectorProvider;

import com.google.inject.Injector;

public class TaskPaperUiInjectorProvider implements IInjectorProvider {
	
	public Injector getInjector() {
		return de.sebastianbenz.task.ui.internal.TaskPaperActivator.getInstance().getInjector("de.sebastianbenz.task.TaskPaper");
	}
	
}
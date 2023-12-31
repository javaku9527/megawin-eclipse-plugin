package com.megawin.embcdt;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class MegawinDebugEventListener implements IDebugEventSetListener {

	public void registerDebugEventListener() {
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	public void unregisterDebugEventListener() {
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getKind() == DebugEvent.TERMINATE) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window != null) {
							IWorkbenchPage page = window.getActivePage();
							if (page != null) {
								IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench()
										.getPerspectiveRegistry();
								IPerspectiveDescriptor perspective = perspectiveRegistry
										.findPerspectiveWithId("org.eclipse.cdt.ui.CPerspective");
								if (perspective != null) {
									page.setPerspective(perspective);
								}
							}
						}
					}
				});
			}
		}
	}

}

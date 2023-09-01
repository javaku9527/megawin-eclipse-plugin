package com.megawin.plugin.app;

import org.eclipse.ui.IStartup;

public class Startup implements IStartup {

	private MegawinDebugEventListener debugEventListener;

	@Override
	public void earlyStartup() {
		System.out.println("55688");
		init();
		registerListeners();
	}

	private void init() {
		// EclipseUtils.loadLibFromJar();
	}

	private void registerListeners() {
		debugEventListener = new MegawinDebugEventListener();
		debugEventListener.registerDebugEventListener();
	}

}

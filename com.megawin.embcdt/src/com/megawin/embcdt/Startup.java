package com.megawin.embcdt;

import org.eclipse.ui.IStartup;

import com.megawin.embcdt.util.EclipseUtils;

public class Startup implements IStartup {

	private MegawinDebugEventListener debugEventListener;
	
	@Override
	public void earlyStartup() {
		init();
		registerListeners();
	}

	private void init() {
		EclipseUtils.loadLibFromJar();
		EclipseUtils.loadConfigLibFromJar();
	}

	private void registerListeners() {
		debugEventListener = new MegawinDebugEventListener();
		debugEventListener.registerDebugEventListener();
	}

}

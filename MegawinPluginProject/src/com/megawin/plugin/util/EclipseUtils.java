package com.megawin.plugin.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class EclipseUtils {

	private static final String LIB_BIN = "/lib-bin/";
	private final static String ACWRAPPER = "DbgCM.dll";
	private final static String MAIN = "main.exe";

	public static void openPopupWindow(ExecutionEvent event, String title, String message) {
		try {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			MessageDialog.openInformation(window.getShell(), title, message);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static void loadLibFromJar() {
		// we need to put both DLLs to temp dir
		String path = "AC_";
		loadLib(path, ACWRAPPER);
		loadLib(path, MAIN);
	}

	public static String getFlashMemExePath() {
		// we need to put both DLLs to temp dir
		String path = "AC_";
		return System.getProperty("java.io.tmpdir") + "/" + path + LIB_BIN + MAIN;
	}

	private static void loadLib(String path, String name) {
		try {
			// have to use a stream
			InputStream in = EclipseUtils.class.getResourceAsStream(LIB_BIN + name);
			// always write to different location
			File fileOut = new File(System.getProperty("java.io.tmpdir") + "/" + path + LIB_BIN + name);
			System.out.println("Writing dll to: " + fileOut.getAbsolutePath());
			OutputStream out = FileUtils.openOutputStream(fileOut);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println("Fail to Load");
		}
	}
}

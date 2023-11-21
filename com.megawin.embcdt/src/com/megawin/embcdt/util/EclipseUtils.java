package com.megawin.embcdt.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;

public class EclipseUtils {

	private static final String LIB_BIN = "lib-bin";
	private static final String CONFIG_BIN = "cfg-bin";
	private static final String PATH = "AC_" + File.separator;
	private final static String FLASH_WIN = "flash.exe";
	private final static String FLASH_LIN = "flash.exe";
	private final static String LIBUSB = "libusb-1.0.dll";

	public static void openPopupWindow(ExecutionEvent event, String title, String message) {
		try {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			MessageDialog.openInformation(window.getShell(), title, message);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static void loadLibFromJar() {

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			loadLib(PATH, FLASH_WIN);
			loadLib(PATH, LIBUSB);
		} else {
			loadLib(PATH, FLASH_LIN);
		}

	}

	public static void loadConfigLibFromJar() {

		try {
			Bundle bundle = Platform.getBundle("com.megawin.embcdt");
			URL url = bundle.getEntry("/" + CONFIG_BIN);

			URL fileURL = FileLocator.toFileURL(url);
			File dir = Paths.get(fileURL.toURI()).toFile();
			File[] files = dir.listFiles();
			for (File file : files) {
				// Copy each file to a different location
				InputStream in = new FileInputStream(file);

				// always write to different location
				File fileOut = new File(System.getProperty("java.io.tmpdir") + File.separator + PATH + CONFIG_BIN
						+ File.separator + file.getName());

				System.out.println("Writing file to: " + fileOut.getAbsolutePath());
				OutputStream out = FileUtils.openOutputStream(fileOut);
				IOUtils.copy(in, out);
				in.close();
				out.close();
			}
		} catch (IOException e) {
			System.out.println("Fail to Load Config");
			e.printStackTrace();
		} catch (URISyntaxException e) {
			System.out.println("111g");
			e.printStackTrace();
		}
	}

	public static String getFlashMemExePath() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return System.getProperty("java.io.tmpdir") + PATH + LIB_BIN + File.separator + FLASH_WIN;
		} else {
			return System.getProperty("java.io.tmpdir") + PATH + LIB_BIN + File.separator + FLASH_LIN;
		}
	}

	private static void loadLib(String path, String name) {
		try {
			// have to use a stream
			InputStream in = EclipseUtils.class.getResourceAsStream("/" + LIB_BIN + "/" + name);
			if (in == null) {
				// this is how we load file within editor (eg eclipse)
				in = EclipseUtils.class.getClassLoader().getResourceAsStream(LIB_BIN + "/" + name);
			}

			// always write to different location
			File fileOut = new File(
					System.getProperty("java.io.tmpdir") + File.separator + path + LIB_BIN + File.separator + name);
			System.out.println("Writing file to: " + fileOut.getAbsolutePath());
			OutputStream out = FileUtils.openOutputStream(fileOut);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println("Fail to Load Lib");
		}
	}
}

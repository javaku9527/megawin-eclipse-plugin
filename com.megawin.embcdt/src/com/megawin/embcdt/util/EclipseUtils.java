package com.megawin.embcdt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;

public class EclipseUtils {

	private static final String LIB_BIN = "lib-bin";
	private static final String CONFIG_BIN = "cfg-bin";
	private static final String PATH = "AC_" + File.separator;
	private final static String FLASH_WIN = "flash.exe";
	private final static String FLASH_LIN = "flash";
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
			loadLib(FLASH_WIN);
			loadLib(LIBUSB);
		} else {
			loadLib(FLASH_LIN);
			changeLibUmask(getDestFlashMemExePath(FLASH_LIN));
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
		} catch (IOException | URISyntaxException e) {
			System.out.println("Fail to Load Config");
			e.printStackTrace();
		}
	}

	public static String getFlashMemExePath() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return System.getProperty("java.io.tmpdir") + File.separator + PATH + LIB_BIN + File.separator + FLASH_WIN;
		} else {
			return System.getProperty("java.io.tmpdir") + File.separator + PATH + LIB_BIN + File.separator + FLASH_LIN;
		}
	}

	public static String getPullUpVoltage() throws InterruptedException {

		String configPath = getConfigPath();

		try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// 使用正規表達式進行匹配
				Pattern pattern = Pattern.compile("mlink pull_voltage (.*)");
				Matcher matcher = pattern.matcher(line);

				// 如果找到匹配的字串，印出捕捉到的值
				if (matcher.find()) {
					String value = matcher.group(1);
					System.out.println("Found: " + value);
					return value;
				}
			}
		} catch (IOException e) {
			System.out.println("config not found: " + configPath);
		}

		throw new InterruptedException("pull up voltage command not found");
	}

	public static String getMCUName() {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.embedcdt.managedbuild.packs.ui");
		return preferences.get("mega_mcu_name", "MCU_NOT_SET");
	}

	private static String getConfigPath() {
		return System.getProperty("java.io.tmpdir") + File.separator + PATH + CONFIG_BIN + File.separator + getMCUName()
				+ ".cfg";
	}

	private static String extractValue(String line, String searchString) {
		// 找到 %s 的位置
		int index = line.indexOf(searchString);
		if (index != -1) {
			// 提取 %s 的值
			return line.substring(index + searchString.length()).trim();
		}
		return null;
	}

	private static String getDestFlashMemExePath(String name) {
		return System.getProperty("java.io.tmpdir") + File.separator + PATH + LIB_BIN + File.separator + name;
	}

	private static void loadLib(String name) {
		try {
			// have to use a stream
			InputStream in = EclipseUtils.class.getResourceAsStream("/" + LIB_BIN + "/" + name);
			if (in == null) {
				// this is how we load file within editor (eg eclipse)
				in = EclipseUtils.class.getClassLoader().getResourceAsStream(LIB_BIN + "/" + name);
			}

			// always write to different location
			File fileOut = new File(getDestFlashMemExePath(name));
			System.out.println("Writing file to: " + fileOut.getAbsolutePath());
			OutputStream out = FileUtils.openOutputStream(fileOut);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println("Fail to Load Lib");
		}
	}

	private static void changeLibUmask(String filePath) {
		try {
			Path path = Paths.get(filePath);
			Set<PosixFilePermission> perms = Files.readAttributes(path, PosixFileAttributes.class).permissions();
			System.out.format("Permissions before: %s%n", PosixFilePermissions.toString(perms));

			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			perms.add(PosixFilePermission.GROUP_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.GROUP_EXECUTE);
			perms.add(PosixFilePermission.OTHERS_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
			Files.setPosixFilePermissions(path, perms);

			System.out.format("Permissions after:  %s%n", PosixFilePermissions.toString(perms));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

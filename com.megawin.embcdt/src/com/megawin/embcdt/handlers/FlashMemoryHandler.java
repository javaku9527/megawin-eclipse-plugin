package com.megawin.embcdt.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.megawin.embcdt.util.EclipseUtils;

public class FlashMemoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {

		try {
			File hexfile = getHexFile();
			String exePath = EclipseUtils.getFlashMemExePath();
			String voltage = EclipseUtils.getPullUpVoltage();
			String mcuName = EclipseUtils.getMCUName();
			
			Process process = new ProcessBuilder(exePath, hexfile.getAbsolutePath(), voltage, mcuName).start();
			String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
			// Wait for the process to finish and get the return value
			int exitCode = process.waitFor();
			if (exitCode == 0) {
				EclipseUtils.openPopupWindow(event, "Success", "Flash Target: " + hexfile.getAbsolutePath() + "\nSuccess!!!");
			} else {
				String[] logs = output.split("\n");				
				EclipseUtils.openPopupWindow(event, "Failed", "Error!! Error Code: " + exitCode + "\n" + logs[logs.length - 1]);
			}

		} catch (ExecutionException e) {
			EclipseUtils.openPopupWindow(event, "Error", "Flash Memory Fail, ERROR: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static File getHexFile() throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IPath rootPath = root.getLocation();

		File file = rootPath.toFile();
		List<File> hexFiles = recursivefindHexFiles(file);
		if (!hexFiles.isEmpty()) {
			return hexFiles.get(0);
		}

		throw new ExecutionException("Hex File Not Found");
	}

	private static List<File> recursivefindHexFiles(File directory) {
		List<File> hexFiles = new ArrayList<>();

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".hex")) {
					hexFiles.add(file);
				} else if (file.isDirectory()) {
					hexFiles.addAll(recursivefindHexFiles(file));
				}
			}
		}

		return hexFiles;
	}
}

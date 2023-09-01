package com.megawin.plugin.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.megawin.plugin.util.EclipseUtils;

public class FlashMemoryHandler2 extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {

		try {
			File f2 = test();
			EclipseUtils.openPopupWindow(event, "Success", f2.getAbsolutePath());
		} catch (ExecutionException e) {
			EclipseUtils.openPopupWindow(event, "Error", "Flash Memory Fail, ERROR: " + e.getMessage());
		}

		return null;
	}

	private static File test() throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IPath rootPath = root.getLocation();

		File file = rootPath.toFile();
		List<File> hexFiles = findHexFiles(file);
		if (!hexFiles.isEmpty()) {
			return hexFiles.get(0);
		}

		throw new ExecutionException("Hex File Not Found");
	}

	private static List<File> findHexFiles(File directory) {
		List<File> hexFiles = new ArrayList<>();

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".hex")) {
					hexFiles.add(file);
				} else if (file.isDirectory()) {
					hexFiles.addAll(findHexFiles(file));
				}
			}
		}

		return hexFiles;
	}

}
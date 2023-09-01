package com.megawin.plugin.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.ResourceUtil;

import com.megawin.plugin.util.EclipseUtils;

public class FlashMemoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		try {
			File f = getSelectedProject(event);
//			String exePath = EclipseUtils.getFlashMemExePath();
//			new ProcessBuilder(exePath, f2.getAbsolutePath()).start();
			EclipseUtils.openPopupWindow(event, "Success", "Flash Memory :" + f.getAbsolutePath() + " Success");
		} catch (ExecutionException e) {
			EclipseUtils.openPopupWindow(event, "Error", "Flash Memory Fail, ERROR: " + e.getMessage());
		}		

		return null;
	}

	private IStructuredSelection getSelection(ExecutionEvent event) throws ExecutionException {
		Object selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection == null) {
			selection = HandlerUtil.getCurrentSelectionChecked(event);
		}
		if (selection instanceof ITextSelection) {
			IEditorInput editorInput = HandlerUtil.getActiveEditorInputChecked(event);
			IResource resource = ResourceUtil.getResource(editorInput);
			if (resource != null) {
				return new StructuredSelection(resource);
			}

			resource = ResourceUtil.getFile(editorInput);
			if (resource != null) {
				return new StructuredSelection(resource);
			}
		}
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return StructuredSelection.EMPTY;
	}

	private File getSelectedProject(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = getSelection(event);
		if (selection.isEmpty()) {
			throw new ExecutionException("No Select File");
		}

		Object element = selection.toArray()[0];
		if (element instanceof IProject) {
			IProject project = (IProject) element;
			File location = project.getLocation().toFile();
			List<File> hexFiles = findHexFiles(location);
			if (!hexFiles.isEmpty()) {
				return hexFiles.get(0);
			}
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

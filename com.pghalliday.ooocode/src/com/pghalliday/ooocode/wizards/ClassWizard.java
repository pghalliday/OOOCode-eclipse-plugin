package com.pghalliday.ooocode.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

import com.pghalliday.ooocode.ClassHeader;
import com.pghalliday.ooocode.CIdentifier;
import com.pghalliday.ooocode.ClassSource;
import com.pghalliday.ooocode.UnitTestHeader;
import com.pghalliday.ooocode.UnitTestSource;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "c". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class ClassWizard extends Wizard implements INewWizard {
	private ClassWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for ClassWizard.
	 */
	public ClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new ClassWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final CIdentifier className = page.getClassName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, className.getName(), monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private void doFinish(
		String containerName,
		String className,
		IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + className, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		ClassHeader classHeader = new ClassHeader(className);
		createFile(className.concat(".h"), classHeader.getContents(), monitor, container);
		monitor.worked(1);
		ClassSource classSource = new ClassSource(className);
		createFile(className.concat(".c"), classSource.getContents(), monitor, container);
		monitor.worked(1);
		UnitTestHeader unitTestHeader = new UnitTestHeader(className);
		createFile(className.concat(".Test.h"), unitTestHeader.getContents(), monitor, container);
		monitor.worked(1);
		UnitTestSource unitTestSource = new UnitTestSource(className);
		final IFile unitTestSourceFile = createFile(className.concat(".Test.c"), unitTestSource.getContents(), monitor, container);
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, unitTestSourceFile, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}

	private IFile createFile(String fileName, String contents, IProgressMonitor monitor,
			IContainer container) throws CoreException {
		final IFile file = container.getFile(new Path(fileName));
		try {
			InputStream stream = new ByteArrayInputStream(contents.getBytes());
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		return file;
	}
	
	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "com.pghalliday.ooocode", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
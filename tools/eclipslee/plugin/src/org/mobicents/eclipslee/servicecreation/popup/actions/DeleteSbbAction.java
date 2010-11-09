/**
 *   Copyright 2005 Open Cloud Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.mobicents.eclipslee.servicecreation.popup.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.mobicents.eclipslee.servicecreation.util.EclipseUtil;
import org.mobicents.eclipslee.servicecreation.util.SbbFinder;
import org.mobicents.eclipslee.util.SLEE;
import org.mobicents.eclipslee.util.slee.xml.ant.AntProjectXML;
import org.mobicents.eclipslee.util.slee.xml.ant.AntTargetXML;
import org.mobicents.eclipslee.util.slee.xml.components.ComponentNotFoundException;
import org.mobicents.eclipslee.util.slee.xml.components.SbbXML;
import org.mobicents.eclipslee.xml.SbbJarXML;

/**
 * @author cath
 */
public class DeleteSbbAction implements IObjectActionDelegate {
	
	public DeleteSbbAction() {
		super();
	}
	
	public DeleteSbbAction(String sbbID) {
		super();
		this.sbbID = sbbID;
	}
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}
	
	public void run(IAction action) {
		
		initialize();
		if (!initialized) {
			MessageDialog.openError(new Shell(), "Error Deleting Service Building Block", getLastError());
			return;
		}
		
		// Open a confirmation dialog.
		String message = "You have chosen to delete the following service building block:\n";
		message += "\tName: " + sbb.getName() + "\n";
		message += "\tVendor: " + sbb.getVendor() + "\n";
		message += "\tVersion: " + sbb.getVersion() + "\n\n";
		message += "Really delete this service building block?";
		
		if (MessageDialog.openQuestion(new Shell(), "Confirmation", message)) {
			
			IProgressMonitor monitor = null;
			
			// Nuke the abstract class, sbb local object, usage interface, sbb aci
			
			// Remove the SBB from the SBB XML
			// Save sbb xml if sbbs remain, else nuke the sbb xml
			
			try {

				
				abstractFile.delete(true, true, monitor);
				if (localFile != null)
					localFile.delete(true, true, monitor);
				if (usageFile != null)
					usageFile.delete(true, true, monitor);
				if (aciFile != null)
					aciFile.delete(true, true, monitor);
				
				sbbJarXML.removeSbb(sbb);
				if (sbbJarXML.getSbbs().length == 0)
					xmlFile.delete(true, true, monitor);
				else
					xmlFile.setContents(sbbJarXML.getInputStreamFromXML(), true, true, monitor);

				// Remove from build file.
				// Determine the base name from the XML file.
				int index = xmlFile.getName().indexOf("-sbb-jar.xml");				
				if (index != -1) {
					String baseName = xmlFile.getName().substring(0, index);
					IFile projectFile = xmlFile.getProject().getFile("/build.xml");
					AntProjectXML projectXML = new AntProjectXML(projectFile.getContents());
					String cleanTargetName = "clean-" + baseName + "-sbb";
					String buildTargetName = "build-" + baseName + "-sbb";
					
					AntTargetXML cleanTarget = projectXML.getTarget(cleanTargetName);
					AntTargetXML buildTarget = projectXML.getTarget(buildTargetName);
					
					projectXML.getTarget("all").removeAntTarget(buildTarget);
					projectXML.getTarget("clean").removeAntTarget(cleanTarget);
					projectXML.removeTarget(cleanTarget);
					projectXML.removeTarget(buildTarget);
					
					projectFile.setContents(projectXML.getInputStreamFromXML(), true, true, monitor);					
				}
			
			} catch (Exception e) {
				MessageDialog.openError(new Shell(), "Error Deleting SBB", "An error occurred while deleting the service building block.  It must be deleted manually.");
				return;
			}
		}
	}
	
	/**
	 * Get the SBBXML data object for the current selection.
	 *
	 */
	
	private void initialize() {
		
		sbb = null;
		sbbJarXML = null;
		
		if (selection == null && selection.isEmpty()) {
			setLastError("Please select an SBB's Java or XML file first.");
			return;
		}
		
		if (!(selection instanceof IStructuredSelection)) {
			setLastError("Please select an SBB's Java or XML file first.");
			return;			
		}
		
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() > 1) {
			setLastError("This plugin only supports editing of one service building block at a time.");
			return;
		}
		
		// Get the first (and only) item in the selection.
		Object obj = ssel.getFirstElement();
		
		if (obj instanceof IFile) {
			
			ICompilationUnit unit = null;
			try {
				unit = JavaCore.createCompilationUnitFrom((IFile) obj);
			} catch (Exception e) {
				// Suppress Exception.  The next check checks for null unit.			
			}
			
			if (unit != null) { // .java file
				sbbJarXML = SbbFinder.getSbbJarXML(unit);
				if (sbbJarXML == null) {
					setLastError("Unable to find the corresponding sbb-jar.xml for this SBB.");
					return;
				}
				
				try {
					sbb = sbbJarXML.getSbb(EclipseUtil.getClassName(unit));
				} catch (org.mobicents.eclipslee.util.slee.xml.components.ComponentNotFoundException e) {
					setLastError("Unable to find the corresponding sbb-jar.xml for this SBB.");
					return;
				}
				
				// Set 'file' to the SBB XML file, not the Java file.
				xmlFile = SbbFinder.getSbbJarXMLFile(unit);
				abstractFile = SbbFinder.getSbbAbstractClassFile(unit);	
				localFile = SbbFinder.getSbbLocalObjectFile(unit);
				aciFile = SbbFinder.getSbbActivityContextInterfaceFile(unit);
				usageFile = SbbFinder.getSbbUsageInterfaceFile(unit);
				
				if (xmlFile == null) {
					setLastError("Unable to find SBB XML.");
					return;
				}
				
				if (abstractFile == null) {
					setLastError("Unable to find SBB abstract class file.");
					return;
				}
				
				
			} else {	
				IFile file = (IFile) obj;
				
				String name = SLEE.getName(sbbID);
				String vendor = SLEE.getVendor(sbbID);
				String version = SLEE.getVersion(sbbID);
				
				try {
					sbbJarXML = new SbbJarXML(file);
				} catch (Exception e) {
					setLastError("Unable to find the corresponding sbb-jar.xml for this SBB.");
					return;
				}
				try {
					sbb = sbbJarXML.getSbb(name, vendor, version);
				} catch (ComponentNotFoundException e) {
					setLastError("This SBB is not defined in this XML file.");
					return;
				}
				
				xmlFile = file;
				abstractFile = SbbFinder.getSbbAbstractClassFile(xmlFile, name, vendor, version);
				localFile = SbbFinder.getSbbLocalObjectFile(xmlFile, name, vendor, version);
				usageFile = SbbFinder.getSbbUsageInterfaceFile(xmlFile, name, vendor, version);
				aciFile = SbbFinder.getSbbActivityContextInterfaceFile(xmlFile, name, vendor, version);
				
				if (abstractFile == null) {
					setLastError("Unable to find SBB abstract class file.");
					return;
				}
			}	
		} else {
			setLastError("Unsupported object type: " + obj.getClass().toString());
			return;
		}
		
		// Initialization complete
		initialized = true;
		return;
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;	
	}
	
	private void setLastError(String error) {
		if (error == null) {
			lastError = "Success";
		} else {
			lastError = error;
		}
	}
	
	private String getLastError() {
		String error = lastError;
		setLastError(null);
		return error;
	}
	
	private ISelection selection;
	private SbbJarXML sbbJarXML;
	private SbbXML sbb;
	private IFile xmlFile;
	private IFile abstractFile, localFile, usageFile, aciFile;
	private String lastError;
	private String sbbID;
	private boolean initialized = false;
	
	
}

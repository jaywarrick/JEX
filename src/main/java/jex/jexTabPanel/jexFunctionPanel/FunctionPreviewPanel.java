package jex.jexTabPanel.jexFunctionPanel;

import guiObject.JSpacedPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import recycling.PreviewMaker;
import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;

public class FunctionPreviewPanel extends JSpacedPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	public FunctionPreviewPanel()
	{
		// Setup updating links
		// SSCenter.defaultCenter().connect(JEXStatics.emulatorPool,
		// DatabaseEmulatorPool.SIG_ObjectsChanged_NULL, this, "refresh",
		// (Class[])null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.VIEWEDENTRY, this, "refresh", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTEDOBJ, this, "refresh", (Class[]) null);
		
		setUp();
	}
	
	private void setUp()
	{
		this.setBackground(DisplayStatics.background);
		this.setLayout(new BorderLayout());
	}
	
	// //// METHODS
	public void refresh()
	{
		// Remove all
		this.removeAll();
		
		// Get the TypeName to preview
		TypeName previewTN = JEXStatics.jexManager.getSelectedObject();
		
		// Get the Entry to preview
		JEXEntry entry = JEXStatics.jexManager.getViewedEntry();
		
		// Get the JEXData to be displayed
		if(previewTN == null || entry == null)
		{
			return;
		}
		
		// First look in the emulator pool for recent function results
		// JEXData data = JEXStatics.emulatorPool.getDataForPreview(previewTN,
		// entry);
		JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(previewTN, entry);
		
		// Make a preview panel for the data
		JPanel previewPane = PreviewMaker.makePreviewPanel(data);
		this.add(previewPane, BorderLayout.CENTER);
		
		// Refresh
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	// //// ACTION
	
	public void actionPerformed(ActionEvent e)
	{   
		
	}
}

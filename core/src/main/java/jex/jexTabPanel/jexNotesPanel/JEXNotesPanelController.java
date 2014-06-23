package jex.jexTabPanel.jexNotesPanel;

import javax.swing.JPanel;

import jex.jexTabPanel.JEXTabPanelController;

public class JEXNotesPanelController extends JEXTabPanelController {
	
	public static String NAVIGATION_CHANGED = "NAVIGATION_CHANGED";
	
	// private JEXNotesPanel centerPane ;
	// private JEXNotesRightPanel rightPane;
	
	public JEXNotesPanelController()
	{
		// centerPane = new JEXNotesPanel(this);
		// rightPane = new JEXNotesRightPanel(this);
		// SSCenter.defaultCenter().connect(JEXStatics.jexManager,
		// JEXManager.DATASETS, this, "changeDetected", (Class[])null);
		
	}
	
	// ////
	// //// Reaction to Signals
	// ////
	
	public void changeDetected()
	{
		// // Send signal of model change
		// SSCenter.defaultCenter().emit(this, NAVIGATION_CHANGED,
		// (Object[])null);
	}
	
	// ////
	// //// JEXTabPanel interface
	// ////
	
	public JPanel getMainPanel()
	{
		return new JPanel();
		// return centerPane;
	}
	
	public JPanel getLeftPanel()
	{
		return null;
	}
	
	public JPanel getRightPanel()
	{
		return new JPanel();
		// return rightPane;
	}
	
	public void closeTab()
	{
		// if (rightPane != null) rightPane.deInitialize();
		// if (centerPane != null) centerPane.deInitialize();
		// centerPane = null;
		// rightPane = null;
	}
	
	public int getFixedPanelWidth()
	{
		return this.fixedPanelWidth;
	}
	
	public void setFixedPanelWidth(int width)
	{
		this.fixedPanelWidth = width;
	}
	
	public double getResizeWeight()
	{
		return this.resizeWeight;
	}
}

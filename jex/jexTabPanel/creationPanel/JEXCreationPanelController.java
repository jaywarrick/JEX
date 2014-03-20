package jex.jexTabPanel.creationPanel;

import javax.swing.JPanel;

import jex.JEXManager;
import jex.jexTabPanel.JEXTabPanelController;
import jex.statics.JEXStatics;
import signals.SSCenter;

public class JEXCreationPanelController extends JEXTabPanelController {
	
	public static String CHANGE_EXP_NAME = "";
	public static String CHANGE_EXP_INFO = "";
	public static String CHANGE_EXP_DATE = "";
	public static String CHANGE_TRAY_NAME = "";
	public static String EDIT_LEVEL = "EDIT_LEVEL";
	
	// Controllers
	private JEXCreationPanel centerPane;
	private JEXCreationRightPanel rightPane;
	
	public JEXCreationPanelController()
	{
		centerPane = new JEXCreationPanel();
		rightPane = new JEXCreationRightPanel();
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
	}
	
	// ////
	// //// EVENTS
	// ////
	
	public void navigationChanged()
	{   
		
	}
	
	// ////
	// //// JEXTabPanel interface
	// ////
	
	public JPanel getMainPanel()
	{
		return centerPane;
	}
	
	public JPanel getLeftPanel()
	{
		return null;
	}
	
	public JPanel getRightPanel()
	{
		return rightPane;
	}
	
	public void closeTab()
	{
		if(rightPane != null)
			rightPane.deInitialize();
		if(centerPane != null)
			centerPane.deInitialize();
		centerPane = null;
		rightPane = null;
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

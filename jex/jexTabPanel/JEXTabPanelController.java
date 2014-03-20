package jex.jexTabPanel;

import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import logs.Logs;
import signals.SSCenter;

public abstract class JEXTabPanelController {
	
	public static String CLOSE_TAB = "CLOSE_TAB";
	
	public static int leftPanelWidth = 200;
	public int fixedPanelWidth = 250;
	public double resizeWeight = 1.0;
	
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
	
	public static int getLeftPanelWidth()
	{
		return leftPanelWidth;
	}
	
	public static void setLeftPanelWidth(int width)
	{
		leftPanelWidth = width;
	}
	
	public abstract JPanel getMainPanel();
	
	public abstract JPanel getLeftPanel();
	
	public abstract JPanel getRightPanel();
	
	public void saveSplitPaneOptions(JSplitPane split)
	{
		java.awt.Component cleft = split.getLeftComponent();
		java.awt.Component cright = split.getRightComponent();
		Rectangle left = (cleft == null) ? null : cleft.getBounds();
		Rectangle right = (cright == null) ? null : cright.getBounds();
		if(resizeWeight == 1.0) // use the size of the right pane
		{
			this.fixedPanelWidth = (right == null) ? leftPanelWidth : right.width;
		}
		else
		// use the size of the left pane
		{
			this.fixedPanelWidth = (left == null) ? leftPanelWidth : left.width;
		}
	}
	
	public void imposeSplitPaneOptions(JSplitPane split)
	{
		java.awt.Component cleft = split.getLeftComponent();
		java.awt.Component cright = split.getRightComponent();
		Rectangle left = (cleft == null) ? null : cleft.getBounds();
		Rectangle right = (cright == null) ? null : cright.getBounds();
		
		int leftWidth = (left == null) ? leftPanelWidth : left.width;
		int rightWidth = (right == null) ? leftPanelWidth : right.width;
		
		split.setResizeWeight(this.resizeWeight);
		if(this.resizeWeight == 1.0) // set the size of the divider location to
		// produce the desired size of the RIGHT
		// pane
		{
			split.setDividerLocation(leftWidth + rightWidth - this.fixedPanelWidth);
		}
		else
		// set the size of the divider location to produce the desired size of
		// the LEFT pane
		{
			split.setDividerLocation(this.fixedPanelWidth);
		}
	}
	
	public void closeTab()
	{
		Logs.log("Emitting close signal", 1, this);
		SSCenter.defaultCenter().emit(this, CLOSE_TAB, (Object[]) null);
	}
	
}

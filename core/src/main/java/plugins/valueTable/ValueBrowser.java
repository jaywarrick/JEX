package plugins.valueTable;

import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import jex.statics.DisplayStatics;
import plugins.plugin.PlugIn;
import tables.DimTable;

public class ValueBrowser extends PlugIn {
	
	// Gui
	private Container pane;
	private MasterDisplayPane masterDisplayPane;
	private MasterControlPane masterControlPane;
	private JSplitPane main;
	
	private static final long serialVersionUID = 1L;
	
	// Controller
	private ValueBrowserController controller;
	
	// //////////////////////////////////////
	// ////////// Constructors //////////////
	// //////////////////////////////////////
	
	public ValueBrowser(ValueBrowserController controller)
	{
		super(controller);
		this.controller = controller;
		
		initializeMain();
		initializeDialog();
		
		this.setVisible(true);
	}
	
	// //////////////////////////////////////
	// //////////Initializers //////////////
	// //////////////////////////////////////
	
	private void initializeDialog()
	{
		pane = this.getContentPane();
		
		this.setBounds(100, 100, 800, 600);
		this.setDefaultCloseOperation(PlugIn.DISPOSE_ON_CLOSE);
		pane.setBackground(DisplayStatics.background);
		pane.setLayout(new BorderLayout());
		pane.add(main, BorderLayout.CENTER);
	}
	
	private void initializeMain()
	{
		this.main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.main.setResizeWeight(0);
		// this.main.setDividerLocation(250);
		this.main.setBackground(DisplayStatics.background);
	}
	
	public void setDBSelection()
	{
		TreeSet<JEXEntry> entries = new TreeSet<JEXEntry>(controller.getEntries());
		TypeName tn = controller.getTypeName();
		
		DimTable imageDimTable = controller.getValueDimTable(tn, entries.first());
		controller.getDataBrowser().setScrollBars(entries.size(), imageDimTable);
		
		this.invalidate();
		this.setTitle(tn.getName());
		controller.entryChanged();
		
		if(this.masterDisplayPane == null)
			this.masterDisplayPane = new MasterDisplayPane(controller.getTable());
		if(this.masterControlPane == null)
			this.masterControlPane = new MasterControlPane(controller.getDataBrowser());
		this.main.setLeftComponent(masterControlPane.panel());
		this.main.setRightComponent(masterDisplayPane.panel());
		
		// pane.removeAll();
		// pane.setBackground(DisplayStatics.background);
		// pane.setLayout(new BorderLayout());
		// if(this.masterDisplayPane == null) this.masterDisplayPane = new
		// MasterDisplayPane(controller.getTable());
		// pane.add(masterDisplayPane.panel(), BorderLayout.CENTER);
		
		this.validate();
		this.repaint();
	}
	
	// //////////////////////////////////////
	// ////////// Actions //////////////
	// //////////////////////////////////////
	
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
	}
	
	/**
	 * Display a panel on the glass pane
	 * 
	 * @param pane
	 * @return
	 */
	public boolean displayGlassPane(JPanel pane, boolean on)
	{
		
		if(pane == null)
		{
			Component c = this.getGlassPane();
			c.setVisible(false);
			return false;
		}
		
		if(on)
		{
			pane.setOpaque(true);
			this.setGlassPane(pane);
			pane.setVisible(true);
			return true;
		}
		else
		{
			pane.setOpaque(false);
			this.setGlassPane(pane);
			pane.setVisible(false);
			return true;
		}
		
	}
	
}

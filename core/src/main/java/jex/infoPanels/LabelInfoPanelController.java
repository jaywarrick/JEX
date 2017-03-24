package jex.infoPanels;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.HierarchyLevel;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.tnvi;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class LabelInfoPanelController extends InfoPanelController {
	
	// Signals
	public static String SELECTED_LABEL_NAME = "SELECTED_LABEL_NAME";
	// public static String SELECTED_LABEL_VALUE = "SELECTED_LABEL_VALUE" ;
	
	// Model
	private List<String> labelNames;
	private List<String> labelValues;
	private String selectedName;
	
	// private String selectedValue ;
	
	public LabelInfoPanelController()
	{
		// Signup for navigation change signals
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "availableLabelChange");
		SSCenter.defaultCenter().connectWithOneStringArgument(JEXStatics.jexManager, SELECTED_LABEL_NAME, this, "setSelectedLabelName");
		// SSCenter.defaultCenter().connectWithOneStringArgument(JEXStatics.jexManager,
		// SELECTED_LABEL_VALUE, this, "setSelectedLabelValue");
		
		// Initialize the variables
		this.initialize();
	}
	
	private void initialize()
	{
		// Set variables
		this.selectedName = null;
		// selectedValue = null;
		this.labelNames = new ArrayList<String>(0);
		this.labelValues = new ArrayList<String>(0);
		TypeName selectedLabel = JEXStatics.jexManager.getSelectedLabel();
		if(selectedLabel != null && selectedLabel.getName() != null)
		{
			this.setSelectedLabelName(selectedLabel.getName());
		}
		
	}
	
	// /// METHODS
	public void setSelectedLabelName(String labelName)
	{
		// Control for invalid labelNames
		if(labelName.equals(""))
		{
			labelName = null;
		}
		
		// Set the current label name selection
		this.selectedName = labelName;
		
		// Set the label name as the selected label
		JEXStatics.jexManager.setSelectedLabel(new TypeName(JEXData.LABEL, this.selectedName));
		
		// MAKE A LIST OF VALUES FOR THE CURRENTLY SELECTED LABEL NAME FROM THE
		// VIEWED ENTRIES
		this.makeValueList();
		
		// Refresh the gui
		this.refreshGUI();
	}
	
	public String getSelectedLabelName()
	{
		return this.selectedName;
	}
	
	// public void setSelectedLabelValue(String text)
	// {
	// // set the selected label value
	// this.selectedValue = text;
	//
	// // Refresh gui
	// refreshGUI();
	// }
	//
	// public String getSelectedLabelValue()
	// {
	// return this.selectedValue;
	// }
	
	public void availableLabelChange()
	{
		Logs.log("Available Label Change", 0, this);
		// Reset the label name list
		this.labelNames = new ArrayList<String>(0);
		
		// get the current tnvi
		tnvi TNVI = JEXStatics.jexManager.getFilteredTNVI();
		
		// grab the available label names
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> nvi = TNVI.get(JEXData.LABEL);
		
		// Add each label name to the list
		this.labelNames.add("");
		if(nvi != null)
		{
			for (String labelName : nvi.keySet())
			{
				this.labelNames.add(labelName);
			}
		}
		
		// Make the value list
		this.makeValueList();
		
		// Refresh the gui
		this.refreshGUI();
	}
	
	public void refreshGUI()
	{
		// Set a new InfoPanel
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_EXP, (Object[]) null);
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_ARR, (Object[]) null);
	}
	
	@Override
	public InfoPanel panel()
	{
		LabelInfoPanel result = new LabelInfoPanel();
		return result;
	}
	
	// /// PRIVATE METHODS
	private void makeValueList()
	{
		// IF the selectedname is null, value list is empty
		this.labelValues = new ArrayList<String>(0);
		if(this.selectedName == null)
		{
			return;
		}
		
		// Get the viewed entries
		HierarchyLevel viewedHierarchy = JEXStatics.jexManager.getViewedHierarchyLevel();
		Set<JEXEntry> entries = viewedHierarchy.getEntries();
		
		// Loop through the entries and look for entries continaing a label
		// named SELECTEDNAME
		for (JEXEntry entry : entries)
		{
			// get the tnv
			TreeMap<Type,TreeMap<String,JEXData>> tnv = entry.getDataList();
			if(tnv == null)
			{
				continue;
			}
			
			// Get the NV
			TreeMap<String,JEXData> nv = tnv.get(JEXData.LABEL);
			if(nv == null)
			{
				continue;
			}
			
			// Get the V
			JEXData data = nv.get(this.selectedName);
			if(data == null)
			{
				continue;
			}
			
			// Add the value to the list of values
			this.labelValues.add(data.getDictionaryValue());
		}
	}
	
	class LabelInfoPanel extends InfoPanel implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		// GUI
		private JButton stopDisplaying = new JButton("X");
		private JComboBox<String> labelSelector = new JComboBox<>();
		private JPanel centerPanel;
		
		private LabelInfoPanel()
		{
			this.makePanel();
		}
		
		private void makePanel()
		{
			// Make the label chooser
			String[] labelStrings = LabelInfoPanelController.this.labelNames.toArray(new String[0]);
			
			// Create the combo box
			this.labelSelector = new JComboBox<String>(labelStrings);
			this.labelSelector.setSelectedItem(LabelInfoPanelController.this.selectedName);
			this.labelSelector.addActionListener(this);
			
			// Create the label
			JLabel label = new JLabel("Display:");
			label.setForeground(DisplayStatics.intoPanelText);
			
			// Create the stop button
			this.stopDisplaying.addActionListener(this);
			
			// Make the headerpanel
			this.centerPanel = new JPanel();
			this.centerPanel.setLayout(new MigLayout("ins 0", "[60]2[fill,grow]2[25]", ""));
			this.centerPanel.setBackground(InfoPanel.centerPaneBGColor);
			this.centerPanel.add(label, "width 60,height 25!");
			this.centerPanel.add(this.labelSelector, "growx,height 25!");
			this.centerPanel.add(this.stopDisplaying, "width 25!,height 25!,wrap");
			
			// Create the label value panel
			this.makeLegend();
			
			// Make the rest of the GUI
			this.setTitle("Viewed label");
			this.setCenterPanel(this.centerPanel);
		}
		
		private void makeLegend()
		{
			// If no label is selected return null
			if(LabelInfoPanelController.this.selectedName == null)
			{
				return;
			}
			// Get the current label values
			TreeMap<String,TreeMap<String,Set<JEXEntry>>> labels = JEXStatics.jexManager.getLabels();
			
			// Get the current label selected values
			TreeMap<String,Set<JEXEntry>> VI = labels.get(LabelInfoPanelController.this.selectedName);
			if(VI == null)
			{
				return;
			}
			
			// Add each value
			for (String value : VI.keySet())
			{
				// Make the label "panel"
				JPanel labelPane = new JPanel();
				// labelPane.setLayout(new
				// BoxLayout(labelPane,BoxLayout.LINE_AXIS));
				labelPane.setLayout(new BorderLayout());
				
				// Make the color
				labelPane.setBackground(JEXStatics.labelColorCode.getColorForLabel(LabelInfoPanelController.this.selectedName, value));
				
				// Make the label value
				JLabel newLabelValue = new JLabel(" " + value);
				newLabelValue.setMaximumSize(new Dimension(100, 15));
				// newLabelValue.setForeground(Color.white);
				labelPane.add(newLabelValue);
				
				// Add it to the infopanel
				this.centerPanel.add(labelPane, "span,wrap,height 15,growx");
			}
		}
		
		// /// EVENTS
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == this.labelSelector)
			{
				String labelName = (String) this.labelSelector.getSelectedItem();
				SSCenter.defaultCenter().emit(JEXStatics.jexManager, SELECTED_LABEL_NAME, labelName);
				// setSelectedLabelName(labelName);
			}
			else if(e.getSource() == this.stopDisplaying)
			{
				SSCenter.defaultCenter().emit(JEXStatics.jexManager, SELECTED_LABEL_NAME, "");
				// setSelectedLabelName(null);
			}
		}
	}
}

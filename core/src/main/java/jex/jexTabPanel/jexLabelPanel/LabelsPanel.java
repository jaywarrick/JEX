package jex.jexTabPanel.jexLabelPanel;

import guiObject.DialogGlassPane;
import guiObject.FlatRoundedStaticButton;
import guiObject.FormGlassPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.ErrorMessagePane;
import jex.JEXManager;
import jex.YesNoMessagePane;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;

public class LabelsPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private TreeMap<String,TreeMap<String,Set<JEXEntry>>> labelList;
	private List<LabelPanelLine> panelList;
	private FlatRoundedStaticButton selectionLabel;
	private FlatRoundedStaticButton dbLabel;
	private String currentlySelected;
	
	private JLabel title;
	private JPanel headerPane;
	private JScrollPane scroll;
	private JPanel labelPane;
	
	private ArrayList<String> openPanels;
	
	/**
	 * Create the label panel list
	 * 
	 * @param parent
	 */
	public LabelsPanel()
	{
		super();
		
		// Link to the signal from a database list change
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.AVAILABLELAB, this, "objectsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SCOPE, this, "scopeChanged", (Class[]) null);
		Logs.log("Connected to database object change signal", 0, this);
		
		this.labelList = new TreeMap<String,TreeMap<String,Set<JEXEntry>>>();
		this.panelList = new ArrayList<LabelPanelLine>(0);
		this.openPanels = new ArrayList<String>(0);
		
		this.selectionLabel = new FlatRoundedStaticButton("selection");
		this.selectionLabel.background = DisplayStatics.menuBackground;
		this.selectionLabel.panel().setFont(FontUtility.defaultFonts);
		this.selectionLabel.enableUnselection(false);
		// selectionLabel.panel().setMaximumSize(new Dimension(35,15));
		this.selectionLabel.addActionListener(this);
		
		this.dbLabel = new FlatRoundedStaticButton("db");
		this.dbLabel.background = DisplayStatics.menuBackground;
		this.dbLabel.panel().setFont(FontUtility.defaultFonts);
		this.dbLabel.enableUnselection(false);
		this.dbLabel.addActionListener(this);
		
		this.title = new JLabel("LABELS");
		this.title.setFont(FontUtility.boldFont);
		this.headerPane = new JPanel();
		this.headerPane.setLayout(new MigLayout("flowx, ins 2, center", "[]5[]5[]", "[center]"));
		this.headerPane.setBackground(DisplayStatics.menuBackground);
		this.headerPane.add(this.title);
		this.headerPane.add(this.selectionLabel.panel());
		this.headerPane.add(this.dbLabel.panel());
		
		this.labelPane = new JPanel();
		this.labelPane.setBackground(DisplayStatics.lightBackground);
		this.labelPane.setLayout(new MigLayout("flowy,ins 3 3 3 3, gap 0", "[fill,grow]", "[]"));
		
		this.scroll = new JScrollPane(this.labelPane);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
		
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new BorderLayout());
		this.add(this.scroll, BorderLayout.CENTER);
		
		// this.setCentralPane(scroll);
		
		this.rebuild();
	}
	
	/**
	 * Rebuild the label list panel
	 */
	public void rebuild()
	{
		// LABEL PANEL
		this.labelPane.removeAll();
		
		if(this.labelList == null || this.labelList.size() == 0)
		{
			JLabel label = new JLabel("No labels, click on + to create new");
			label.setFont(FontUtility.italicFonts);
			
			JPanel temp = new JPanel();
			temp.setLayout(new BorderLayout());
			temp.add(label);
			temp.setPreferredSize(new Dimension(15, 15));
			this.labelPane.add(temp);
		}
		else
		{
			this.panelList = new ArrayList<LabelPanelLine>(0);
			Set<String> keys = this.labelList.keySet();
			for (String key : keys)
			{
				TreeMap<String,Set<JEXEntry>> labels = this.labelList.get(key);
				LabelPanelLine newLabelPanel = new LabelPanelLine(key, labels, this);
				
				this.panelList.add(newLabelPanel);
				this.labelPane.add(newLabelPanel, "growx");
				// labelPane.add(Box.createRigidArea(new Dimension(1,1)));
			}
		}
		this.labelPane.add(Box.createVerticalGlue());
		this.refresh();
		
		this.labelPane.invalidate();
		this.labelPane.validate();
		this.labelPane.repaint();
	}
	
	/**
	 * Refresh the labelpanel list in case the selection has changed
	 */
	public void refresh()
	{
		Logs.log("Refreshing label Panel", 2, this);
		for (LabelPanelLine o : this.panelList)
		{
			o.refresh();
		}
		
		String displayRule = JEXStatics.jexManager.getScope(this.getClass().toString());
		displayRule = (displayRule == null) ? JEXManager.FILTERED_DATABASE : displayRule;
		if(displayRule.equals(JEXManager.ALL_DATABASE))
		{
			this.selectionLabel.setPressed(false);
			this.dbLabel.setPressed(true);
		}
		if(displayRule.equals(JEXManager.FILTERED_DATABASE))
		{
			this.selectionLabel.setPressed(true);
			this.dbLabel.setPressed(false);
		}
	}
	
	/**
	 * Set the list of labels to display
	 * 
	 * @param labelList
	 */
	public void setLabelList(TreeMap<String,TreeMap<String,Set<JEXEntry>>> labelList)
	{
		this.labelList = labelList;
	}
	
	/**
	 * For visual purposes remember which panel was open
	 * 
	 * @param name
	 */
	public void openedLabelPanel(String name)
	{
		for (int k = 0, len = this.openPanels.size(); (k < len); k++)
		{
			if(name.equals(this.openPanels.get(k)))
			{
				this.openPanels.remove(k);
				return;
			}
		}
		this.openPanels.add(name);
	}
	
	/**
	 * Was the label panel open?
	 * 
	 * @param name
	 * @return true if label panel open
	 */
	public boolean isPanelOpen(String name)
	{
		for (int k = 0, len = this.openPanels.size(); (k < len); k++)
		{
			if(name.equals(this.openPanels.get(k)))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Set the currently selected object
	 * 
	 * @param tn
	 */
	public void setCurrentlySelected(String name)
	{
		if(name == null)
		{
			this.currentlySelected = name;
		}
		else if(name.equals(this.currentlySelected))
		{
			this.currentlySelected = null;
		}
		else
		{
			this.currentlySelected = name;
		}
		this.refresh();
	}
	
	/**
	 * Return the currently selected labelPanelLine
	 * 
	 * @return
	 */
	public String getCurrentlySelected()
	{
		return this.currentlySelected;
	}
	
	/**
	 * Remove selected label
	 */
	public void remove()
	{
		if(this.currentlySelected == null)
		{
			return;
		}
		
		Set<JEXEntry> selectedEntries = JEXStatics.jexManager.getSelectedEntries();
		if(selectedEntries == null || selectedEntries.size() == 0)
		{
			Logs.log("No selected entries to remove the label", 1, this);
			JEXStatics.statusBar.setStatusText("No selected entries");
			
			DialogGlassPane diagPanel = new DialogGlassPane("Warning");
			diagPanel.setSize(400, 200);
			
			ErrorMessagePane errorPane = new ErrorMessagePane("You must select the entries in which you want to remove the label");
			diagPanel.setCentralPanel(errorPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
			return;
		}
		
		Logs.log("Asking before removing the selected label", 1, this);
		
		DialogGlassPane diagPanel = new DialogGlassPane("Info");
		diagPanel.setSize(400, 200);
		
		YesNoMessagePane yesNoPane = new YesNoMessagePane("Are you sure you want to delete this data set");
		yesNoPane.callMethodOfClassOnAcceptance(this, "removeData");
		diagPanel.setCentralPanel(yesNoPane);
		
		JEXStatics.main.displayGlassPane(diagPanel, true);
	}
	
	public void removeData()
	{
		Logs.log("Removing the selected object", 1, this);
		
		Set<JEXEntry> selectedEntries = JEXStatics.jexManager.getSelectedEntries();
		TreeMap<JEXEntry,Set<JEXData>> dataArray = new TreeMap<JEXEntry,Set<JEXData>>();
		for (JEXEntry entry : selectedEntries)
		{
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, this.currentlySelected), entry);
			HashSet<JEXData> datas = new HashSet<JEXData>();
			datas.add(data);
			dataArray.put(entry, datas);
		}
		JEXStatics.jexDBManager.removeDataListFromEntry(dataArray);
	}
	
	public void duplicateLabel(FormGlassPane formPane)
	{
		if(formPane == null)
		{
			return;
		}
		
		// Get the form
		Logs.log("Duplicating the label", 1, this);
		LinkedHashMap<String,String> form = (LinkedHashMap<String,String>) formPane.form();
		
		// get the new name and info
		String objectName = form.get("Label name");
		String objectInfo = form.get("Label info");
		
		// make the objects
		Set<JEXEntry> selectedEntries = JEXStatics.jexManager.getSelectedEntries();
		TypeName selectedTN = JEXStatics.jexManager.getSelectedLabel();
		TreeMap<JEXEntry,JEXData> dataArray = new TreeMap<JEXEntry,JEXData>();
		for (JEXEntry entry : selectedEntries)
		{
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(selectedTN, entry);
			JEXData newData = new JEXData(data);
			newData.setDataObjectName(objectName);
			newData.setDataObjectInfo(objectInfo);
			newData.setDataObjectDate(miscellaneous.DateUtility.getDate());
			newData.setDataObjectModifDate(miscellaneous.DateUtility.getDate());
			
			dataArray.put(entry, newData);
		}
		
		// Make the new objects
		// JEXStatics.jexManager.addDataToEntries(dataArray, true);
		JEXStatics.jexDBManager.saveDataInEntries(dataArray);
	}
	
	public void doAction(String actionString)
	{
		if(actionString.equals("Duplicate object"))
		{
			Logs.log("Opening a label duplication query box", 1, this);
			
			// get info on the currently selected object
			Set<JEXEntry> selectedEntries = JEXStatics.jexManager.getSelectedEntries();
			TypeName selectedTN = JEXStatics.jexManager.getSelectedLabel();
			if(selectedTN == null)
			{
				Logs.log("No label selected", 1, this);
				JEXStatics.statusBar.setStatusText("No label selected");
				return;
			}
			else if(selectedEntries.size() == 0)
			{
				Logs.log("No entry selected", 1, this);
				JEXStatics.statusBar.setStatusText("No entry selected");
				return;
			}
			
			// create a form for getting info on the new object
			String newObjectName = JEXStatics.jexDBManager.getUniqueObjectName(selectedEntries, selectedTN.getType(), selectedTN.getName());
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(selectedTN, selectedEntries.iterator().next());
			LinkedHashMap<String,String> form = new LinkedHashMap<String,String>();
			form.put("Label name", newObjectName);
			form.put("Label info", data.getDataObjectInfo());
			
			// create a form panel
			FormGlassPane formPane = new FormGlassPane(form);
			formPane.callMethodOfClassOnAcceptance(this, "duplicateLabel", new Class[] { FormGlassPane.class });
			
			// create a glass panel
			DialogGlassPane diagPanel = new DialogGlassPane("Duplicate label");
			diagPanel.setSize(400, 200);
			diagPanel.setCentralPanel(formPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.selectionLabel)
		{
			Logs.log("Show labels only from this tray", 1, this);
			String displayRule = JEXStatics.jexManager.getScope(this.getClass().toString());
			displayRule = (displayRule == null) ? JEXManager.FILTERED_DATABASE : displayRule;
			
			if(displayRule.equals(JEXManager.FILTERED_DATABASE))
			{
				return;
			}
			JEXStatics.jexManager.setScope(this.getClass().toString(), JEXManager.FILTERED_DATABASE);
		}
		if(e.getSource() == this.dbLabel)
		{
			Logs.log("Show labels only from the whole database", 1, this);
			String displayRule = JEXStatics.jexManager.getScope(this.getClass().toString());
			displayRule = (displayRule == null) ? JEXManager.FILTERED_DATABASE : displayRule;
			
			if(displayRule.equals(JEXManager.ALL_DATABASE))
			{
				return;
			}
			JEXStatics.jexManager.setScope(this.getClass().toString(), JEXManager.ALL_DATABASE);
		}
	}
	
	/**
	 * Call when object list has changed
	 */
	public void objectsChanged()
	{
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> objectList = JEXStatics.jexManager.getLabels();
		this.setLabelList(objectList);
		this.rebuild();
	}
	
	/**
	 * Call when scope setting has changed
	 */
	public void scopeChanged()
	{
		this.refresh();
	}
	
	public void diplayPanel()
	{}
	
	public void stopDisplayingPanel()
	{}
	
	public JPanel getHeader()
	{
		return this.headerPane;
	}
}

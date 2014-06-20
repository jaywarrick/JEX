package jex.jexTabPanel.jexLabelPanel;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import net.miginfocom.swing.MigLayout;
import plugins.labelManager.ColorPallet;
import plugins.labelManager.DatabaseLabelManager;
import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.DataReader.LabelReader;
import Database.Definition.Experiment;
import Database.Definition.Filter;
import Database.Definition.TypeName;

public class LabelDistributionPanel implements ActionListener {
	
	// GUI stuff
	JPanel panel;
	JPanel contents;
	DatabaseLabelManager manager = JEXStatics.labelManager;
	JButton startButton, cancelButton, saveButton;
	ColorPallet colorPallet;
	LabelerArray labeler;
	
	// Variables
	String distributingLabelName = null;
	HashMap<Point,JEXLabel> labels;
	Experiment curTray = null;
	
	public LabelDistributionPanel()
	{
		this.initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	/**
	 * Initialize the panel
	 */
	private void initialize()
	{
		this.labels = new HashMap<Point,JEXLabel>();
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new MigLayout("ins 0, center, center", "[center]", "[fill,grow,center]"));
		this.contents = new JPanel();
		this.contents.setBackground(DisplayStatics.lightBackground);
		this.contents.setLayout(new MigLayout("flowy, ins 5, center, center, width 400:400:400", "[fill,grow]", "[]0[]0[30]5[fill,grow]"));
		this.panel.add(this.contents);
		// startButton = new JButton("Start");
		// startButton.addActionListener(this);
		
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.addActionListener(this);
		
		this.saveButton = new JButton("Save");
		this.saveButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(DisplayStatics.lightBackground);
		buttonPanel.setLayout(new MigLayout("flowx, ins 0", "[fill,grow]", "[fill,grow]"));
		// buttonPanel.add(startButton,"grow");
		buttonPanel.add(this.cancelButton, "grow");
		buttonPanel.add(this.saveButton, "grow");
		
		this.colorPallet = new ColorPallet();
		
		this.labeler = new LabelerArray();
		
		this.contents.add(this.colorPallet.panel(), "growx");
		this.contents.add(this.manager.panel());
		this.contents.add(buttonPanel, "growx");
		this.contents.add(this.labeler.panel(), "grow");
		
		SSCenter.defaultCenter().connect(this.manager, DatabaseLabelManager.SIG_SelectionChanged_NULL, this, "selectionChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.manager, DatabaseLabelManager.SIG_ContentsChanged_NULL, this, "contentsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.colorPallet, ColorPallet.SIG_ColorSelected_Null, this, "colorClicked", (Class[]) null);
		SSCenter.defaultCenter().connect(this.labeler, LabelerArray.SIG_SelectAll_NULL, this, "selectAll", (Class[]) null);
		SSCenter.defaultCenter().connect(this.labeler, LabelerArray.SIG_SelectRow_INTEGER, this, "selectRow", new Class[] { Integer.class });
		SSCenter.defaultCenter().connect(this.labeler, LabelerArray.SIG_SelectCol_INTEGER, this, "selectCol", new Class[] { Integer.class });
		SSCenter.defaultCenter().connect(this.labeler, LabelerArray.SIG_SelectCell_POINT, this, "selectCell", new Class[] { Point.class });
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
		
		this.navigationChanged();
		
	}
	
	public void getCurrentLabelsInTray()
	{
		this.labels.clear();
		if(this.curTray == null)
		{
			return;
		}
		for (int x = 0; x < this.labeler.cols; x++)
		{
			for (int y = 0; y < this.labeler.rows; y++)
			{
				JEXLabel label = this.getCurrentLabelInEntry(this.curTray.get(x).get(y));
				if(label != null)
				{
					this.labels.put(new Point(x, y), label);
				}
			}
		}
	}
	
	/**
	 * Return a filter with the type name and value of the currently clicked label
	 * 
	 * @return filter
	 */
	public Filter getCurrentLabelToDrop()
	{
		JEXLabel label = this.manager.getCurrentLabel();
		if(label == null)
		{
			return null;
		}
		Filter result = new Filter(JEXData.LABEL, label.getTypeName().getName(), label.getLabelValue());
		return result;
	}
	
	public JEXLabel getCurrentLabelInEntry(JEXEntry e)
	{
		String selectedLabelName = this.manager.getCurrentLabelName();
		if(selectedLabelName == null)
		{
			return null;
		}
		JEXData currentLabel = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, selectedLabelName), e);
		if(currentLabel == null)
		{
			return null;
		}
		JEXLabel ret = new JEXLabel(LabelReader.readLabelName(currentLabel), LabelReader.readLabelValue(currentLabel), LabelReader.readLabelUnit(currentLabel));
		return ret;
	}
	
	/**
	 * Create the labels that have been droped
	 */
	private boolean createLabels()
	{
		if(this.curTray == null)
		{
			return false;
		}
		TreeMap<JEXEntry,JEXData> toAdd = new TreeMap<JEXEntry,JEXData>();
		TreeMap<JEXEntry,Set<JEXData>> toRemove = new TreeMap<JEXEntry,Set<JEXData>>();
		TreeSet<JEXData> temp;
		for (int x = 0; x < this.labeler.cols; x++)
		{
			for (int y = 0; y < this.labeler.rows; y++)
			{
				JEXEntry e = this.curTray.get(x).get(y);
				JEXLabel newL = this.labels.get(new Point(x, y));
				JEXData oldL = this.getCurrentLabelInEntry(this.curTray.get(x).get(y));
				temp = new TreeSet<JEXData>();
				temp.add(oldL);
				toRemove.put(e, temp);
				if(newL != null)
				{
					toAdd.put(e, newL);
				}
			}
		}
		boolean successful = JEXStatics.jexDBManager.removeDataListFromEntry(toRemove);
		if(!successful)
		{
			successful = JEXStatics.jexDBManager.saveDataListInEntries(toRemove, true);
			if(!successful)
			{
				JEXStatics.statusBar.setStatusText("Label creator ERROR");
			}
		}
		else
		{
			successful = JEXStatics.jexDBManager.saveDataInEntries(toAdd);
			if(successful)
			{
				JEXStatics.statusBar.setStatusText("Labels created successfully");
			}
			else
			{
				JEXStatics.statusBar.setStatusText("Label creation was aborted. No changes made.");
			}
		}
		
		return successful;
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.cancelButton)
		{
			this.cancelDistribution();
		}
		else if(e.getSource() == this.saveButton)
		{
			this.createLabels();
			this.cancelDistribution();
		}
	}
	
	public void navigationChanged()
	{
		// Clear changes
		this.labels.clear();
		
		// If tray viewed, change array view
		this.curTray = null;
		String viewedExp = JEXStatics.jexManager.getViewedExperiment();
		
		if(viewedExp != null)
		{
			TreeMap<String,Experiment> expTree = JEXStatics.jexManager.getExperimentTree();
			this.curTray = expTree.get(viewedExp);
			int width = 0, height = 0;
			for (Integer x : this.curTray.keySet())
			{
				if((x + 1) > width)
				{
					width = x + 1;
				}
				for (Integer y : this.curTray.get(x).keySet())
				{
					if((y + 1) > height)
					{
						height = y + 1;
					}
				}
			}
			if(!(width == 0 || height == 0))
			{
				this.labeler.setRowsAndCols(height, width);
				this.getCurrentLabelsInTray();
				this.labeler.setColors(this.getColorMap());
				return;
			}
		}
		
		// Else set labeler blank
		this.labeler.setBlank();
	}
	
	public void selectRow(Integer y)
	{
		// Check status of first row
		Point p = new Point(0, y);
		JEXLabel thisLabel = this.labels.get(p);
		
		JEXLabel newLabel = JEXStatics.labelManager.getCurrentLabel();
		if(newLabel == null)
		{
			return;
		}
		
		// Change row labels
		if(thisLabel == null || !thisLabel.getLabelValue().equals(newLabel.getLabelValue()))
		{
			for (int x = 0; x < this.labeler.cols; x++)
			{
				JEXLabel temp = new JEXLabel(newLabel.name, newLabel.getLabelValue(), newLabel.getLabelUnit());
				this.labels.put(new Point(x, y), temp);
			}
		}
		else
		{
			for (int x = 0; x < this.labeler.cols; x++)
			{
				this.labels.remove(new Point(x, y));
			}
		}
		this.labeler.setColors(this.getColorMap());
	}
	
	public void selectCol(Integer x)
	{
		// Check status of first col
		Point p = new Point(x, 0);
		JEXLabel thisLabel = this.labels.get(p);
		
		JEXLabel newLabel = JEXStatics.labelManager.getCurrentLabel();
		if(newLabel == null)
		{
			return;
		}
		
		// Change row labels
		if(thisLabel == null || !thisLabel.getLabelValue().equals(newLabel.getLabelValue()))
		{
			for (int y = 0; y < this.labeler.rows; y++)
			{
				JEXLabel temp = new JEXLabel(newLabel.name, newLabel.getLabelValue(), newLabel.getLabelUnit());
				this.labels.put(new Point(x, y), temp);
			}
		}
		else
		{
			for (int y = 0; y < this.labeler.rows; y++)
			{
				this.labels.remove(new Point(x, y));
			}
		}
		this.labeler.setColors(this.getColorMap());
	}
	
	public void selectAll()
	{
		// Check status of first cell
		Point p = new Point(0, 0);
		JEXLabel thisLabel = this.labels.get(p);
		
		JEXLabel newLabel = JEXStatics.labelManager.getCurrentLabel();
		if(newLabel == null)
		{
			return;
		}
		
		// Change labels
		if(thisLabel == null || !thisLabel.getLabelValue().equals(newLabel.getLabelValue()))
		{
			for (int y = 0; y < this.labeler.rows; y++)
			{
				for (int x = 0; x < this.labeler.cols; x++)
				{
					JEXLabel temp = new JEXLabel(newLabel.name, newLabel.getLabelValue(), newLabel.getLabelUnit());
					this.labels.put(new Point(x, y), temp);
				}
			}
		}
		else
		{
			this.labels.clear();
		}
		this.labeler.setColors(this.getColorMap());
	}
	
	public void selectCell(Point p)
	{
		// Check status of cell
		JEXLabel thisLabel = this.labels.get(p);
		
		JEXLabel newLabel = JEXStatics.labelManager.getCurrentLabel();
		
		if(newLabel == null)
		{
			return;
		}
		
		// Change label
		if(thisLabel == null || !thisLabel.getLabelValue().equals(newLabel.getLabelValue()))
		{
			JEXLabel temp = new JEXLabel(newLabel.name, newLabel.getLabelValue(), newLabel.getLabelUnit());
			this.labels.put(p, temp);
		}
		else
		{
			this.labels.remove(p);
		}
		this.labeler.setColors(this.getColorMap());
	}
	
	public HashMap<Point,Color> getColorMap()
	{
		HashMap<Point,Color> ret = new HashMap<Point,Color>();
		for (Point p : this.labels.keySet())
		{
			ret.put(p, JEXStatics.labelColorCode.getColorForLabel(this.labels.get(p)));
		}
		return ret;
	}
	
	public void selectionChanged()
	{
		JEXLabel currentSelectedLabel = this.manager.getCurrentLabel();
		String currentSelectedLabelName = this.manager.getCurrentLabelName();
		// Cover case of nothing selected or only a label name selected
		if(currentSelectedLabel == null || currentSelectedLabelName == null)
		{
			this.cancelDistribution();
		}
		else
		// Cover case of a label value selected
		{
			// Determine and set the appropriate distribution mode
			// cancel the current label distribution or continue it if adding
			// labels of a different or similar name
			if(currentSelectedLabelName == null || !currentSelectedLabelName.equals(this.distributingLabelName))
			{
				this.cancelDistribution();
			}
			
			// Update the color pallet selection
			String csvColor = this.manager.getColorString();
			this.colorPallet.setSelectedColor(csvColor);
			this.labeler.setColors(this.getColorMap());
			
		}
		
		//
		// // Cover case of a label value selected
		//
		//
		//
		// // Get currently selected label information
		// //JEXLabel selectedLabel = this.manager.getCurrentLabel();
		// String selectedLabelName = this.manager.getCurrentLabelName();
		// // if(selectedLabel != null)
		// // {
		// // selectedLabelName = selectedLabel.getTypeName().getName();
		// // }
		// //this.distributingLabelName = this.manager.getCurrentLabelName();
		// if(distributingLabelName == null)
		// {
		// distributingLabelName = selectedLabelName;
		// this.getCurrentLabelsInTray();
		// this.labeler.setColors(this.getColorMap());
		// }
		//
		// // Determine and set the appropriate distribution mode
		// // cancel the current label distribution or continue it if adding
		// labels of a different or similar name
		// if(selectedLabelName == null ||
		// !selectedLabelName.equals(distributingLabelName))
		// {
		// this.cancelDistribution();
		// }
		//
		// // Update the color pallet selection
		// String csvColor = this.manager.getColorString();
		// this.colorPallet.setSelectedColor(csvColor);
		// this.labeler.setColors(this.getColorMap());
	}
	
	public void contentsChanged()
	{
		// Cancel distribution
		this.cancelDistribution();
	}
	
	private void cancelDistribution()
	{
		this.getCurrentLabelsInTray();
		this.distributingLabelName = this.manager.getCurrentLabelName();
		this.getCurrentLabelsInTray();
		this.labeler.setColors(this.getColorMap());
		// Update the color pallet selection
		String csvColor = this.manager.getColorString();
		this.colorPallet.setSelectedColor(csvColor);
		this.labeler.setColors(this.getColorMap());
	}
	
	public void colorClicked()
	{
		// Get the color
		Color color = this.colorPallet.getSelectedColor();
		
		// Set the color
		this.manager.setColorForSelection(color);
		this.labeler.setColors(this.getColorMap());
	}
}

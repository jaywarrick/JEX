package plugins.labelManager;

import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXLabel;
import Database.SingleUserDatabase.JEXDBInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import preferences.XPreferences;
import signals.SSCenter;

/* ListDemo.java requires no other files. */
public class DatabaseLabelManager {
	
	private XPrefListManager labelNames, labelValues;
	private JPanel panel;
	private JEXLabel currentLabel;
	private XPreferences labels;
	
	public static final String SIG_ContentsChanged_NULL = "SIG_ContentsChanged_NULL";
	public static final String SIG_SelectionChanged_NULL = "SIG_SelectionChanged_NULL";
	
	public DatabaseLabelManager()
	{
		this.panel = new JPanel(new BorderLayout());
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new MigLayout("flowx, ins 0", "[fill,grow]0[fill,grow]", "[fill,grow]"));
		
		currentLabel = null;
		
		labelNames = new XPrefListManager("Label Names");
		
		labelValues = new XPrefListManager("Label Values");
		
		this.panel.add(labelNames.panel(), "grow");
		this.panel.add(labelValues.panel(), "grow");
		
		SSCenter.defaultCenter().connect(labelNames, XPrefListManager.SIG_ValueSelected_NULL, this, "_labelSelected", (Class[]) null);
		SSCenter.defaultCenter().connect(labelValues, XPrefListManager.SIG_ValueSelected_NULL, this, "_valueSelected", (Class[]) null);
		SSCenter.defaultCenter().connect(labelNames, XPrefListManager.SIG_NoValueSelected_NULL, this, "_noLabelSelected", (Class[]) null);
		SSCenter.defaultCenter().connect(labelValues, XPrefListManager.SIG_NoValueSelected_NULL, this, "_noValueSelected", (Class[]) null);
		SSCenter.defaultCenter().connect(labelNames, XPrefListManager.SIG_AddClicked_NULL, this, "_addLabelName", (Class[]) null);
		SSCenter.defaultCenter().connect(labelValues, XPrefListManager.SIG_AddClicked_NULL, this, "_addLabelValue", (Class[]) null);
		SSCenter.defaultCenter().connect(labelNames, XPrefListManager.SIG_RemoveClicked_NULL, this, "_removeLabelName", (Class[]) null);
		SSCenter.defaultCenter().connect(labelValues, XPrefListManager.SIG_RemoveClicked_NULL, this, "_removeLabelValue", (Class[]) null);
		SSCenter.defaultCenter().connect(labelNames, XPrefListManager.SIG_RenameClicked_NULL, this, "_renameLabelName", (Class[]) null);
		SSCenter.defaultCenter().connect(labelValues, XPrefListManager.SIG_RenameClicked_NULL, this, "_renameLabelValue", (Class[]) null);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void setLabels(XPreferences labels)
	{
		this.labels = labels;
		this.labelNames.setModel(labels);
		SSCenter.defaultCenter().emit(this, SIG_ContentsChanged_NULL, (Object[]) null);
	}
	
	public void _labelSelected()
	{
		String labelName = this.labelNames.getCurrentSelection();
		if(labelName == null || !labels.hasChildNode(labelName))
		{
			Logs.log("Selected label didn't exist in DBInfo!", 0, this);
			return;
		}
		XPreferences label = labels.getChildNode(labelName);
		this.labelValues.setModel(label);
		SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
	}
	
	public void _valueSelected()
	{
		String name = this.labelNames.getCurrentSelection();
		String value = this.labelValues.getCurrentSelection();
		this.currentLabel = new JEXLabel(name, value, "");
		SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
	}
	
	public void _noLabelSelected()
	{
		this.labelValues.setModel(new XPreferences());
		this.currentLabel = null;
		SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
	}
	
	public void _noValueSelected()
	{
		this.currentLabel = null;
		SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
	}
	
	public void labelsChanged()
	{
		String name = this.labelNames.getCurrentSelection();
		String value = this.labelValues.getCurrentSelection();
		if(name == null || value == null)
			this.currentLabel = null;
		else
			this.currentLabel = new JEXLabel(name, value, "");
		SSCenter.defaultCenter().emit(this, SIG_ContentsChanged_NULL, (Object[]) null);
	}
	
	public JEXLabel getCurrentLabel()
	{
		return this.currentLabel;
	}
	
	public String getCurrentLabelName()
	{
		return this.labelNames.getCurrentSelection();
	}
	
	public void setColorForSelection(Color newColor)
	{
		JEXLabel label = this.getCurrentLabel();
		if(label == null)
			return;
		XPreferences labelNode = this.labels.getChildNode(label.getTypeName().getName());
		XPreferences valueNode = labelNode.getChildNode(label.getLabelValue());
		valueNode.put(JEXDBInfo.DB_LABELCOLORCODE_COLOR, ColorPallet.colorToString(newColor));
		JEXStatics.labelColorCode.setColorForLabel(label.getTypeName().getName(), label.getLabelValue(), newColor);
	}
	
	public String getColorString()
	{
		if(this.currentLabel == null)
			return null;
		String nameStr = this.currentLabel.getTypeName().getName();
		String valueStr = this.currentLabel.getFirstSingle().get(JEXDataSingle.VALUE);
		String colorStr = null;
		XPreferences labels = JEXStatics.jexManager.getDatabaseInfo().getLabels();
		XPreferences label, value;
		if(labels.hasChildNode(nameStr))
			label = labels.getChildNode(nameStr);
		else
			return null;
		if(label.hasChildNode(valueStr))
			value = label.getChildNode(valueStr);
		else
			return null;
		if(value.has(JEXDBInfo.DB_LABELCOLORCODE_COLOR))
		{
			colorStr = value.get(JEXDBInfo.DB_LABELCOLORCODE_COLOR);
			return colorStr;
		}
		else
			return null;
	}
	
	// ////////////////////////////////////////
	// ////////// Label NAMES ////////////////
	// ////////////////////////////////////////
	
	// Adding LabelName //////////////////////////////////////////
	public void _addLabelName()
	{
		String text = this.labelNames.getTextField();
		int newIndex = this._addLabelName(text);
		labelNames.setSelectedIndex(newIndex, labelNames.getCurrentSelectionIndex());
		this.labelsChanged();
	}
	
	private int _addLabelName(String text)
	{
		if(labelNames.getModel().hasChildNode(text))
			return -1;
		int index = this.labelNames.size();
		this.labelNames.getModel().getChildNode(text); // Adds node if not there
		// already
		this.labelNames.getListModel().fireIntervalAdded(index);
		return index;
	}
	
	// Removing LabelName //////////////////////////////////////////
	public void _removeLabelName()
	{
		int currentIndex = this.labelNames.getCurrentSelectionIndex();
		String text = this.labelNames.getCurrentSelection();
		int newIndex = this._removeLabelName(text);
		labelNames.setSelectedIndex(newIndex, currentIndex);
		this.labelsChanged();
	}
	
	private int _removeLabelName(String text)
	{
		if(!labelNames.getModel().hasChildNode(text))
			return -1;
		int index = this.labelNames.getCurrentSelectionIndex();
		this.labelNames.getModel().removeNode(text);
		this.labelNames.getListModel().fireIntervalRemoved(index);
		return index - 1;
	}
	
	// Renaming LabelName //////////////////////////////////////////
	public void _renameLabelName()
	{
		String oldText = this.labelNames.getCurrentSelection();
		String newText = this.labelNames.getTextField();
		int newIndex = this._renameLabelName(oldText, newText);
		labelNames.setSelectedIndex(newIndex, 0);
		this.labelsChanged();
	}
	
	private int _renameLabelName(String oldText, String newText)
	{
		if(!this.labelNames.getModel().hasChildNode(oldText) || this.labelNames.getModel().hasChildNode(newText))
			return -1;
		int index = this.labelNames.getIndexOfItem(oldText);
		this.labelNames.getModel().getChildNode(oldText).put(XPreferences.XPREFERENCES_NODENAMEKEY, newText);
		this.labelNames.getListModel().fireContentsChanged(index);
		return index;
	}
	
	// ////////////////////////////////////////
	// ////////// Label VALUES ////////////////
	// ////////////////////////////////////////
	
	// Adding LabelValue //////////////////////////////////////////
	public void _addLabelValue()
	{
		String text = this.labelValues.getTextField();
		int newIndex = this._addLabelValue(text);
		labelValues.setSelectedIndex(newIndex, labelValues.getCurrentSelectionIndex());
		this.labelsChanged();
	}
	
	private int _addLabelValue(String text)
	{
		if(labelValues.getModel().hasChildNode(text))
			return -1;
		int index = this.labelValues.size();
		String csvColor = this.getFirstAvailableColor();
		XPreferences value = this.labelValues.getModel().getChildNode(text); // Adds
		// node
		// if
		// not
		// there
		// already
		value.put(JEXDBInfo.DB_LABELCOLORCODE_COLOR, csvColor);
		Color color = ColorPallet.stringToColor(csvColor);
		if(color == null)
			return -1;
		JEXStatics.labelColorCode.setColorForLabel(labelValues.getModel().getName(), value.getName(), color);
		this.labelValues.getListModel().fireIntervalAdded(index);
		return index;
	}
	
	// Removing LabelValue //////////////////////////////////////////
	public void _removeLabelValue()
	{
		int currentIndex = this.labelValues.getCurrentSelectionIndex();
		String text = this.labelValues.getCurrentSelection();
		int newIndex = this._removeLabelValue(text);
		labelValues.setSelectedIndex(newIndex, currentIndex);
		this.labelsChanged();
	}
	
	private int _removeLabelValue(String text)
	{
		if(!labelValues.getModel().hasChildNode(text))
			return -1;
		int index = this.labelValues.getCurrentSelectionIndex();
		this.labelValues.getModel().removeNode(text);
		this.labelValues.getListModel().fireIntervalRemoved(index);
		return index - 1;
	}
	
	// Renaming LabelValue //////////////////////////////////////////
	public void _renameLabelValue()
	{
		String oldText = this.labelValues.getCurrentSelection();
		String newText = this.labelValues.getTextField();
		int newIndex = this._renameLabelValue(oldText, newText);
		labelValues.setSelectedIndex(newIndex, 0);
		this.labelsChanged();
	}
	
	private int _renameLabelValue(String oldText, String newText)
	{
		if(!this.labelValues.getModel().hasChildNode(oldText) || this.labelValues.getModel().hasChildNode(newText))
			return -1;
		int index = this.labelValues.getIndexOfItem(oldText);
		this.labelValues.getModel().getChildNode(oldText).put(XPreferences.XPREFERENCES_NODENAMEKEY, newText);
		this.labelValues.getListModel().fireContentsChanged(index);
		return index;
	}
	
	private String getFirstAvailableColor()
	{
		List<Integer> takenColors = this.getTakenColorIndicies();
		int index = this.getFirstAvailableColorIndex(takenColors);
		return ColorPallet.getColorString(index);
	}
	
	private List<Integer> getTakenColorIndicies()
	{
		List<Integer> ret = new Vector<Integer>();
		for (XPreferences value : this.labelValues.getModel().getChildNodes())
		{
			String csvColor = value.get(JEXDBInfo.DB_LABELCOLORCODE_COLOR);
			Color color = ColorPallet.stringToColor(csvColor);
			if(color == null)
				ret.add(-1);
			ret.add(ColorPallet.getColorIndex(color));
		}
		return ret;
	}
	
	private int getFirstAvailableColorIndex(List<Integer> takenColors)
	{
		boolean isTaken = true;
		int i = 0;
		while (isTaken)
		{
			isTaken = false;
			for (Integer colorIndex : takenColors)
			{
				if(i == colorIndex)
				{
					isTaken = true;
				}
			}
			if(!isTaken)
			{
				return i;
			}
			else
			{
				i = i + 1;
			}
		}
		return i;
	}
	
}

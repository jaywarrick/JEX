package jex.infoPanels;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXDBIO;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.arrayView.ImageDisplay;
import jex.arrayView.ImageDisplayController;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import preferences.XMLPreferences_XElement;
import preferences.XPreferencePanelController;
import preferences.XPreferences;
import signals.SSCenter;

public class SelectedObjectInfoPanelController extends InfoPanelController {
	
	// variables
	TypeName selectedObject = null;
	JEXEntry viewedEntry = null;
	JEXData data = null;
	
	String objectValue = null;
	Type objectType = null;
	String objectName = null;
	
	public SelectedObjectInfoPanelController()
	{
		// pass variables
		viewedEntryOrDataChange();
		
		// Sign up to dbInfo change signals
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTEDOBJ, this, "viewedEntryOrDataChange");
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.VIEWEDENTRY, this, "viewedEntryOrDataChange");
	}
	
	public void viewedEntryOrDataChange()
	{
		// Get the entry viewed
		viewedEntry = JEXStatics.jexManager.getViewedEntry();
		
		// Get the viewed data
		selectedObject = JEXStatics.jexManager.getSelectedObject();
		
		// If the data viewed is not null, get the data in the entry
		data = null;
		objectValue = null;
		objectType = null;
		objectName = null;
		if(selectedObject == null || viewedEntry == null)
		{
			// Refresh the gui
			SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_EXP, (Object[]) null);
			SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_ARR, (Object[]) null);
			return;
		}
		
		// get the entry TNV
		TreeMap<Type,TreeMap<String,JEXData>> entrymap = viewedEntry.getDataList();
		
		// Get the NV
		TreeMap<String,JEXData> objectmap = entrymap.get(selectedObject.getType());
		
		// Get the V
		String objName = (selectedObject == null) ? "" : selectedObject.getName();
		objectValue = (objectmap == null || objectmap.get(objName) == null) ? "" : objectmap.get(objName).getDictionaryValue();
		
		// Get the dataobject
		data = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.selectedObject, viewedEntry);
		
		// Make selected object type and name
		objectType = selectedObject.getType();
		objectName = selectedObject.getName();
		
		// Refresh the gui
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_EXP, (Object[]) null);
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_ARR, (Object[]) null);
	}
	
	public InfoPanel panel()
	{
		return new SelectedObjectInfoPanel();
	}
	
	class SelectedObjectInfoPanel extends InfoPanel implements ImageDisplayController, MouseListener, ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		// gui
		JPanel centerPanel;
		JLabel openXML;
		JCheckBox viewInArray = new JCheckBox("Preview object in the array");
		
		public SelectedObjectInfoPanel()
		{
			// Create
			super();
			
			// Do some connexions
			viewInArray.addActionListener(this);			
			
			// Make the gui
			rebuild();
		}
		
		private void rebuild()
		{
			// make the repository label
			JLabel label1 = new JLabel("Name:");
			// label1.setBackground(transparent);
			label1.setForeground(Color.white);
			
			JLabel label2 = new JLabel(objectName);
			// label2.setBackground(transparent);
			label2.setForeground(Color.white);
			
			// Make the database label
			JLabel label3 = new JLabel("Type:");
			// label3.setBackground(transparent);
			label3.setForeground(Color.white);
			
			String typeString = "";
			if(objectType != null)
			{
				typeString = objectType.toString();
			}	
			JLabel label4 = new JLabel(typeString);
			label4.setForeground(Color.white);
			
			
			// The tick box
			boolean flag = JEXStatics.jexManager.viewDataInArray();
			viewInArray.setSelected(flag);
			viewInArray.setOpaque(false);
			viewInArray.setForeground(Color.white);
			
			// Place the items in the panel
			centerPanel = new JPanel();
			centerPanel.setLayout(new MigLayout("ins 0"));
			centerPanel.setBackground(InfoPanel.centerPaneBGColor);
			centerPanel.add(label1, "width 60,height 15");
			centerPanel.add(label2, "width 60,height 15,growx,wrap");
			centerPanel.add(label3, "width 60,height 15");
			centerPanel.add(label4, "width 60,height 15,growx,wrap");
			centerPanel.add(viewInArray, "width 60,height 15,growx,span 2,wrap");
			
			// If the viewed entry is null add a label
			if(viewedEntry == null)
			{
				JLabel newLabelValue = new JLabel("No entry selected");
				newLabelValue.setForeground(Color.white);
				centerPanel.add(newLabelValue, "height 15,growx,span,wrap");
			}
			
			// If the viewed entry is null add a label
			else if(selectedObject == null)
			{
				JLabel newLabelValue = new JLabel("No object selected");
				newLabelValue.setForeground(Color.white);
				centerPanel.add(newLabelValue, "height 15,growx,span,wrap");
			}
			
			// If the data is null add a label
			else if(data == null)
			{
				JLabel newLabelValue = new JLabel("No data in selected entry");
				newLabelValue.setForeground(Color.white);
				centerPanel.add(newLabelValue, "height 15,growx,span,wrap");
			}
			
			// Get the data value
			else
			{
				// Display the data preview
				JComponent comp = makeDataComponent(selectedObject.getType().toString(), selectedObject.getName(), objectValue);
				if(comp != null)
				{
					int compHeight = (int) comp.getSize().getHeight();
					centerPanel.add(comp, "height 15:" + compHeight + ":" + compHeight + ",grow,span,wrap");
					
					// Add an opening button for the xml
					openXML = new JLabel("open xml");
					openXML.setForeground(Color.white);
					// openXML.setBackground(transparent);
					openXML.setFont(FontUtility.italicFonts);
					openXML.removeMouseListener(this);
					openXML.addMouseListener(this);
					centerPanel.add(openXML, "height 15,growx,span,wrap");
				}
			}
			
			this.setTitle("Viewed Object");
			this.setCenterPanel(centerPanel);
			this.centerPanel.revalidate();
			this.centerPanel.repaint();
		}
		
		private JComponent makeDataComponent(String type, String name, String value)
		{
			if(type.equals(JEXData.IMAGE))
			{
				ImageDisplay imagePanel = new ImageDisplay(this, "Viewer");
				ImagePlus image = new ImagePlus(value);
				if(image.getProcessor() == null)
					return null;
				imagePanel.setImage(image);
				imagePanel.setSize(new Dimension(200, 150));
				// imagePanel.setBackground(transparent);
				imagePanel.setAlignmentX(LEFT_ALIGNMENT);
				return imagePanel;
			}
			else if(type.equals(JEXData.FILE))
			{
				JLabel newLabelValue = new JLabel(value);
				newLabelValue.setMaximumSize(new Dimension(100, 15));
				newLabelValue.setForeground(Color.white);
				newLabelValue.setSize(new Dimension(100, 15));
				newLabelValue.setAlignmentX(LEFT_ALIGNMENT);
				return newLabelValue;
			}
			else if(type.equals(JEXData.MOVIE))
			{
				JLabel newLabelValue = new JLabel(value);
				newLabelValue.setMaximumSize(new Dimension(100, 15));
				newLabelValue.setForeground(Color.white);
				newLabelValue.setSize(new Dimension(100, 15));
				newLabelValue.setAlignmentX(LEFT_ALIGNMENT);
				return newLabelValue;
			}
			else if(type.equals(JEXData.VALUE))
			{
				JLabel newLabelValue = new JLabel("Value:" + value);
				newLabelValue.setMaximumSize(new Dimension(100, 15));
				// newLabelValue.setBackground(transparent);
				newLabelValue.setForeground(Color.white);
				newLabelValue.setSize(new Dimension(100, 15));
				newLabelValue.setAlignmentX(LEFT_ALIGNMENT);
				return newLabelValue;
			}
			else if(type.equals(JEXData.LABEL))
			{
				JLabel newLabelValue = new JLabel("Value:" + value);
				newLabelValue.setMaximumSize(new Dimension(100, 15));
				// newLabelValue.setBackground(transparent);
				newLabelValue.setForeground(Color.white);
				newLabelValue.setSize(new Dimension(100, 15));
				newLabelValue.setAlignmentX(LEFT_ALIGNMENT);
				return newLabelValue;
			}
			else if(type.equals(JEXData.FUNCTION_OLD))
			{
				JLabel newLabelValue = new JLabel(value);
				newLabelValue.setMaximumSize(new Dimension(100, 15));
				newLabelValue.setForeground(Color.white);
				newLabelValue.setSize(new Dimension(100, 15));
				newLabelValue.setAlignmentX(LEFT_ALIGNMENT);
				return newLabelValue;
			}
			return null;
		}
		
		public void clickedPoint(Point p)
		{}
		
		public void extendedRectangle(Rectangle r)
		{}
		
		public void rightClickedPoint(Point p)
		{}
		
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == viewInArray)
			{
				JEXStatics.jexManager.setViewDataInArray(!JEXStatics.jexManager.viewDataInArray());
				rebuild();
			}
		}
		
		public void mouseClicked(MouseEvent e)
		{}
		
		public void mousePressed(MouseEvent e)
		{
			if(e.getSource() == openXML)
			{
				if(data != null && data instanceof JEXData)
				{
					// Get the XData
					XMLPreferences_XElement xdata = JEXDBIO.getViewingXMLElement(data);
					// XMLPreferences_XElement xdata =
					// ((JEXData)data).getViewingXMLElement();
					
					// Get the preferences
					XPreferences prefs = new XPreferences();
					prefs.setElement(xdata);
					XPreferencePanelController pPanel = new XPreferencePanelController(prefs, false);
					JPanel pane = pPanel.getPanel();
					
					// Show the panel in a popup frame
					JDialog diag = new JDialog();
					diag.setResizable(true);
					diag.setBounds(800, 50, 200, 400);
					diag.setMinimumSize(new Dimension(75, 300));
					diag.setTitle("Je'Xperiment - Dataview");
					diag.setContentPane(pane);
					
					diag.setVisible(true);
				}
			}
		}
		
		public void mouseReleased(MouseEvent e)
		{}
		
		public void mouseEntered(MouseEvent e)
		{}
		
		public void mouseExited(MouseEvent e)
		{}
	}
}

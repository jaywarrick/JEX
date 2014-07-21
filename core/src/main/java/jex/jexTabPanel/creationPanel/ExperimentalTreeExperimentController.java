package jex.jexTabPanel.creationPanel;

import Database.DBObjects.JEXEntry;
import Database.Definition.Experiment;
import Database.Definition.HierarchyLevel;
import icons.IconRepository;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class ExperimentalTreeExperimentController implements Comparable<ExperimentalTreeExperimentController> {
	
	// Model
	public Experiment exp;
	public ExperimentalTreeExperimentPanel panel;
	private ExperimentalTreeController parentController;
	
	// GUI
	private JLabel editbutton = new JLabel("");
	private JLabel openbutton = new JLabel("");
	private JLabel expLabel = new JLabel("");
	private JLabel title = new JLabel("");
	private JLabel info = new JLabel("");
	private JLabel more = new JLabel("");
	public boolean expanded = false;
	public boolean viewed = false;
	private Color background = ExperimentalTreeController.defaultBackground;
	
	public ExperimentalTreeExperimentController(ExperimentalTreeController parentController)
	{
		this.parentController = parentController;
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "selectionChanged", (Class[]) null);
		
		// initialize
		editbutton = new JLabel("");
		editbutton.setText(null);
		editbutton.setToolTipText("Edit the dataset");
		editbutton.setIcon(JEXStatics.iconRepository.getIconWithName(IconRepository.MISC_PREFERENCES, 30, 30));
		
		openbutton = new JLabel("");
		openbutton.setText(null);
		openbutton.setToolTipText("Open dataset");
		openbutton.setIcon(JEXStatics.iconRepository.getIconWithName(IconRepository.BUTTON_FORWARD, 30, 30));
		
		expLabel = new JLabel("");
		expLabel.setText(null);
		expLabel.setIcon(JEXStatics.iconRepository.getIconWithName(IconRepository.EXPERIMENT_ICON, 30, 30));
		
	}
	
	// /
	// / GETTERS AND SETTERS
	// /
	
	public void setExperiment(Experiment exp)
	{
		this.exp = exp;
		rebuildModel();
	}
	
	public Experiment getExperiment()
	{
		return exp;
	}
	
	public void setSelected(boolean selected)
	{
		// Get the entries
		Set<JEXEntry> entries = exp.getEntries();
		
		// Add or remove from selection
		if(selected)
		{
			JEXStatics.jexManager.addEntriesToSelection(entries);
		}
		else
		{
			JEXStatics.jexManager.removeEntriesFromSelection(entries);
		}
	}
	
	public boolean isSelected()
	{
		// Get the entries
		Set<JEXEntry> entries = exp.getEntries();
		
		// Are all of the entries selected?
		boolean result = JEXStatics.jexManager.isAllSelected(entries);
		
		return result;
	}
	
	public boolean isExpanded()
	{
		return this.expanded;
	}
	
	public void setExpanded(boolean expanded)
	{
		this.expanded = expanded;
		
		rebuildModel();
	}
	
	public void rebuildModel()
	{
		Logs.log("Rebuilding Experiment tree node model - " + exp.getName(), 1, this);
		
		title.setText(exp.getName());
		title.setFont(FontUtility.boldFont);
		info.setFont(FontUtility.italicFont);
		info.setText(exp.expInfo);
		
		more.setText("[" + exp.size() + " x " + exp.get(0).size() + "]");
		more.setFont(FontUtility.italicFont);
		
		panel().rebuild();
	}
	
	public ExperimentalTreeExperimentPanel panel()
	{
		if(panel == null)
		{
			panel = new ExperimentalTreeExperimentPanel();
			panel.rebuild();
		}
		return panel;
	}
	
	// /
	// / SIGNALS
	// /
	
	public void edit(HierarchyLevel hLevel)
	{
		SSCenter.defaultCenter().emit(this, ExperimentalTreeController.DATA_EDIT, new Object[] { hLevel });
	}
	
	public void open(HierarchyLevel hLevel)
	{
		SSCenter.defaultCenter().emit(this, ExperimentalTreeController.DATA_OPEN, new Object[] { hLevel });
	}
	
	// /
	// / IMPLEMENTED INTERFACES
	// /
	
	public int compareTo(ExperimentalTreeExperimentController toCompare)
	{
		// Get the sorting option
		String sortOption = ExperimentalTreeController.sortBy;
		
		// Get the experiment to compare with
		Experiment toCompareExp = toCompare.getExperiment();
		
		// Get the current experiment
		Experiment exp = this.getExperiment();
		
		if(sortOption.equals(ExperimentalTreeController.SORT_OPTIONS[0]))
		{
			String str1 = exp.getName();
			String str2 = toCompareExp.getName();
			return str1.compareTo(str2);
		}
		else if(sortOption.equals(ExperimentalTreeController.SORT_OPTIONS[1]))
		{
			String str1 = exp.expDate;
			String str2 = toCompareExp.expDate;
			return str1.compareTo(str2);
		}
		else if(sortOption.equals(ExperimentalTreeController.SORT_OPTIONS[2]))
		{
			String str1 = exp.expInfo;
			String str2 = toCompareExp.expInfo;
			return str1.compareTo(str2);
		}
		else
		{
			String str1 = exp.getName();
			String str2 = toCompareExp.getName();
			return str1.compareTo(str2);
		}
	}
	
	public void selectionChanged()
	{
		panel().setPanelSelected(isSelected());
	}
	
	class ExperimentalTreeExperimentPanel extends JPanel implements MouseListener {
		
		private static final long serialVersionUID = 1L;
		private JPanel topPanel;
		
		ExperimentalTreeExperimentPanel()
		{
			// make the holding panel
			topPanel = new JPanel();
			
			// Make the listeners
			topPanel.addMouseListener(this);
			editbutton.addMouseListener(this);
			openbutton.addMouseListener(this);
		}
		
		public void rebuild()
		{
			Logs.log("Rebuilding Experiment tree node panel - " + exp.getName(), 1, this);
			
			// remove all
			this.removeAll();
			
			// Set the mig layout
			this.setLayout(new MigLayout("flowy, ins 0", "10[fill,grow]10", ""));
			this.setBackground(DisplayStatics.background);
			
			// Add all the components
			topPanel.setLayout(new MigLayout("flowy, ins 0", "10[fill,grow]10", "[]10[]10[]"));
			topPanel.add(title, "width 0:0:");
			topPanel.add(info, "width 100:100:");
			topPanel.add(more, "width 0:0:");
			this.add(topPanel);
			
			// Place the cropping button
			topPanel.add(expLabel, "width 35,gapx 10, Dock west");
			if(parentController.treeMode() == ExperimentalTreeController.TREE_MODE_CREATION)
			{
				topPanel.add(openbutton, "width 35, Dock east");
				topPanel.add(editbutton, "width 35, Dock east");
			}
			
			// If the cell is selected display the background
			if(isSelected())
				topPanel.setBorder(BorderFactory.createLineBorder(DisplayStatics.redoutline, 2));
			else
				topPanel.setBorder(BorderFactory.createLineBorder(DisplayStatics.dividerColor, 2));
			
			// refresh display
			panel().revalidate();
			panel().repaint();
		}
		
		public void setPanelSelected(boolean selected)
		{
			// If the cell is selected display the background
			if(selected)
				topPanel.setBorder(BorderFactory.createLineBorder(DisplayStatics.redoutline, 2));
			else
				topPanel.setBorder(BorderFactory.createLineBorder(DisplayStatics.dividerColor, 2));
		}
		
		public void mouseClicked(MouseEvent e)
		{}
		
		public void mouseEntered(MouseEvent e)
		{
			if(e.getSource() == topPanel)
			{
				background = ExperimentalTreeController.selectedBackground;
				topPanel.setBackground(background);
				this.repaint();
			}
		}
		
		public void mouseExited(MouseEvent e)
		{
			if(e.getSource() == topPanel)
			{
				background = ExperimentalTreeController.defaultBackground;
				topPanel.setBackground(background);
				this.repaint();
			}
		}
		
		public void mousePressed(MouseEvent e)
		{}
		
		public void mouseReleased(MouseEvent e)
		{
			if(e.getSource() == topPanel)
			{
				setSelected(!isSelected());
			}
			else if(e.getSource() == editbutton)
			{
				edit(exp);
			}
			else if(e.getSource() == openbutton)
			{
				open(exp);
			}
		}
		
	}
	
}
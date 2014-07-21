package jex.jexTabPanel.jexStatisticsPanel;

import Database.Definition.Type;
import Database.Definition.TypeName;
import guiObject.FlatRoundedStaticButton;
import guiObject.TypeNameButton;
import icons.IconRepository;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import net.miginfocom.swing.MigLayout;

public class FilterPanel extends JPanel implements ActionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	// Input Panel
	private JPanel imagePane;
	private TypeNameButton dataButton;
	private FlatRoundedStaticButton viewButton;
	private FlatRoundedStaticButton groupButton;
	private JButton reductionButton = new JButton("-");
	private JButton expansionButton = new JButton("+");
	private boolean expanded = false;
	
	// Class Variables
	private boolean isAllFiltered = false;
	private boolean isGroupedBy = false;
	private HashMap<String,FilterValuePanel> panelList;
	
	private JEXStatisticsRightPanel parent;
	public String labelName;
	public Type labelType;
	private TypeName labelTypeName;
	private TreeSet<String> labels;
	
	public FilterPanel(Type type, String labelName, TreeSet<String> labels, JEXStatisticsRightPanel parent)
	{
		this.labels = labels;
		this.labelName = labelName;
		this.labelType = type;
		this.labelTypeName = new TypeName(type, labelName);
		this.parent = parent;
		
		initialize();
		refresh();
	}
	
	public FilterPanel(Type type, String labelName, String[] labels, JEXStatisticsRightPanel parent)
	{
		TreeSet<String> thisLabels = new TreeSet<String>();
		for (String label : labels)
		{
			thisLabels.add(label);
		}
		this.labels = thisLabels;
		this.labelName = labelName;
		this.labelType = type;
		this.labelTypeName = new TypeName(type, labelName);
		this.parent = parent;
		
		initialize();
		refresh();
	}
	
	/**
	 * Initialize the label panel
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[24]5[24]5[fill,grow]5[]5[]", "[0:0,24]"));
		this.setBackground(DisplayStatics.lightBackground);
		this.addMouseListener(this);
		
		reductionButton.setIcon(JEXStatics.iconRepository.getIconWithName(IconRepository.MISC_MINUS, 20, 20));
		reductionButton.setText(null);
		expansionButton.setIcon(JEXStatics.iconRepository.getIconWithName(IconRepository.MISC_PLUS, 20, 20));
		expansionButton.setText(null);
		reductionButton.setMaximumSize(new Dimension(20, 20));
		expansionButton.setMaximumSize(new Dimension(20, 20));
		reductionButton.setPreferredSize(new Dimension(20, 20));
		expansionButton.setPreferredSize(new Dimension(20, 20));
		reductionButton.addActionListener(this);
		expansionButton.addActionListener(this);
		
		viewButton = new FlatRoundedStaticButton("Filter all");
		viewButton.enableUnselection(false);
		viewButton.addActionListener(this);
		
		groupButton = new FlatRoundedStaticButton("Group");
		groupButton.enableUnselection(false);
		groupButton.addActionListener(this);
		
		dataButton = new TypeNameButton(labelTypeName);
		
		// if it is the valid label, then expand it automatically
		if(labelName.equals("Valid"))
			expanded = true;
	}
	
	/**
	 * Make the label panel
	 */
	public void makeLabelPane()
	{
		imagePane = new JPanel();
		imagePane.setBackground(DisplayStatics.lightBackground);
		imagePane.setLayout(new MigLayout("flowy, ins 0, gap 0", "[fill,grow]", "[]"));
		panelList = new HashMap<String,FilterValuePanel>();
		for (String label : labels)
		{
			FilterValuePanel labelPane = new FilterValuePanel(labelType, labelName, label);
			panelList.put(label, labelPane);
			imagePane.add(labelPane);
		}
	}
	
	/**
	 * Set whether this object is ticked for view or not
	 * 
	 * @param isTicked
	 */
	public void setIsFiltered(boolean isAllFiltered)
	{
		this.isAllFiltered = isAllFiltered;
	}
	
	public void setAllValuesToFalse()
	{
		for (FilterValuePanel pane : panelList.values())
		{
			pane.setFiltered(false);
		}
		setIsFiltered(false);
		viewButton.setPressed(false);
	}
	
	public void setFilteredValue(String value, boolean isFiltered)
	{
		FilterValuePanel labelPane = panelList.get(value);
		if(labelPane == null)
			return;
		labelPane.setFiltered(isFiltered);
		
		if(isFiltered)
			setIsFiltered(true);
		
		// // test is all elements are filtered
		// boolean filt = true;
		// for (FilterValuePanel pane: panelList.values()){
		// if (!pane.isFiltered()) {
		// filt = false;
		// break;
		// }
		// }
		// setIsFiltered(filt);
		if(!this.isAllFiltered)
		{
			viewButton.setPressed(false);
		}
		else
		{
			viewButton.setPressed(true);
		}
	}
	
	/**
	 * Set the selected flag of this object panel
	 * 
	 * @param isSelected
	 */
	public void setIsGrouped(boolean isGroupedBy)
	{
		this.isGroupedBy = isGroupedBy;
		if(!this.isGroupedBy)
		{
			groupButton.setPressed(false);
		}
		else
		{
			groupButton.setPressed(true);
		}
	}
	
	public boolean isAllValuesFiltered()
	{
		for (FilterValuePanel labelPane : panelList.values())
		{
			if(!labelPane.isFiltered())
				return false;
		}
		return true;
	}
	
	/**
	 * Refresh the label panel
	 */
	public void refresh()
	{
		this.removeAll();
		this.setPreferredSize(null);
		
		makeLabelPane();
		
		if(!this.isAllFiltered)
		{
			viewButton.setPressed(false);
		}
		else
		{
			viewButton.setPressed(true);
		}
		
		if(!this.isGroupedBy)
		{
			groupButton.setPressed(false);
		}
		else
		{
			groupButton.setPressed(true);
		}
		
		if(expanded == true)
			expanded();
		else
			reduced();
	}
	
	private void expanded()
	{
		
		this.removeAll();
		this.add(reductionButton);
		this.add(dataButton);
		this.add(new JLabel(labelName), "growx, width 25:25:");
		this.add(groupButton.panel());
		this.add(viewButton.panel());
		this.add(imagePane, "south");
		
		this.revalidate();
		this.repaint();
		
	}
	
	private void reduced()
	{
		this.removeAll();
		this.add(expansionButton);
		this.add(dataButton);
		this.add(new JLabel(labelName), "growx, width 25:25:");
		this.add(groupButton.panel());
		this.add(viewButton.panel());
		
		this.revalidate();
		this.repaint();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == groupButton)
		{
			if(this.isGroupedBy)
				parent.removedGroup(labelType, labelName);
			else
				parent.addedGroup(labelType, labelName);
		}
		if(e.getSource() == viewButton)
		{
			boolean allFiltered = isAllFiltered;
			for (String labelValue : labels)
			{
				if(allFiltered)
					parent.removedFilter(labelType, labelName, labelValue);
				else
					parent.addedFilter(labelType, labelName, labelValue);
			}
		}
		if(e.getSource() == reductionButton)
		{
			expanded = false;
			reduced();
		}
		if(e.getSource() == expansionButton)
		{
			expanded = true;
			expanded();
		}
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{}
	
	/**
	 * Label Value Class
	 * 
	 * @author erwinberthier
	 * 
	 */
	class FilterValuePanel extends JPanel implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		private Type labelType;
		private String labelName;
		private String labelValue;
		private boolean isFiltered = false;
		private FlatRoundedStaticButton selectButton;
		
		FilterValuePanel(Type type, String labelName, String labelValue)
		{
			this.labelType = type;
			this.labelName = labelName;
			this.labelValue = labelValue;
			initialize();
		}
		
		private void initialize()
		{
			this.setLayout(new MigLayout("flowx, ins 0 30 0 0", "[fill,grow]5[]", "[]"));
			this.setBackground(DisplayStatics.lightBackground);
			
			selectButton = new FlatRoundedStaticButton("filter");
			selectButton.enableUnselection(false);
			selectButton.addActionListener(this);
			
			JLabel newLabelValue = new JLabel(this.labelValue);
			newLabelValue.setForeground(JEXStatics.labelColorCode.getColorForLabel(this.labelName, this.labelValue));
			
			this.add(newLabelValue, "growx");
			this.add(selectButton.panel());
		}
		
		public boolean isFiltered()
		{
			return isFiltered;
		}
		
		public void setFiltered(boolean isFiltered)
		{
			this.isFiltered = isFiltered;
			if(isFiltered)
				selectButton.setPressed(true);
			else
				selectButton.setPressed(false);
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == selectButton)
			{
				if(isFiltered)
					parent.removedFilter(labelType, labelName, labelValue);
				else
					parent.addedFilter(labelType, labelName, labelValue);
			}
		}
		
	}
	
}

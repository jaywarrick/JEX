package jex.jexTabPanel.jexLabelPanel;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;
import guiObject.FlatRoundedStaticButton;
import guiObject.TypeNameButton;
import icons.IconRepository;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class LabelPanelLine extends JPanel implements ActionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	// Input Panel
	private Color foregroundColor = DisplayStatics.lightBackground;
	private JPanel buttonPane = new JPanel();
	
	private JButton reductionButton = new JButton("-");
	private JButton expansionButton = new JButton("+");
	
	private JPanel imagePane;
	
	private TypeNameButton dataButton;
	private FlatRoundedStaticButton viewButton;
	
	public boolean expanded = false;
	
	// Class Variables
	private LabelsPanel parent;
	public String labelName;
	private TypeName labelTypeName;
	private TreeMap<String,Set<JEXEntry>> labels;
	
	public LabelPanelLine(String labelName, TreeMap<String,Set<JEXEntry>> labels, LabelsPanel parent)
	{
		this.labels = labels;
		this.labelName = labelName;
		this.labelTypeName = new TypeName(JEXData.LABEL, labelName);
		this.parent = parent;
		
		// Connect to the label selection listener
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTEDLABEL, this, "refresh", (Class[]) null);
		Logs.log("Connected to database label selection signal", 0, this);
		
		initialize();
		makeLabelPane();
		refresh();
	}
	
	/**
	 * Initialize the label panel
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[24]5[24]5[fill,grow]5[]", "[0:0,24]"));
		this.setBackground(DisplayStatics.lightBackground);
		this.addMouseListener(this);
		
		ImageIcon minusicon = JEXStatics.iconRepository.getIconWithName(IconRepository.MISC_MINUS, 20, 20);
		ImageIcon plusicon = JEXStatics.iconRepository.getIconWithName(IconRepository.MISC_PLUS, 20, 20);
		reductionButton.setIcon(minusicon);
		reductionButton.setText(null);
		expansionButton.setIcon(plusicon);
		expansionButton.setText(null);
		reductionButton.setMaximumSize(new Dimension(20, 20));
		expansionButton.setMaximumSize(new Dimension(20, 20));
		reductionButton.setPreferredSize(new Dimension(20, 20));
		expansionButton.setPreferredSize(new Dimension(20, 20));
		reductionButton.addActionListener(this);
		expansionButton.addActionListener(this);
		
		dataButton = new TypeNameButton(labelTypeName);
		
		viewButton = new FlatRoundedStaticButton("View");
		viewButton.enableUnselection(false);
		viewButton.addActionListener(this);
		
		JLabel label = new JLabel(labelName);
		
		this.add(expansionButton);
		this.add(dataButton);
		this.add(label, "growx, width 25:25:");
		this.add(viewButton.panel());
	}
	
	/**
	 * Initialize the GUI for creating batch functions
	 */
	private void expanded()
	{
		JLabel label = new JLabel(labelName);
		
		this.removeAll();
		this.add(reductionButton);
		this.add(dataButton);
		this.add(label, "growx, width 25:25:");
		this.add(viewButton.panel());
		
		this.add(imagePane, "south");
	}
	
	/**
	 * Display reduced version of this panel
	 */
	private void reduced()
	{
		JLabel label = new JLabel(labelName);
		
		this.removeAll();
		this.add(expansionButton);
		this.add(dataButton);
		this.add(label, "growx, width 25:25:");
		this.add(viewButton.panel());
		
	}
	
	/**
	 * Make the label panel
	 */
	public void makeLabelPane()
	{
		imagePane = new JPanel();
		imagePane.setBackground(DisplayStatics.lightBackground);
		imagePane.setLayout(new MigLayout("flowy, ins 0, gap 0", "[fill,grow,right]", "[]"));
		
		for (String label : labels.keySet())
		{
			LabelValuePanel labelPane = new LabelValuePanel(labelName, label);
			imagePane.add(labelPane);
		}
	}
	
	/**
	 * Refresh the label panel
	 */
	public void refresh()
	{
		if(expanded == true)
			expanded();
		else
			reduced();
		
		if(labelName.equals(parent.getCurrentlySelected()))
		{
			buttonPane.setBackground(DisplayStatics.menuBackground);
			viewButton.background = DisplayStatics.menuBackground;
		}
		else
		{
			buttonPane.setBackground(foregroundColor);
			viewButton.background = foregroundColor;
		}
		
		TypeName labelTN = JEXStatics.jexManager.getSelectedLabel();
		if(labelTN == null || !labelTN.equals(labelTypeName))
		{
			viewButton.normalBack = foregroundColor;
			viewButton.setPressed(false);
		}
		else
		{
			viewButton.normalBack = DisplayStatics.dividerColor;
			viewButton.setPressed(true);
		}
		
		parent.revalidate();
		parent.repaint();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == reductionButton)
		{
			Logs.log("Reduction expansion button clicked, current status is " + expanded, 1, this);
			parent.openedLabelPanel(this.labelName);
			expanded = false;
			refresh();
		}
		if(e.getSource() == expansionButton)
		{
			Logs.log("Reduction expansion button clicked, current status is " + expanded, 1, this);
			parent.openedLabelPanel(this.labelName);
			expanded = true;
			refresh();
		}
		if(e.getSource() == viewButton)
		{
			TypeName labelTN = JEXStatics.jexManager.getSelectedLabel();
			if(labelTN == null || !labelTN.equals(labelTypeName))
			{
				JEXStatics.jexManager.setSelectedLabel(labelTypeName);
			}
			else
			{
				JEXStatics.jexManager.setSelectedLabel(null);
			}
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
	{
		Logs.log("Label " + labelName + " clicked", 1, this);
		parent.setCurrentlySelected(labelName);
	}
	
	/**
	 * Label Value Class
	 * 
	 * @author erwinberthier
	 * 
	 */
	class LabelValuePanel extends JPanel implements MouseListener {
		
		private static final long serialVersionUID = 1L;
		private String labelName;
		private String labelValue;
		
		LabelValuePanel(String labelName, String labelValue)
		{
			this.labelName = labelName;
			this.labelValue = labelValue;
			this.addMouseListener(this);
			initialize();
		}
		
		private void initialize()
		{
			this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			
			this.setMinimumSize(new Dimension(100, 15));
			this.setMaximumSize(new Dimension(100, 15));
			this.setPreferredSize(new Dimension(100, 15));
			this.setBackground(JEXStatics.labelColorCode.getColorForLabel(this.labelName, this.labelValue));
			
			JLabel newLabelValue = new JLabel(this.labelValue);
			newLabelValue.setMaximumSize(new Dimension(100, 15));
			newLabelValue.setForeground(Color.white);
			// newLabelValue.setForeground(JEXStatics.labelColorCode.getColorForLabel(this.labelName,this.labelValue));
			
			this.add(Box.createHorizontalGlue());
			this.add(newLabelValue);
			this.add(Box.createHorizontalGlue());
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
		{   
			
		}
		
	}
	
}

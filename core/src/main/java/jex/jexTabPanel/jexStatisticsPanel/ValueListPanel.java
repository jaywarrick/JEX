package jex.jexTabPanel.jexStatisticsPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.Definition.TypeName;

public class ValueListPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Class variables
	
	// GUI variables
	private HashMap<String,ValuePanel> panelList;
	
	private JPanel titlePane;
	private JPanel mainPane = new JPanel();
	private JPanel centerPane = new JPanel();
	private JScrollPane centralScrollPane = new JScrollPane(centerPane);
	private JPanel suffixPane;
	
	/**
	 * Create a new JEXData list
	 * 
	 * @param parent
	 */
	public ValueListPanel()
	{
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.DATASETS, this, "valuesChanged", (Class[]) null);
		
		panelList = new HashMap<String,ValuePanel>();
		
		init();
		valuesChanged();
	}
	
	private void init()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(DisplayStatics.background);
		
		JLabel title = new JLabel("VALUES TO ANALYZE");
		title.setFont(FontUtility.boldFont);
		titlePane = new JPanel();
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
		titlePane.setBackground(DisplayStatics.menuBackground);
		titlePane.setPreferredSize(new Dimension(100, 15));
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(title);
		titlePane.add(Box.createHorizontalGlue());
		this.add(titlePane, BorderLayout.PAGE_START);
		
		suffixPane = new JPanel();
		suffixPane.setBackground(DisplayStatics.menuBackground);
		suffixPane.setLayout(new BoxLayout(suffixPane, BoxLayout.LINE_AXIS));
		suffixPane.setPreferredSize(new Dimension(15, 15));
		this.add(suffixPane, BorderLayout.PAGE_END);
		
		centerPane.setBackground(DisplayStatics.lightBackground);
		centerPane.setLayout(new MigLayout("flowy,ins 3 3 3 3, gap 0", "[fill,grow]", "[]"));
		centralScrollPane.setBorder(BorderFactory.createEmptyBorder());
		centralScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		centralScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		mainPane.setBackground(DisplayStatics.lightBackground);
		mainPane.setLayout(new BorderLayout());
		mainPane.add(centralScrollPane, BorderLayout.CENTER);
		this.add(centralScrollPane, BorderLayout.CENTER);
	}
	
	public void valuesChanged()
	{
		centerPane.removeAll();
		TreeMap<Type,TreeMap<String,TreeMap<String,Set<JEXEntry>>>> objects = JEXStatics.jexManager.getTNVI();
		if(objects == null)
			return;
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> values = objects.get(JEXData.VALUE);
		
		if(values == null || values.size() == 0)
		{
			JPanel temp = new JPanel();
			temp.setLayout(new BorderLayout());
			JLabel label = new JLabel("No objects");
			label.setFont(FontUtility.italicFonts);
			temp.add(label);
			temp.setPreferredSize(new Dimension(20, 20));
			centerPane.add(temp);
			this.repaint();
			return;
		}
		
		// Make the values panel
		panelList = new HashMap<String,ValuePanel>();
		for (String valueName : values.keySet())
		{
			ValuePanel valuePanel = new ValuePanel(valueName, this);
			panelList.put(valueName, valuePanel);
		}
		
		// Display them
		for (ValuePanel pane : panelList.values())
		{
			centerPane.add(pane, "growx");
		}
		
		this.revalidate();
		this.repaint();
	}
	
	public void setViewedObject(String valueName)
	{
		Logs.log("Setting viewed object", 1, this);
		JEXStatics.jexManager.setSelectedStatisticsObject(new TypeName(JEXData.VALUE, valueName));
	}
	
	public void actionPerformed(ActionEvent e)
	{   
		
	}
	
}

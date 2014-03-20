package jex.jexTabPanel.jexFunctionPanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;

public class FunctionLoadSaveAddRunPanel implements ActionListener {
	
	// GUI variables
	private JPanel panel;
	private FunctionListPanel parent;
	
	// Buttons
	private JButton loadButton = new JButton();
	private JButton saveButton = new JButton();
	// private JButton addButton = new JButton();
	private JButton runButton = new JButton();
	private JCheckBox autoSave = new JCheckBox();
	
	public FunctionLoadSaveAddRunPanel(FunctionListPanel parent)
	{
		this.parent = parent;
		this.initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void initialize()
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		this.panel.setLayout(new MigLayout("center, flowy, ins 3", "[fill, grow]", "[fill, grow]3[fill, grow]3[fill, grow]3[]"));
		// this.setMaximumSize(new Dimension(250,800));
		// this.setPreferredSize(new Dimension(250,200));
		
		// Create the add button
		this.loadButton.setText("LOAD");
		this.loadButton.setToolTipText("Click to add a function to the list");
		// loadButton.setPreferredSize(new Dimension(60,30));
		// loadButton.setMaximumSize(new Dimension(60,500));
		this.loadButton.addActionListener(this);
		
		this.saveButton.setText("SAVE");
		this.saveButton.setToolTipText("Click to add a function to the list");
		// saveButton.setPreferredSize(new Dimension(60,30));
		// saveButton.setMaximumSize(new Dimension(60,500));
		this.saveButton.addActionListener(this);
		
		// addButton.setText("ADD");
		// addButton.setToolTipText("Click to add a function to the list");
		// //addButton.setPreferredSize(new Dimension(60,30));
		// //addButton.setMaximumSize(new Dimension(60,500));
		// addButton.addActionListener(this);
		
		// Create the run all button
		this.runButton.setText("RUN");
		this.runButton.setToolTipText("Click to add a function to the list");
		// runButton.setPreferredSize(new Dimension(60,30));
		// runButton.setMaximumSize(new Dimension(60,500));
		this.runButton.addActionListener(this);
		
		// Create autoSave checkBox
		this.autoSave.setText("Auto-Save");
		this.autoSave.setSelected(true);
		
		// Create the button panel
		this.panel.setBackground(DisplayStatics.lightBackground);
		// this.panel.add(addButton,"growx, width 10:10:");
		this.panel.add(this.loadButton, "growx, height 10:10:");
		this.panel.add(this.saveButton, "growx, height 10:10:");
		this.panel.add(this.runButton, "growx, height 10:10:");
		this.panel.add(this.autoSave);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.runButton)
		{
			this.parent.runAllFunctions(this.isAutoSaveSelected());
		}
		else if(e.getSource() == this.loadButton)
		{
			this.parent.loadFunctionList();
		}
		else if(e.getSource() == this.saveButton)
		{
			this.parent.saveFunctionList();
		}
	}
	
	public boolean isAutoSaveSelected()
	{
		return this.autoSave.isSelected();
	}
	
}

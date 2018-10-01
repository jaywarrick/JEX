package jex.jexTabPanel.jexFunctionPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;

public class FunctionLoadSaveAndRunPanel implements ActionListener, ItemListener {

	// GUI variables
	private JPanel panel;
	private FunctionListPanel parent;

	// Buttons
	private JButton loadButton = new JButton();
	private JButton saveButton = new JButton();
	// private JButton addButton = new JButton();
	private JButton saveJXWToDB = new JButton();
	private JButton runAll = new JButton();
	private JCheckBox autoSave = new JCheckBox();
	private JCheckBox autoUpdate = new JCheckBox();

	public FunctionLoadSaveAndRunPanel(FunctionListPanel parent)
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
		this.panel.setLayout(new MigLayout("center, flowy, ins 3", "[fill, grow]", "[fill, grow]3[fill, grow]3[fill, grow]3[fill, grow]3[]"));
		this.panel.setMinimumSize(new Dimension(160,100));
		// this.setPreferredSize(new Dimension(250,200));

		// Create the add button
		this.loadButton.setText("LOAD");
		this.loadButton.setToolTipText("Click to load a workflow from a existing file.");
		// loadButton.setPreferredSize(new Dimension(60,30));
		// loadButton.setMaximumSize(new Dimension(60,500));
		this.loadButton.addActionListener(this);

		this.saveButton.setText("SAVE");
		this.saveButton.setToolTipText("Click to save the current workflow to a file.");
		// saveButton.setPreferredSize(new Dimension(60,30));
		// saveButton.setMaximumSize(new Dimension(60,500));
		this.saveButton.addActionListener(this);

		// addButton.setText("ADD");
		// addButton.setToolTipText("Click to add a function to the list");
		// //addButton.setPreferredSize(new Dimension(60,30));
		// //addButton.setMaximumSize(new Dimension(60,500));
		// addButton.addActionListener(this);

		// Create the run all button
		this.saveJXWToDB.setText("SAVE TO DB");
		this.saveJXWToDB.setToolTipText("Save the workflow to the database.");
		// runButton.setPreferredSize(new Dimension(60,30));
		// runButton.setMaximumSize(new Dimension(60,500));
		this.saveJXWToDB.addActionListener(this);

		// Create the run all button
		this.runAll.setText("RUN ALL");
		this.runAll.setToolTipText("Run all the functions in the workflow.");
		// runButton.setPreferredSize(new Dimension(60,30));
		// runButton.setMaximumSize(new Dimension(60,500));
		this.runAll.addActionListener(this);

		// Create autoSave checkBox
		this.autoSave.setText("Auto-Saving (ON)");
		this.autoSave.setSelected(true);
		this.autoSave.addItemListener(this);

		// Create autoSave checkBox
		this.autoUpdate.setText("Auto-Updating (OFF)");
		this.autoUpdate.setSelected(false);
		this.autoUpdate.addItemListener(this);

		// Create the button panel
		this.panel.setBackground(DisplayStatics.lightBackground);
		// this.panel.add(addButton,"growx, width 10:10:");
		this.panel.add(this.loadButton, "growx, height 10:10:");
		this.panel.add(this.saveButton, "growx, height 10:10:");
		this.panel.add(this.saveJXWToDB, "growx, height 10:10:");
		this.panel.add(this.runAll, "growx, height 10:10:");
		this.panel.add(this.autoSave);
		this.panel.add(this.autoUpdate);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.saveJXWToDB)
		{
			this.parent.saveAllFunctions(this.isAutoSavingOn());
		}
		if(e.getSource() == this.runAll)
		{
			this.parent.runAllFunctions(this.isAutoSavingOn(), this.isAutoUpdatingOn());
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

	public boolean isAutoSavingOn()
	{
		return this.autoSave.isSelected();
	}
	
	public boolean isAutoUpdatingOn()
	{
		return this.autoUpdate.isSelected();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == this.autoSave)
		{
			if(this.autoSave.isSelected())
			{
				this.autoSave.setText("Auto-Saving (ON)");
				// Call code to start auto-updater
			}
			else
			{
				this.autoSave.setText("Auto-Saving (OFF)");
				// Call code to cancel auto-updater
			}
		}
		if(e.getSource() == this.autoUpdate)
		{
			if(this.autoUpdate.isSelected())
			{
				this.autoUpdate.setText("Auto-Updating (ON)");
				// Call code to start auto-updater
			}
			else
			{
				this.autoUpdate.setText("Auto-Updating (OFF)");
				// Call code to cancel auto-updater
			}
		}
	}

}

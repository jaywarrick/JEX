package jex.jexTabPanel.jexPluginPanel;

import guiObject.DialogGlassPane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import plugin.rscripter.RScripter;
import plugins.imageAligner2.ImageAligner;
import plugins.selector.SelectorPlugInTester;
import plugins.viewer.ImageBrowser;
import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Experiment;
import Database.Definition.TypeName;
import Exporter.Exporter;
import Exporter.ExporterOptionPanel;

public class PluginPanel implements ActionListener {
	
	public JPanel panel;
	
	private Color foregroundColor = DisplayStatics.lightBackground;
	private JButton exportCSV = new JButton("Export to CSV");
	private JButton exportImage = new JButton("Export to Image");
	private JButton roiMaker = new JButton("Make ROIs");
	private JButton imageAligner = new JButton("Get Image Alignments");
	private JButton selectorPanel = new JButton("Selector Panel");
	private JButton rScripterPanel = new JButton("R Scripter");
	
	public PluginPanel()
	{
		this.panel = new JPanel();
		this.initialize();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void initialize()
	{
		this.panel.setBackground(this.foregroundColor);
		this.panel.setLayout(new MigLayout("flowy, ins 10, gapy 3, alignx center", "[]", "[]"));
		
		JLabel label1 = new JLabel("Exporting tools:");
		label1.setFont(FontUtility.boldFont);
		this.exportCSV.setMaximumSize(new Dimension(150, 20));
		this.exportCSV.setPreferredSize(new Dimension(150, 20));
		this.exportCSV.addActionListener(this);
		this.exportImage.setMaximumSize(new Dimension(150, 20));
		this.exportImage.setPreferredSize(new Dimension(150, 20));
		this.exportImage.addActionListener(this);
		JLabel label2 = new JLabel("Object creation tools:");
		label2.setFont(FontUtility.boldFont);
		this.roiMaker.setMaximumSize(new Dimension(150, 20));
		this.roiMaker.setPreferredSize(new Dimension(150, 20));
		this.roiMaker.addActionListener(this);
		this.imageAligner.setMaximumSize(new Dimension(150, 20));
		this.imageAligner.setPreferredSize(new Dimension(150, 20));
		this.imageAligner.addActionListener(this);
		this.selectorPanel.setMaximumSize(new Dimension(150, 20));
		this.selectorPanel.setPreferredSize(new Dimension(150, 20));
		this.selectorPanel.addActionListener(this);
		this.rScripterPanel.setMaximumSize(new Dimension(150, 20));
		this.rScripterPanel.setPreferredSize(new Dimension(150, 20));
		this.rScripterPanel.addActionListener(this);
		
		this.panel.add(label1, "left");
		// this.panel.add(Box.createVerticalStrut(3));
		this.panel.add(this.exportCSV);
		// this.panel.add(Box.createVerticalStrut(3));
		this.panel.add(this.exportImage);
		// this.panel.add(Box.createVerticalStrut(3));
		this.panel.add(label2, "left");
		// this.panel.add(Box.createVerticalStrut(3));
		this.panel.add(this.roiMaker);
		// this.panel.add(Box.createVerticalStrut(3));
		this.panel.add(this.imageAligner);
		// this.panel.add(Box.createVerticalStrut(3));
		this.panel.add(this.selectorPanel);
		// this.panel.add(Box.createVerticalGlue());
		this.panel.add(this.rScripterPanel);
	}
	
	// ----------------------------------------------------
	// --------- OTHER FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void diplayPanel()
	{}
	
	public void stopDisplayingPanel()
	{}
	
	public JPanel getHeader()
	{
		return null;
	}
	
	// ----------------------------------------------------
	// --------- EVENT FUNCTIONS -----------------
	// ----------------------------------------------------
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.exportCSV)
		{
			DialogGlassPane diagPanel = new DialogGlassPane("Info");
			diagPanel.setSize(400, 200);
			
			ExporterOptionPanel exportPane = new ExporterOptionPanel(Exporter.EXPORT_AS_CSV);
			diagPanel.setCentralPanel(exportPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
		}
		if(e.getSource() == this.exportImage)
		{
			DialogGlassPane diagPanel = new DialogGlassPane("Info");
			diagPanel.setSize(400, 300);
			
			ExporterOptionPanel exportPane = new ExporterOptionPanel(Exporter.EXPORT_AS_IMAGE);
			diagPanel.setCentralPanel(exportPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
		}
		if(e.getSource() == this.roiMaker)
		{
			TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
			TypeName tn = JEXStatics.jexManager.getSelectedObject();
			if(entries.size() > 0 && tn != null && tn.getType().equals(JEXData.IMAGE))
			{
				new ImageBrowser(entries, tn);
			}
		}
		if(e.getSource() == this.imageAligner)
		{
			TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
			TypeName tn = JEXStatics.jexManager.getSelectedObject();
			if(entries.size() > 0 && tn != null && tn.getType().equals(JEXData.IMAGE))
			{
				ImageAligner aligner = new ImageAligner();
				aligner.setDBSelection(entries, tn);
				
			}
		}
		if(e.getSource() == this.selectorPanel)
		{
			TreeMap<String,Experiment> experiments = JEXStatics.jexManager.getExperimentTree();
			new SelectorPlugInTester(experiments);
		}
		if(e.getSource() == this.rScripterPanel)
		{
			@SuppressWarnings("unused")
			RScripter scripter = new RScripter();
		}
	}
}

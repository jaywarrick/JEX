package plugin.rscripter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.SimpleFileFilter;
import net.miginfocom.swing.MigLayout;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXWorkflow;
import Database.DataWriter.ValueWriter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import cruncher.JEXFunction;

public class ND2DatasetCreator implements PlugInController, ActionListener {
	
	public PlugIn dialog;
	public TreeSet<JEXEntry> entries;
	public TypeName tn;
	public JPanel main;
	public JPanel infos;
	public JPanel buttons;
	
	// Make the text fields
	public JTextArea expName = new JTextArea("", 5, 50);
	public JTextArea expInfo = new JTextArea("", 5, 50);
	public JTextArea expDate = new JTextArea("", 5, 50);
	public JTextArea directory = new JTextArea("", 5, 50);
	public JTextArea image = new JTextArea("", 5, 50);
	public JTextArea imRows = new JTextArea("", 5, 50);
	public JTextArea imCols = new JTextArea("", 5, 50);
	
	// Make the buttons
	public JButton create = new JButton("Create");
	public JButton cancel = new JButton("Cancel");
	public JButton fileButton = new JButton("...");
	
	public ND2DatasetCreator()
	{
		this.dialog = new PlugIn(this);
		this.dialog.setBounds(100, 100, 500, 400);
		initialize();
		this.dialog.setVisible(true);
	}
	
	private void initialize()
	{				
		// Create the buttons panel
		this.buttons = new JPanel();
		this.buttons.setBackground(DisplayStatics.lightBackground);
		this.buttons.setLayout(new MigLayout("flowx, ins 0", "[fill,grow]3", "[50]3"));
		this.create = new JButton("Create");
		this.cancel = new JButton("Cancel");
		this.create.addActionListener(this);
		this.cancel.addActionListener(this);
		this.buttons.add(this.cancel, "growx");
		this.buttons.add(this.create, "growx");
		
		// Create the infos panel and add elements
		this.infos = new JPanel();
		this.infos.setBackground(DisplayStatics.lightBackground);
		this.infos.setLayout(new MigLayout("flowy", "[fill,grow]", "[][][][][][][]"));
		this.infos.add(getInfo("Experiment Name", this.expName, false),  "growx");
		this.infos.add(getInfo("Experiment Date", this.expDate, false),  "growx");
		this.infos.add(getInfo("Experiment Info", this.expInfo, true),  "growx");
		this.infos.add(this.getChooserPanel("ND2 File Directory", this.directory, this.fileButton), "growx");
		this.infos.add(getInfo("Image Name", this.image, false),  "growx");
		this.infos.add(getInfo("ImRows", this.imRows, false),  "growx");
		this.infos.add(getInfo("ImRows", this.imCols, false),  "growx");
		
		// Create the main panel and add elements
		this.main = new JPanel();
		this.main.setBackground(DisplayStatics.lightBackground);
		this.main.setLayout(new MigLayout("flowy, ins 3", "[fill,grow,left]", "[grow]3[50]"));
		this.main.add(this.infos, "grow");
		this.main.add(this.buttons, "growx");
		
		Container contents = this.dialog.getContentPane();
		contents.setLayout(new BorderLayout());
		contents.add(this.main, BorderLayout.CENTER);
	}
	
	private JPanel getChooserPanel(String name, JTextArea area, JButton fileButton)
	{
		// This
		JPanel ret = new JPanel();
		ret.setBackground(DisplayStatics.lightBackground);
		ret.setLayout(new MigLayout("flowx, ins 2", "[150:0:,left]5[0:0,fill,grow]3[]", "[]"));
		JLabel label = new JLabel(name);
		ret.add(label, "");
		fileButton.addActionListener(this);
		area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
		ret.add(area, "grow, width 0:0:, height 20:0");
		ret.add(fileButton, "grow, width 40:0, height 20:0");
		return ret;
	}
	
	private static Component getInfo(String name, JTextArea area, boolean wrap)
	{
		JPanel ret = new JPanel();
		ret.setBackground(DisplayStatics.lightBackground);
		ret.setLayout(new MigLayout("flowx, ins 2", "[150:0,left]2[0:0,fill,grow]", "[]"));
		JLabel label = new JLabel(name);
		ret.add(label, "");
		
		if(wrap)
		{
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
			area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
			area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
			JScrollPane infoPane = new JScrollPane(area);
			infoPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			infoPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			infoPane.setBorder(BorderFactory.createEmptyBorder());
			ret.add(infoPane, "grow, width 0:0:, height 70:1000");
		}
		else
		{
			ret.add(area, "grow, width 0:0:, height 20:0");
			area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
			area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
		}
		return ret;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		if(arg0.getSource() == this.create)
		{
			Logs.log("Creating new data sets", 1, this);
			
			File dir = new File(this.directory.getText().trim());
			if(!dir.exists() || !dir.isDirectory())
			{
				JOptionPane.showMessageDialog(this.dialog, "Must select an existing folder (i.e., must exist and can't be a file).");
			}
			else
			{
				File[] files = getND2Files(dir);
				int w = files.length;
				int h = 1;
				
				String expNameStr = this.expName.getText().trim();
				String expInfoStr = this.expInfo.getText().trim();
				String dateStr = this.expDate.getText().trim();
				
				JEXStatics.jexManager.createEntryArray(expNameStr, dateStr, expInfoStr, w, h);
				this.entries = JEXStatics.jexManager.getExperimentTree().get(expNameStr).entries;
				JEXStatics.jexManager.setViewedExperiment(this.expName.getText().trim());
				JEXStatics.jexManager.setSelectedEntries(this.entries);
				this.saveValueObject(files);
				this.runImport();
				this.dialog.setVisible(false);
				this.dialog.dispose();
			}
		}
		if(arg0.getSource() == this.fileButton)
		{
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int returnVal = fc.showOpenDialog(JEXStatics.main);
			String directory = null;
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					directory = fc.getSelectedFile().getCanonicalPath();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return;
				}
				if(directory == null)
				{
					return;
				}
			}
			this.directory.setText(directory);
		}
		if(arg0.getSource() == this.cancel)
		{
			this.dialog.setVisible(false);
			this.dialog.dispose();
		}
	}
	
	private File[] getND2Files(File dir)
	{
		FileFilter filter = new SimpleFileFilter(new String[]{".nd2"});
		File[] ret = dir.listFiles(filter);
		return ret;
	}
	
	public void finalizePlugIn()
	{   
		
	}
	
	public PlugIn plugIn()
	{
		return this.dialog;
	}
	
	private void saveValueObject(File[] files)
	{
		TreeMap<JEXEntry,JEXData> dataArray = new TreeMap<JEXEntry,JEXData>();
		TreeMap<DimensionMap,JEXEntry> selectedEntries = new TreeMap<DimensionMap,JEXEntry>();
		
		// Organize the selected entries by row and col
		for (JEXEntry entry : this.entries)
		{
			DimensionMap map = new DimensionMap();
			map.put("X", "" + entry.getTrayX());
			map.put("Y", "" + entry.getTrayY());
			selectedEntries.put(map, entry);
		}
		
		// iterator goes Through them in dimensionMap sorted order
		int count = 0;
		for (JEXEntry target : selectedEntries.values())
		{
			JEXData value = ValueWriter.makeValueObject(this.image.getText().trim(), files[count].getAbsolutePath());
			dataArray.put(target, value);
			count = count + 1;
		}
		JEXStatics.jexDBManager.saveDataInEntries(dataArray);
		JEXStatics.jexManager.saveCurrentDatabase();
	}
	
	public void runImport()
	{
		JEXFunction function;
		try
		{
			function = new JEXFunction("Import ND2 Files");
			function.setInput("Path", new TypeName(JEXData.VALUE, this.image.getText().trim()));
			ParameterSet params = function.getParameters();
			params.setValueOfParameter("ImRows", this.imRows.getText().trim());
			params.setValueOfParameter("ImCols", this.imCols.getText().trim());
			function.setExpectedOutputName(0, this.image.getText().trim());
			JEXWorkflow wf = new JEXWorkflow("Import ND2 Files");
			wf.add(function);
			JEXStatics.main.displayFunctionPane();
			JEXStatics.cruncher.runWorkflow(wf, this.entries, true);
		}
		catch (InstantiationException e)
		{
			JOptionPane.showMessageDialog(this.dialog, "Couldn't find the 'Import ND2 Files' function!");
			e.printStackTrace();
		}
	}
}

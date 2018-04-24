package plugin.rscripter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.LabelReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import Database.SingleUserDatabase.tnvi;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.LSVList;
import miscellaneous.Pair;
import miscellaneous.StringUtility;
import miscellaneous.TSVList;
import net.miginfocom.swing.MigLayout;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import rtools.R;
import tables.DimensionMap;

public class RScripter implements PlugInController, ActionListener, ClipboardOwner {
	
	public PlugIn dialog;
	public TreeSet<JEXEntry> entries;
	public TypeName tn;
	
	public JTextArea code;
	public JPanel main;
	public JScrollPane scroll;
	public JPanel buttons;
	public JButton getFileTable;
	public JButton tableFromText;
	public JButton valuesFromText;
	public JButton getJEXDatas;
	public JTextField valueObjectName;
	public JLabel buttonHeader;
	
	public RScripter()
	{
		this.dialog = new PlugIn(this);
		this.dialog.setBounds(100, 100, 500, 400);
		initialize();
		this.dialog.setVisible(true);
	}
	
	private void initialize()
	{
		this.main = new JPanel();
		this.main.setLayout(new MigLayout("flowx, ins 3", "[fill,grow]3[50]", "[fill,grow]"));
		this.code = new JTextArea();
		this.scroll = new JScrollPane(this.code);
		this.code.setLineWrap(true);
		this.main.add(this.scroll, "grow");
		this.buttons = new JPanel();
		this.buttons.setLayout(new MigLayout("flowy, ins 0", "[]", "[]3[]"));
		this.main.add(this.buttons, "grow");
		
		this.buttonHeader = new JLabel("Script Macros");
		this.getFileTable = new JButton("Get File Table");
		this.tableFromText = new JButton("Table From Text");
		this.valuesFromText = new JButton("Values From Text");
		this.getJEXDatas = new JButton("Get JEX Datas");
		this.valueObjectName = new JTextField("ValueName");
		
		this.buttons.add(buttonHeader, "growx");
		this.buttons.add(getFileTable, "growx");
		this.buttons.add(tableFromText, "growx");
		this.buttons.add(valuesFromText, "growx");
		this.buttons.add(getJEXDatas, "growx");
		this.buttons.add(valueObjectName, "growx");
		
		// Add button here
		
		this.getFileTable.addActionListener(this);
		this.tableFromText.addActionListener(this);
		this.valuesFromText.addActionListener(this);
		this.getJEXDatas.addActionListener(this);
		
		Container contents = this.dialog.getContentPane();
		contents.setLayout(new BorderLayout());
		contents.add(this.main, BorderLayout.CENTER);
		
	}
	
	public void setDBSelection(TreeSet<JEXEntry> entries, TypeName tn)
	{
		this.entries = entries;
		this.tn = tn;
	}
	
	public void finalizePlugIn()
	{   
		
	}
	
	public PlugIn plugIn()
	{
		return this.dialog;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		this.setDBSelection(JEXStatics.jexManager.getSelectedEntries(), JEXStatics.jexManager.getSelectedObject());
		if(e.getSource() == this.getFileTable)
		{
			if(this.entries != null && this.entries.size() > 0 && this.tn != null)
			{
				String command = getRScript_FileTable(this.entries, this.tn);
				if(command != null)
				{
					this.code.setText(command);
					this.code.selectAll();
					StringSelection stringSelection = new StringSelection(command);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, this);
				}
			}
		}
		if(e.getSource() == this.getJEXDatas)
		{
			if(this.entries != null && this.entries.size() > 0 && this.tn != null)
			{
				String command = getRScript_FileTable(this.entries, this.tn);
				if(command != null)
				{
					this.code.setText(command);
					this.code.selectAll();
					StringSelection stringSelection = new StringSelection(command);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, this);
				}
			}
		}
		if(e.getSource() == this.tableFromText)
		{
			String tableText = this.code.getText();
			String path = JEXWriter.saveText(tableText, "txt");
			String command = "";
			if(StringUtility.isNumeric(tableText.substring(0, 1)))
			{
				command = "read.table(" + R.quotedPath(path) + ", header = FALSE, sep = '\\t', quote = \"\\\"'\", dec = '.', comment.char = '#', stringsAsFactors = default.stringsAsFactors(), na.strings = 'NA', strip.white = FALSE, blank.lines.skip = TRUE)";
			}
			else
			{
				command = "read.table(" + R.quotedPath(path) + ", header = TRUE, sep = '\\t', quote = \"\\\"'\", dec = '.', comment.char = '#', stringsAsFactors = default.stringsAsFactors(), na.strings = 'NA', strip.white = FALSE, blank.lines.skip = TRUE)";
			}
			this.code.setText(command);
			this.code.selectAll();
			StringSelection stringSelection = new StringSelection(command);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, this);
		}
		if(e.getSource() == this.valuesFromText)
		{
			Object[] options = { "OK", "Cancel" };
			boolean foundDuplicate = false;
			for (JEXEntry entry : this.entries)
			{
				if(JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.VALUE, this.valueObjectName.getText()), entry) != null)
				{
					foundDuplicate = true;
					break;
				}
			}
			if(foundDuplicate)
			{
				int n = JOptionPane.showOptionDialog(JEXStatics.main, "An object with the name \"" + this.valueObjectName.getText() + "\" was found in at least \none of the selected entries. Should these be overwritten?", "Creating value object: Overwrite?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(n == 1)
				{
					Logs.log("Canceling edit to prevent overwriting existing data.", 0, this);
					JEXStatics.statusBar.setStatusText("Canceling object edit to prevent overwriting existing data.");
					return;
				}
			}
			TreeMap<JEXEntry,JEXData> dataArray = new TreeMap<JEXEntry,JEXData>();
			TreeMap<DimensionMap,JEXEntry> selectedEntries = new TreeMap<DimensionMap,JEXEntry>();
			
			// Organize the selected entries by row and col
			TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
			for (JEXEntry entry : entries)
			{
				DimensionMap map = new DimensionMap();
				map.put("X", "" + entry.getTrayX());
				map.put("Y", "" + entry.getTrayY());
				selectedEntries.put(map, entry);
			}
			
			String tableText = this.code.getText();
			LSVList rows = new LSVList(tableText);
			for (int y = 0; y < rows.size(); y++)
			{
				TSVList cols = new TSVList(rows.get(y));
				for (int x = 0; x < cols.size(); x++)
				{
					DimensionMap targetMap = new DimensionMap();
					targetMap.put("X", "" + x);
					targetMap.put("Y", "" + y);
					JEXEntry target = selectedEntries.get(targetMap);
					if(target != null)
					{
						JEXData value = ValueWriter.makeValueObject(this.valueObjectName.getText(), cols.get(x));
						dataArray.put(target, value);
					}
				}
			}
			JEXStatics.jexDBManager.saveDataInEntries(dataArray);
		}
	}
	
	public static String getRScript_FileTable(TreeSet<JEXEntry> entries, TypeName tn)
	{
		
		//		TreeMap<DimensionMap,String> files = new TreeMap<DimensionMap,String>();
		//		DimTable totalDimTable = new DimTable();
		//		TreeSet<String> expts = new TreeSet<String>();
		//		// TreeSet<String> trays = new TreeSet<String>();
		//		TreeSet<String> xs = new TreeSet<String>();
		//		TreeSet<String> ys = new TreeSet<String>();
		
		LSVList lsv = new LSVList();
		lsv.add("sourceGitHubFile <- function(user, repo, branch, file){");
		lsv.add("require(curl)");
		lsv.add("destfile <- tempfile()");
		lsv.add("fileToGet <- paste0('https://raw.githubusercontent.com/', user, '/', repo, '/', branch, '/', file)");
		lsv.add("curl_download(url=fileToGet, destfile)");
		lsv.add("source(destfile)");
		lsv.add("}");
		lsv.add("");
		lsv.add("library(data.table)");
		lsv.add("library(bit64)");
		lsv.add("sourceGitHubFile('jaywarrick','R-General','master','.Rprofile')");
		lsv.add("jData <- list()");
		for (JEXEntry e : entries)
		{
			
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, e);
			if(data != null)
			{
				String dataS = getJEXDataAsRString(e, tn);
				lsv.add("jData[[" + R.sQuote(e.getEntryID()) + "]] <- " + dataS);
				//				DimTable tempDimTable = data.getDimTable();
				//				totalDimTable = DimTable.union(totalDimTable, tempDimTable);
				//				if(tn.getType().matches(JEXData.FILE))
				//				{
				//					TreeMap<DimensionMap,String> temp = FileReader.readObjectToFilePathTable(data);
				//					for (DimensionMap map : temp.keySet())
				//					{
				//						DimensionMap newMap = map.copy();
				//						String expt = e.getEntryExperiment();
				//						// String tray = e.getEntryTrayName();
				//						int x = e.getTrayX();
				//						int y = e.getTrayY();
				//						newMap.put("Experiment", expt);
				//						// newMap.put("Tray", tray);
				//						newMap.put("X", "" + x);
				//						newMap.put("Y", "" + y);
				//						expts.add(expt);
				//						// trays.add(tray);
				//						xs.add("" + x);
				//						ys.add("" + y);
				//						files.put(newMap, temp.get(map));
				//					}
				//				}
				//				else
				//				{
				//					DimensionMap newMap = new DimensionMap();
				//					String expt = e.getEntryExperiment();
				//					// String tray = e.getEntryTrayName();
				//					int x = e.getTrayX();
				//					int y = e.getTrayY();
				//					newMap.put("Experiment", expt);
				//					// newMap.put("Tray", tray);
				//					newMap.put("X", "" + x);
				//					newMap.put("Y", "" + y);
				//					expts.add(expt);
				//					// trays.add(tray);
				//					xs.add("" + x);
				//					ys.add("" + y);
				//					String path = JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath();
				//					files.put(newMap, path);
				//				}
			}
		}
		lsv.add("jData <- rbindlist(jData)");
		lsv.add("x <- readJEXDataTables(jData)");
		//		Dim exptDim = new Dim("Experiment");
		//		exptDim.dimValues.addAll(expts);
		//		// Dim trayDim = new Dim("Tray");
		//		// trayDim.dimValues.addAll(trays);
		//		Dim xDim = new Dim("X");
		//		xDim.dimValues.addAll(xs);
		//		Dim yDim = new Dim("Y");
		//		yDim.dimValues.addAll(ys);
		//		DimTable newTable = new DimTable();
		//		newTable.add(exptDim);
		//		// newTable.add(trayDim);
		//		newTable.add(xDim);
		//		newTable.add(yDim);
		//		newTable.addAll(totalDimTable);
		//		
		//		String path = JEXTableWriter.writeTable("tempFileTable", new Table<String>(newTable, files));
		//		LSVList commands = new LSVList();
		//		commands.add("library(foreign)");
		//		commands.add("fileTable <- read.arff(" + R.quotedPath(path) + ")");
		
		return lsv.toString();
	}
	
	public static Vector<Pair<String,String>> getLabelsInEntry(JEXEntry e)
	{
		Vector<TypeName> tns = new Vector<>();
		Vector<Pair<String,String>> ret = new Vector<>();
		TreeSet<JEXEntry> set = new TreeSet<>();
		set.add(e);
		tnvi a = JEXStatics.jexManager.getTNVIforEntryList(set);
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> labels = a.get(JEXData.LABEL);
		for(String name : labels.keySet())
		{
			tns.add(new TypeName(JEXData.LABEL, name));
		}
		for(TypeName tn : tns)
		{
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, e);
			ret.add(new Pair<String,String>(LabelReader.readLabelName(data), LabelReader.readLabelValue(data)));
		}
		return ret;
	}
	
	private static String getListRCommand(Vector<Pair<String,String>> listItems)
	{
		String ret = "list(";
		boolean first = true;
		for(Pair<String,String> item : listItems)
		{
			if(first)
			{
				ret = ret + item.p1 + "=" + R.sQuote(item.p2);
			}
			else
			{
				ret = ret + "," + item.p1 + "=" + R.sQuote(item.p2);
			}
			first = false;
		}
		ret = ret + ")";
		return ret;
	}
	
	public static String getJEXDataAsRString(JEXEntry e, TypeName tn)
	{
		Vector<Pair<String,String>> labels = getLabelsInEntry(e);
		String listC = getListRCommand(labels);
		String ret = "readJEXData(dbPath=" + R.sQuote(JEXWriter.getDatabaseFolder()) + ", ds=" + R.sQuote(e.getEntryExperiment()) + ", x=" + e.getTrayX() + ", y=" + e.getTrayY() + ", type=" + R.sQuote(tn.getType().getType()) + ", name=" + R.sQuote(tn.getName()) + ", " + listC + ")";
		ret = ret.replace("\\", "/"); // R likes forward slashes.
		return ret;
	}
	
	public void lostOwnership(Clipboard arg0, Transferable arg1)
	{
		// DO NOTHING
	}
	
}

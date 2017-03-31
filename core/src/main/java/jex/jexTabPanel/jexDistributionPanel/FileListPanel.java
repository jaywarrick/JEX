package jex.jexTabPanel.jexDistributionPanel;

import guiObject.DialogGlassCenterPanel;
import guiObject.JTickedComponent;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.FileUtility;
import miscellaneous.FontUtility;
import miscellaneous.StringUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class FileListPanel extends DialogGlassCenterPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	private JList<File> displayListOfFiles;
	private JButton loadButton = new JButton("OK");
	private JButton refreshButton = new JButton("Re-filter");
	private JTickedComponent foldersOrFilesView;
	private JTickedComponent reverseFileOrder;
	private JTickedComponent skipFiles;
	private JTickedComponent skipFilesStart;
	private JTickedComponent grabSequence;
	private JTickedComponent takeOnlyFilesWithExtension;
	private JTickedComponent takeOnlyFilesWithPrefix;
	private JTickedComponent takeOnlyFilesWithSuffix;
	private JTickedComponent fileEndsWithNumber;
	
	File[] listOfFiles = null;
	File[] viewedFiles = null;
	public List<File> files2Distribute;
	private static String folderVisited = null;
	private JEXDistributionPanelController controller;
	
	public FileListPanel(JEXDistributionPanelController controller)
	{
		this.controller = controller;
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowy, ins 10, gapy 0", "[fill,grow]", "[]3[]0[fill,grow]0[]"));
		this.setBackground(DisplayStatics.lightBackground);
		files2Distribute = new ArrayList<File>();
		
		displayListOfFiles = new JList<File>();
		displayListOfFiles.setBackground(DisplayStatics.lightBackground);
		displayListOfFiles.setFont(FontUtility.defaultFonts);
		displayListOfFiles.setCellRenderer(new FileListCellRenderer());
		
		DefaultListModel<File> newModel = new DefaultListModel<File>();
		if(files2Distribute == null)
			files2Distribute = new Vector<File>();
		for (File f : files2Distribute)
		{
			newModel.addElement(f);
		}
		displayListOfFiles.setModel(newModel);
		
		JScrollPane fileListScroll = new JScrollPane(displayListOfFiles);
		fileListScroll.setBackground(DisplayStatics.lightBackground);
		fileListScroll.setBorder(BorderFactory.createLineBorder(DisplayStatics.dividerColor));
		
		loadButton.setText("Load");
		loadButton.addActionListener(this);
		refreshButton.addActionListener(this);
		
		foldersOrFilesView = new JTickedComponent("Folder/Files View (ON = Files)", new JLabel(""));
		foldersOrFilesView.setSelected(true);
		reverseFileOrder = new JTickedComponent("Reverse file order", new JLabel(""));
		skipFiles = new JTickedComponent("Keep a file every ", new JTextField("2"));
		skipFilesStart = new JTickedComponent("Start with file no. ", new JTextField("1"));
		grabSequence = new JTickedComponent("Grab suffix in range", new JTextField("1,100"));
		takeOnlyFilesWithExtension = new JTickedComponent("Keep files with extension ", new JTextField("tif"));
		takeOnlyFilesWithPrefix = new JTickedComponent("Keep files with prefix ", new JTextField("Image"));
		takeOnlyFilesWithSuffix = new JTickedComponent("Keep files with suffix ", new JTextField("_BF"));
		fileEndsWithNumber = new JTickedComponent("Reorder using number at end of file name ", new JLabel(""));
		foldersOrFilesView.setBackground(DisplayStatics.lightBackground);
		reverseFileOrder.setBackground(DisplayStatics.lightBackground);
		skipFiles.setBackground(DisplayStatics.lightBackground);
		skipFilesStart.setBackground(DisplayStatics.lightBackground);
		grabSequence.setBackground(DisplayStatics.lightBackground);
		takeOnlyFilesWithExtension.setBackground(DisplayStatics.lightBackground);
		takeOnlyFilesWithPrefix.setBackground(DisplayStatics.lightBackground);
		takeOnlyFilesWithSuffix.setBackground(DisplayStatics.lightBackground);
		fileEndsWithNumber.setBackground(DisplayStatics.lightBackground);
		
		this.add(foldersOrFilesView, "left");
		this.add(loadButton, "growx");
		this.add(fileListScroll, "grow");
		this.add(reverseFileOrder, "growx");
		this.add(skipFiles, "growx");
		this.add(skipFilesStart, "growx");
		this.add(grabSequence, "growx");
		this.add(takeOnlyFilesWithExtension, "growx");
		this.add(takeOnlyFilesWithPrefix, "growx");
		this.add(takeOnlyFilesWithSuffix, "growx");
		this.add(fileEndsWithNumber, "growx");
		this.add(refreshButton, "growx");
		this.setMaximumSize(new Dimension(300, 600));
		
		SSCenter.defaultCenter().connect(foldersOrFilesView, JTickedComponent.SIG_SelectionChanged_NULL, this, "fileViewToggled", (Class[]) null);
	}
	
	/**
	 * Called when clicked yes on the dialog panel
	 */
	public void yes()
	{
		Logs.log("File list selected", 1, this);
		controller.fileListChanged();
	}
	
	/**
	 * Called when clicked cancel on the dialog panel
	 */
	public void cancel()
	{
		Logs.log("Repository creation canceled", 1, this);
		
	}
	
	/**
	 * Reorder the loaded file list
	 */
	public void reOrderFileList()
	{
		boolean boolReverseFileOrder = reverseFileOrder.isTicked();
		boolean boolSkipFiles = skipFiles.isTicked();
		boolean boolTakeOnlyFilesWithExtension = takeOnlyFilesWithExtension.isTicked();
		boolean boolTakeOnlyFilesWithPrefix = takeOnlyFilesWithPrefix.isTicked();
		boolean boolTakeOnlyFilesWithSuffix = takeOnlyFilesWithSuffix.isTicked();
		boolean boolFileEndsWithNumber = fileEndsWithNumber.isTicked();
		boolean boolGrabSequence = grabSequence.isTicked();
		
		List<File> newList;
		// Shall we reverse the file list?
		if(boolReverseFileOrder)
		{
			newList = new Vector<File>();
			for (File f : files2Distribute)
			{
				newList.add(0, f);
			}
			files2Distribute = newList;
		}
		
		// Shall we skip one every several files?
		if(boolSkipFiles)
		{
			int skip = Integer.parseInt(skipFiles.getValue());
			int start = Integer.parseInt(skipFilesStart.getValue());
			int length = files2Distribute.size() / skip;
			newList = new Vector<File>();
			for (int i = 0; i < length; i++)
			{
				newList.add(files2Distribute.get(skip * i + start - 1));
			}
			files2Distribute = newList;
		}
		
		if(boolGrabSequence)
		{
			CSVList sequence = new CSVList(grabSequence.getValue());
			int start = Integer.parseInt(sequence.get(0));
			int end = Integer.parseInt(sequence.get(1));
			if(sequence.size() > 1)
			{
				newList = new Vector<File>();
				for (File f : files2Distribute)
				{
					// grab the number suffix if available
					String suffix = FileUtility.getFileNameSuffixDigits(f.getName());
					if(!suffix.equals(""))
					{
						int suffixInt = Integer.parseInt(suffix);
						if(suffixInt >= start && suffixInt <= end)
						{
							newList.add(f);
						}
					}
				}
				files2Distribute = newList;
			}
		}
		
		// Shall we filter files based on their extension?
		if(boolTakeOnlyFilesWithExtension)
		{
			String extension = takeOnlyFilesWithExtension.getValue();
			newList = new Vector<File>();
			for (File f : files2Distribute)
			{
				String thisExtention = FileUtility.getFileNameExtension(f.getName());
				if(extension.equals(thisExtention))
				{
					newList.add(f);
				}
			}
			files2Distribute = newList;
		}
		
		// Select only the files with a given prefix
		if(boolTakeOnlyFilesWithPrefix)
		{
			String prefix = takeOnlyFilesWithPrefix.getValue();
			newList = new Vector<File>();
			for (File f : files2Distribute)
			{
				if(prefix.length() <= f.getName().length())
				{
					String thisPrefix = f.getName().substring(0, prefix.length());
					if(prefix.equals(thisPrefix))
					{
						newList.add(f);
					}
				}
			}
			files2Distribute = newList;
		}
		
		// Select only the files with a given suffix
		if(boolTakeOnlyFilesWithSuffix)
		{
			String suffix = takeOnlyFilesWithSuffix.getValue();
			newList = new Vector<File>();
			for (File f : files2Distribute)
			{
				if(FileUtility.getFileNameWithoutExtension(f.getName()).endsWith(suffix))
				{
					newList.add(f);
				}
			}
			files2Distribute = newList;
		}
		
		if(boolFileEndsWithNumber)
		{
			TreeSet<File> files = new TreeSet<File>();
			for (File f : files2Distribute)
			{
				File dest = reNameFile(f, 4);
				files.add(dest);
			}
			newList = new Vector<File>();
			for (File f : files)
			{
				newList.add(f);
			}
			files2Distribute = newList;
		}
		
		// Update the JList
		DefaultListModel<File> newModel = new DefaultListModel<File>();
		for (File f : files2Distribute)
		{
			newModel.addElement(f);
		}
		displayListOfFiles.setModel(newModel);
		displayListOfFiles.repaint();
	}
	
	private File reNameFile(File f, int size)
	{
		String fpath = f.getParent();
		String fileName = f.getName().substring(0, f.getName().length() - 4);
		
		int index = fileName.length() - 1;
		while (Character.isDigit(fileName.charAt(index)) && index >= 0)
		{
			index = index - 1;
		}
		
		if(index >= fileName.length() - 1)
			return f;
		String fPrefix = fileName.substring(0, index + 1);
		String fIndex = StringUtility.fillLeft(fileName.substring(index + 1), size, "0");
		String fExtension = f.getName().substring(f.getName().length() - 4);
		
		String newName = fpath + File.separator + fPrefix + fIndex + fExtension;
		File dest = new File(newName);
		f.renameTo(dest);
		
		Logs.log("Moved file " + f.getPath() + " to new path " + dest.getPath(), 1, this);
		return dest;
	}
	
	/**
	 * Add files to the list of files selected
	 * 
	 * @return
	 */
	public void selectFiles()
	{
		// File f = new File("/Volumes/shared/")
		JFileChooser fc = null;
		if(folderVisited != null)
		{
			fc = new JFileChooser(folderVisited);
		}
		else
		{
			fc = new JFileChooser();
		}
		// JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(true);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int returnVal = fc.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			Logs.log("Treating file list...", 1, this);
			File[] temp = fc.getSelectedFiles();
			if(temp != null)
			{
				listOfFiles = temp;
				this.setFiles2Distribute();
			}
		}
		else
		{
			Logs.log("File opening cancelled / not possible...", 1, this);
		}
	}
	
	public void fileViewToggled()
	{
		this.setFiles2Distribute();
	}
	
	private void setFiles2Distribute()
	{
		if(listOfFiles == null || listOfFiles.length == 0)
			return;
		List<File> filteredList = new ArrayList<File>(0);
		if(foldersOrFilesView.isTicked()) // get selected files and files in
		// selected folders
		{
			for (File f : listOfFiles)
			{
				folderVisited = f.getParent();
				if(f.isDirectory())
				{
					for (File child : f.listFiles())
					{
						filteredList.add(child);
					}
				}
				else
				{
					filteredList.add(f);
				}
			}
		}
		else
		{
			for (File f : listOfFiles)
			{
				folderVisited = f.getParent();
				if(f.isDirectory())
				{
					filteredList.add(f);
				}
			}
		}
		FileUtility.sortFileList(filteredList);
		files2Distribute = new Vector<File>();
		files2Distribute.addAll(filteredList);
		;
		// Update the JList
		DefaultListModel<File> newModel = new DefaultListModel<File>();
		for (File f : files2Distribute)
		{
			newModel.addElement(f);
		}
		displayListOfFiles.setModel(newModel);
		displayListOfFiles.repaint();
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == loadButton)
		{
			Logs.log("Opening file loader...", 1, this);
			selectFiles(); // sets files2Distribute
		}
		if(e.getSource() == refreshButton)
		{
			reOrderFileList();
		}
	}
}

package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import Database.Definition.Parameter;
import guiObject.JLabeledComboBox;
import guiObject.JLabeledTextField;
import guiObject.JParameterPanel;
import jex.statics.DisplayStatics;
import jex.statics.JEXDialog;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import tables.DimTable;

public class JEXDistributionRightPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	// Manual distribution method
	public JEXDistributionPanelController parentController;

	// Array distribution buttons and fields
	private String[] typeStr = { "Choose", "Image", "Movie", "File", "Value", "Valid", "Voice note" };
	private JLabeledComboBox typebox = new JLabeledComboBox("Type of object:", typeStr);
	private String[] dimStr = { "0D (eg. image)", "1D (eg. image stack)", "2D (eg. image array)", "3D (e.g array timelapse)", "4D", "5D" };
	private JLabeledComboBox dimensionField = new JLabeledComboBox("Dimension:", dimStr);

	private JButton filesButton = new JButton("Choose files");
	private JButton viewResultButton = new JButton("Deal files !");
	private JPanel quickDistribute = new JPanel();
	private JList<File> displayListOfFiles;

	// Object creation
	private JLabeledTextField lastSelected = new JLabeledTextField("Object Name:", "New object");
	private JLabeledTextField infoField = new JLabeledTextField("Info:", "");
	private JLabeledTextField dimNameField = new JLabeledTextField("Manual Dim:", "T");

	private int minHeight = 20;
	private JParameterPanel startPt = new JParameterPanel(new Parameter("First Drop Location", "Choose where in the array the first files will be dropped.", Parameter.DROPDOWN, new String[]{"UL","UR","LL","LR"}), 100);
	//	private JParameterPanel firstMove = new JParameterPanel(new Parameter("First Indexing Direction", "Choose which way to 'move' first when indexing to the next array location.", Parameter.DROPDOWN, new String[]{"Horizontal","Vertical"}), 100);
	private JParameterPanel snaking = new JParameterPanel(new Parameter("Snaking?", "Choose if indexing should be done in a snaking pattern.", Parameter.CHECKBOX, true), 20);

	private JParameterPanel rows = new JParameterPanel(new Parameter("Tile Rows", "How many rows of tiles are there per image (must be >= 1). If rows and cols are both <=1, no tiles are created.", Parameter.TEXTFIELD, "1"), 100);
	private JParameterPanel cols = new JParameterPanel(new Parameter("Tile Cols", "How many cols of tiles are there per image (must be >= 1). If rows and cols are both <=1, no tiles are created.", Parameter.TEXTFIELD, "1"), 100);
	private JParameterPanel overlap = new JParameterPanel(new Parameter("Tile Percent Overlap", "What is the percent overlap between the tiles.", Parameter.TEXTFIELD, "1.0"), 100);

	private JParameterPanel filter = new JParameterPanel(new Parameter("Exclusion Filter Table", "Which dimension values should be excluded upon import? Format is <DimName1>=<Val1>,<Val2>,...,<Valn>;<DimName2>=...", Parameter.TEXTFIELD, ""), 100);

	private JParameterPanel virtual = new JParameterPanel(new Parameter("Create Virtual Object?", "If no tiles are being created, should the object just refer to the original files (virtual, warning:experimental), or actually copy the files as usual to create 'real' objects.", Parameter.CHECKBOX, false), 20);

	JEXDistributionRightPanel(JEXDistributionPanelController parentController)
	{
		this.parentController = parentController;

		initialize();
	}

	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}

	private void initialize()
	{
		// Set the layout and background
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new MigLayout("flowy, ins 3", "[fill,grow]", ""));

		// Make the object type and dimension labels
		typebox.setBackground(DisplayStatics.lightBackground);
		typebox.setChangeActor(this);
		typebox.getElement().addActionListener(this);
		dimensionField.setBackground(DisplayStatics.lightBackground);
		dimensionField.setChangeActor(this);
		dimensionField.getElement().addActionListener(this);
		this.add(typebox, "growx");
		this.add(dimensionField, "growx");

		// Make the object creation labels
		lastSelected.setBackground(DisplayStatics.lightBackground);
		infoField.setBackground(DisplayStatics.lightBackground);
		dimNameField.setBackground(DisplayStatics.lightBackground);
		this.add(lastSelected, "growx");
		this.add(infoField, "growx");
		this.add(dimNameField, "growx");

		// Make a separator
		JLabel label = new JLabel("File import: Deal from list below");
		label.setFont(FontUtility.italicFonts);
		this.add(label, "growx");
		JLabel label2 = new JLabel("or drag and drop from finder into the array ");
		label2.setFont(FontUtility.italicFonts);
		this.add(label2, "growx");

		// Make the file list panel
		displayListOfFiles = new JList<File>();
		displayListOfFiles.setBackground(DisplayStatics.lightBackground);
		displayListOfFiles.setFont(FontUtility.defaultFonts);
		displayListOfFiles.setCellRenderer(new FileListCellRenderer());
		new FileListDropArea(displayListOfFiles);

		JScrollPane fileListScroll = new JScrollPane(displayListOfFiles);
		fileListScroll.setBackground(DisplayStatics.lightBackground);
		fileListScroll.setBorder(BorderFactory.createLineBorder(DisplayStatics.dividerColor));
		this.add(fileListScroll, "grow,height 100%");

		// Make the file chooser button
		filesButton.addActionListener(this);
		this.add(filesButton, "growx");

		// Make the dealing rules panel
		makeFileDistributionControlPanel();
		this.add(quickDistribute, "growx");

		// Make the distribution direction buttons.
		this.startPt.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.startPt.panel(), "growx");
		//		this.firstMove.panel().setMinimumSize(new Dimension(10, minHeight));
		//		this.add(this.firstMove.panel(), "growx");
		this.snaking.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.snaking.panel(), "growx");

		// Make the distribution direction buttons.
		this.overlap.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.overlap.panel(), "growx");
		this.rows.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.rows.panel(), "growx");
		this.cols.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.cols.panel(), "growx");

		this.filter.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.filter.panel(), "growx");

		this.virtual.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.virtual.panel(), "growx");

		// Make the deal button
		viewResultButton.addActionListener(this);
		this.add(viewResultButton, "growx");
	}

	/**
	 * Set the list of files
	 * 
	 * @param files
	 */
	public void setFileList(List<File> files)
	{
		DefaultListModel<File> newModel = new DefaultListModel<>();
		if(files == null)
			files = new Vector<File>();
		for (File f : files)
		{
			newModel.addElement(f);
		}
		displayListOfFiles.setModel(newModel);
		displayListOfFiles.repaint();
	}

	/**
	 * Rebuild the distribution panel
	 */
	public void rebuild()
	{
		// Remove all
		this.removeAll();

		// Set the layout and background
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new MigLayout("flowy, ins 3", "[fill,grow]", ""));

		// Make the object type and dimension labels
		typebox.setBackground(DisplayStatics.lightBackground);
		typebox.setChangeActor(this);
		dimensionField.setBackground(DisplayStatics.lightBackground);
		dimensionField.setChangeActor(this);
		this.add(typebox, "growx");
		this.add(dimensionField, "growx");

		// Make the object creation labels
		lastSelected.setBackground(DisplayStatics.lightBackground);
		infoField.setBackground(DisplayStatics.lightBackground);
		dimNameField.setBackground(DisplayStatics.lightBackground);
		this.add(lastSelected, "growx");
		this.add(infoField, "growx");
		this.add(dimNameField, "growx");

		// Make a separator
		JLabel label = new JLabel("File import: Deal from list below");
		label.setFont(FontUtility.italicFonts);
		this.add(label, "growx");
		JLabel label2 = new JLabel("or drag and drop from finder into the array ");
		label2.setFont(FontUtility.italicFonts);
		this.add(label2, "growx");

		// Make the file list panel
		displayListOfFiles = new JList<File>();
		displayListOfFiles.setBackground(DisplayStatics.lightBackground);
		displayListOfFiles.setFont(FontUtility.defaultFonts);
		displayListOfFiles.setCellRenderer(new FileListCellRenderer());
		new FileListDropArea(displayListOfFiles);
		JScrollPane fileListScroll = new JScrollPane(displayListOfFiles);
		fileListScroll.setBackground(DisplayStatics.lightBackground);
		fileListScroll.setBorder(BorderFactory.createLineBorder(DisplayStatics.dividerColor));
		this.add(fileListScroll, "grow,height 100%");

		// Make the file chooser button
		this.add(filesButton, "growx");

		// Make the dealing rules panel
		makeFileDistributionControlPanel();
		this.add(quickDistribute, "growx");

		// Make the distribution direction buttons.
		this.startPt.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.startPt.panel(), "growx");
		//		this.firstMove.panel().setMinimumSize(new Dimension(10, minHeight));
		//		this.add(this.firstMove.panel(), "growx");
		this.snaking.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.snaking.panel(), "growx");

		// Make the distribution direction buttons.
		this.overlap.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.overlap.panel(), "growx");
		this.rows.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.rows.panel(), "growx");
		this.cols.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.cols.panel(), "growx");

		// Make the filter text box
		this.filter.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.filter.panel(), "growx");

		// Make the virtual checkbox
		this.virtual.panel().setMinimumSize(new Dimension(10, minHeight));
		this.add(this.virtual.panel(), "growx");

		// Make the deal button
		this.add(viewResultButton, "growx");

		// Refresh gui
		this.invalidate();
		this.validate();
		this.repaint();
	}

	/**
	 * Make the command panel for file distribution
	 */
	public void makeFileDistributionControlPanel()
	{
		quickDistribute.setLayout(new MigLayout("flowy, ins 3, gapy 3", "[fill,grow]", "[]"));
		quickDistribute.setBackground(DisplayStatics.lightBackground);
		quickDistribute.removeAll();

		JLabel title = new JLabel("File dealing order:");
		quickDistribute.add(title, "growx");

		String[] dimPos = { "Array Row", "Array Column", "T", "Z", "R", "C" };
		String dimStr = dimensionField.getSelectedOption().toString();
		this.parentController.dimension = indexOf(dimStr, this.dimStr) + 2;
		this.parentController.dimensions = new HashMap<Integer,DimensionSelector>();
		for (int i = 0; i < this.parentController.dimension; i++)
		{
			DimensionSelector rulei = null;
			if(i == 0)
				rulei = new DimensionSelector(i + 1, "By: ", dimPos);
			else
				rulei = new DimensionSelector(i + 1, "By: ", dimPos);
			rulei.setAlignmentX(LEFT_ALIGNMENT);
			this.parentController.dimensions.put(new Integer(i), rulei);
			quickDistribute.add(rulei);
		}
	}

	// ----------------------------------------------------
	// --------- GETTERS AND SETTERS ----------------------
	// ----------------------------------------------------

	public String getObjectName()
	{
		return lastSelected.getText();
	}

	public String getObjectInfo()
	{
		return infoField.getText();
	}

	public String getObjectType()
	{
		return typebox.getSelectedOption().toString();
	}

	public boolean getSnaking()
	{
		return Boolean.parseBoolean(this.snaking.getValue());
	}

	//	public boolean getFirstMoveHorizontal()
	//	{
	//		return this.firstMove.getValue().equals("Horizontal");
	//	}

	public String getStartPt()
	{
		return this.startPt.getValue();
	}

	public double getOverlap()
	{
		return Double.parseDouble(this.overlap.getValue());
	}

	public int getRows()
	{
		Integer rows = Integer.parseInt(this.rows.getValue());
		if(rows < 1)
		{
			JEXDialog.messageDialog("The number of tile rows must be >= 1. Changing value to 1.", this);
			return 1;
		}
		return rows;
	}

	public int getCols()
	{
		Integer cols = Integer.parseInt(this.cols.getValue());
		if(cols < 1)
		{
			JEXDialog.messageDialog("The number of tile cols must be >= 1. Changing value to 1.", this);
			return 1;
		}
		return cols;
	}

	public DimTable getFilterTable()
	{
		return new DimTable(this.filter.getValue());
	}
	
	public Boolean getVirtual()
	{
		return Boolean.parseBoolean(this.virtual.getValue());
	}

	public String getManualDimensionName()
	{
		return dimNameField.getText();
	}

	public int getObjectDimension()
	{
		String dimStr = dimensionField.getSelectedOption().toString();
		int result = indexOf(dimStr, this.dimStr);
		return result;
	}

	// ----------------------------------------------------
	// --------- OTHER FUNCTIONS --------------------------
	// ----------------------------------------------------

	public void diplayPanel()
	{}

	public void stopDisplayingPanel()
	{}

	public JPanel getHeader()
	{
		return null;
	}

	/**
	 * Return the index of STR in LIST
	 * 
	 * @param str
	 * @param list
	 * @return
	 */
	private int indexOf(String str, String[] list)
	{
		for (int i = 0, len = list.length; i < len; i++)
		{
			String s = list[i];
			if(str.equals(s))
				return i;
		}
		return -1;
	}

	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------

	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.typebox.getElement() || e.getSource() == this.dimensionField.getElement())
		{
			rebuild();
		}
		if(e.getSource() == filesButton)
		{
			Logs.log("Opening filelist selection panel", 1, this);
			this.parentController.selecFiles();

			rebuild();
		}
		if(e.getSource() == viewResultButton)
		{
			Logs.log("Testing the distribution of files", 1, this);
			this.parentController.dealFiles();
		}
	}

	class FileListDropArea extends DropTargetAdapter {

		@SuppressWarnings("unused")
		private DropTarget dropTarget;
		private JList<File> area;

		public FileListDropArea(JList<File> area)
		{
			this.area = area;
			dropTarget = new DropTarget(this.area, DnDConstants.ACTION_COPY, this, true, null);
		}

		@SuppressWarnings("unchecked")
		public void drop(DropTargetDropEvent event)
		{

			try
			{
				if(event.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
				{
					Transferable tr = event.getTransferable();
					int action = event.getDropAction();
					event.acceptDrop(action);

					java.util.List<File> files = (java.util.List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
					parentController.addFiles2Distribute(files);

					event.dropComplete(true);
					Logs.log("Drop completed...", 1, this);

					return;
				}
				event.rejectDrop();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				event.rejectDrop();
			}
		}
	}
}
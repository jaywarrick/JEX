package jex.jexTabPanel.creationPanel;

import Database.DBObjects.JEXEntry;
import Database.Definition.HierarchyLevel;
import guiObject.SignalFlatRoundedButtonDeletable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.DateUtility;
import miscellaneous.FileUtility;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class JEXCreationPanel extends JPanel implements MouseListener, ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Model
	private ExperimentalTreeController creationArrayController;
	private HierarchyLevel hlevel;
	String initialExpName, initialExpInfo, initialExpDate;
	
	// GUI
	private JComboBox<Object> sortBy = new JComboBox<Object>(ExperimentalTreeController.SORT_OPTIONS);
	private SignalFlatRoundedButtonDeletable newArrayButton;
	
	JEXCreationPanel()
	{
		this.initialize();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this.creationArrayController);
		SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		// Create a new database button
		this.newArrayButton = new SignalFlatRoundedButtonDeletable("Add Dataset");
		this.newArrayButton.background = DisplayStatics.background;
		this.newArrayButton.normalBack = DisplayStatics.lightBackground;
		this.newArrayButton.mouseOverBack = DisplayStatics.lightMouseOverBackground;
		this.newArrayButton.selectedBack = DisplayStatics.menuBackground;
		this.newArrayButton.setLabelFont(FontUtility.boldFontl);
		this.newArrayButton.addMouseListener(this);
		
		// Initialize the gui components
		this.creationArrayController = new ExperimentalTreeController();
		this.creationArrayController.setTreeMode(ExperimentalTreeController.TREE_MODE_CREATION);
		JScrollPane scroll = new JScrollPane(this.creationArrayController.panel());
		scroll.setBorder(BorderFactory.createEmptyBorder());
		
		// Place the components in the split pane
		this.setLayout(new MigLayout("flowx, ins 0", "2[][fill,grow]2", "5[25:25]5[fill,grow]5[40:40]5"));
		this.setBackground(DisplayStatics.background);
		
		// Add the dataset creator button
		this.add(new JLabel("Sort datasets by: "), "height 25");
		this.add(this.sortBy, "height 25, wrap");
		this.sortBy.addActionListener(this);
		
		// Place the components in this panel
		this.add(scroll, "growx, span 2, wrap");
		
		// Add the dataset creator button
		this.add(this.newArrayButton, "growx, span 2, height 40");
		
		// Connect the signals
		SSCenter.defaultCenter().connect(this.creationArrayController, ExperimentalTreeController.DATA_EDIT, this, "edit", new Class[] { HierarchyLevel.class });
	}
	
	/**
	 * Display a modal dialog box with options to create a new array
	 */
	public void showDataSetCreationDialog()
	{
		DataSetCreator dataCreator = new DataSetCreator();
		dataCreator.setModal(true);
		dataCreator.setUndecorated(false);
		dataCreator.setBounds(400, 300, 450, 300);
		dataCreator.setVisible(true);
	}
	
	/**
	 * Display a modal dialog box with options to create a new array
	 */
	public void showDataEditCreationDialog()
	{
		DataSetEditor dataEditor = new DataSetEditor();
		dataEditor.setModal(true);
		dataEditor.setUndecorated(false);
		dataEditor.setBounds(400, 300, 450, 300);
		dataEditor.setVisible(true);
	}
	
	// ----------------------------------------------------
	// --------- EVENT FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void edit(HierarchyLevel hlevel)
	{
		Logs.log("Signal change for viewing a different hierarchy level received", 1, this);
		this.hlevel = hlevel;
		this.showDataEditCreationDialog();
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{}
	
	@Override
	public void mouseEntered(MouseEvent e)
	{}
	
	@Override
	public void mouseExited(MouseEvent e)
	{}
	
	@Override
	public void mousePressed(MouseEvent e)
	{}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		if(e.getSource() == this.newArrayButton)
		{
			this.showDataSetCreationDialog();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.sortBy)
		{
			Logs.log("Changing the dataset sorting options", 1, this);
			this.creationArrayController.setSortOption((String) this.sortBy.getSelectedItem());
			this.creationArrayController.rebuildModel();
		}
	}
	
	class DataSetCreator extends JDialog implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		JTextArea expName = new JTextArea("New experiment");
		JTextArea expInfo = new JTextArea("No Info", 5, 50);
		JTextArea expDate = new JTextArea(DateUtility.getDate());
		JTextArea trayWidth = new JTextArea("1");
		JTextArea trayHeight = new JTextArea("1");
		private JButton createButton;
		private JPanel createPanel;
		
		public DataSetCreator()
		{
			this.initialize();
		}
		
		private void initialize()
		{
			this.setLayout(new BorderLayout());
			this.setBackground(DisplayStatics.background);
			
			// Initialize buttons
			this.createButton = new JButton("Create Array");
			this.createButton.addActionListener(this);
			
			this.createPanel = this.makeCreateObjectPanel();
			
			JPanel p = (JPanel) this.getContentPane();
			p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			p.setBackground(DisplayStatics.background);
			
			this.add(this.createPanel, BorderLayout.CENTER);
			this.repaint();
		}
		
		public JPanel makeCreateObjectPanel()
		{
			JPanel result = new JPanel();
			
			// make the labels
			JLabel expNameLabel = new JLabel("Dataset Name");
			JLabel expInfoLabel = new JLabel("Notes");
			JLabel expDateLabel = new JLabel("Date");
			JLabel trayWidthLabel = new JLabel("Array Width");
			JLabel trayHeightLabel = new JLabel("Array Height");
			
			// Make the text fields
			this.expName = new JTextArea("Dataset Name");
			this.expInfo = new JTextArea("No notes.", 0, 0);
			this.expDate = new JTextArea(DateUtility.getDate());
			this.trayWidth = new JTextArea("1");
			this.trayHeight = new JTextArea("1");
			
			// Format the text fields
			this.expInfo.setLineWrap(true);
			this.expInfo.setWrapStyleWord(true);
			this.expName.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.expDate.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.trayWidth.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.trayHeight.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			JScrollPane infoPane = new JScrollPane(this.expInfo);
			infoPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			infoPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			infoPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			
			// Set the layout
			result.setLayout(new MigLayout("ins 3", "[0:0,75,left]2[0:0,fill,grow]", "[]2[grow,top]2[]2[]"));
			result.add(expNameLabel);
			result.add(this.expName, "growx, wrap");
			result.add(expInfoLabel);
			result.add(infoPane, "grow, wrap, width 0:0:");
			result.add(expDateLabel);
			result.add(this.expDate, "growx, wrap");
			result.add(trayWidthLabel);
			result.add(this.trayWidth, "growx, wrap");
			result.add(trayHeightLabel);
			result.add(this.trayHeight, "growx, wrap");
			result.add(this.createButton, "growx, span 2");
			
			return result;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			if(arg0.getSource() == this.createButton)
			{
				Logs.log("Creating new data sets", 1, this);
				
				String expNameStr = FileUtility.removeWhiteSpaceOnEnds(this.expName.getText());
				String expInfoStr = FileUtility.removeWhiteSpaceOnEnds(this.expInfo.getText());
				String dateStr = FileUtility.removeWhiteSpaceOnEnds(this.expDate.getText());
				String arrayWidthStr = FileUtility.removeWhiteSpaceOnEnds(this.trayWidth.getText());
				String arrayHeightStr = FileUtility.removeWhiteSpaceOnEnds(this.trayHeight.getText());
				int w = Integer.parseInt(arrayWidthStr);
				int h = Integer.parseInt(arrayHeightStr);
				
				JEXStatics.jexManager.createEntryArray(expNameStr, dateStr, expInfoStr, w, h);
				
				Logs.log("Closing the panel", 1, this);
				this.setVisible(false);
				this.dispose();
			}
		}
	}
	
	class DataSetEditor extends JDialog implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		JTextArea viewedExpName = new JTextArea("New experiment");
		JTextArea viewedExpInfo = new JTextArea("No Info", 5, 50);
		JTextArea viewedExpDate = new JTextArea(DateUtility.getDate());
		JTextArea viewedTrayWidth = new JTextArea("1");
		JTextArea viewedTrayHeight = new JTextArea("1");
		private JButton editButton;
		private JButton deleteButton;
		private JPanel editPanel;
		
		public DataSetEditor()
		{
			this.initialize();
		}
		
		private void initialize()
		{
			this.setLayout(new BorderLayout());
			this.setBackground(DisplayStatics.background);
			
			// Initialize buttons
			this.editButton = new JButton("Apply Changes");
			this.editButton.addActionListener(this);
			
			this.deleteButton = new JButton("Delete Dataset");
			this.deleteButton.addActionListener(this);
			
			this.editPanel = this.makeEditObjectPanel();
			this.editPanel.setBackground(DisplayStatics.lightBackground);
			
			JPanel p = (JPanel) this.getContentPane();
			p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			p.setBackground(DisplayStatics.background);
			
			this.add(this.editPanel, BorderLayout.CENTER);
			this.repaint();
		}
		
		public JPanel makeEditObjectPanel()
		{
			JPanel result = new JPanel();
			
			// make the labels
			JLabel viewedExpNameLabel = new JLabel("Dataset Name");
			JLabel viewedExpInfoLabel = new JLabel("Notes");
			JLabel viewedExpDateLabel = new JLabel("Date");
			
			// Make the string to preset in the fields
			String expNameStr = (JEXCreationPanel.this.hlevel != null) ? JEXCreationPanel.this.hlevel.getRepresentativeEntry().getEntryExperiment() : "New Experiment";
			String expInfoStr = (JEXCreationPanel.this.hlevel != null) ? JEXCreationPanel.this.hlevel.getRepresentativeEntry().getEntryExperimentInfo() : "No Notes";
			String expDateStr = (JEXCreationPanel.this.hlevel != null) ? JEXCreationPanel.this.hlevel.getRepresentativeEntry().getDate() : DateUtility.getDate();
			
			// Store initial exp name
			JEXCreationPanel.this.initialExpName = expNameStr;
			JEXCreationPanel.this.initialExpInfo = expInfoStr;
			JEXCreationPanel.this.initialExpDate = expDateStr;
			
			// Make the text fields
			this.viewedExpName = new JTextArea(expNameStr);
			this.viewedExpName.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.viewedExpInfo = new JTextArea(expInfoStr, 5, 50);
			this.viewedExpInfo.setLineWrap(true);
			this.viewedExpInfo.setWrapStyleWord(true);
			JScrollPane infoPane = new JScrollPane(this.viewedExpInfo);
			infoPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			infoPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			infoPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.viewedExpDate = new JTextArea(expDateStr);
			this.viewedExpDate.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			
			// Set the layout
			result.setLayout(new MigLayout("ins 3", "[0:0,75,left]2[0:0,fill,grow]", "[]2[grow,top]2[]2[]2[]"));
			result.add(viewedExpNameLabel);
			result.add(this.viewedExpName, "growx, wrap");
			result.add(viewedExpInfoLabel);
			result.add(infoPane, "grow, wrap, width 0:0:");
			result.add(viewedExpDateLabel);
			result.add(this.viewedExpDate, "growx, wrap");
			result.add(this.deleteButton, "growx, span 2, wrap");
			result.add(this.editButton, "growx, span 2");
			
			return result;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			if(arg0.getSource() == this.editButton)
			{
				Logs.log("Editing data sets", 1, this);
				
				// Get the new values requested
				String expNameStr = FileUtility.removeWhiteSpaceOnEnds(this.viewedExpName.getText());
				String expInfoStr = FileUtility.removeWhiteSpaceOnEnds(this.viewedExpInfo.getText());
				String expDateStr = FileUtility.removeWhiteSpaceOnEnds(this.viewedExpDate.getText());
				
				// Get the entries
				if(JEXCreationPanel.this.hlevel == null)
				{
					return;
				}
				TreeSet<JEXEntry> entries = JEXCreationPanel.this.hlevel.getEntries();
				
				if(JEXCreationPanel.this.initialExpName.equals(expNameStr) && JEXCreationPanel.this.initialExpInfo.equals(expInfoStr) && JEXCreationPanel.this.initialExpDate.equals(expDateStr))
				{
					JEXStatics.statusBar.setStatusText("Entered info is the same as before. No changes were made.");
					Logs.log("Entered info is the same as before. No changes were made.", 0, this);
					return;
				}
				boolean nameConflict = JEXStatics.jexManager.getCurrentDatabase().getExperimentalTable().containsKey(expNameStr);
				if(nameConflict && !JEXCreationPanel.this.initialExpName.equals(expNameStr))
				{
					JEXStatics.statusBar.setStatusText("Naming conflict with other experiment.");
					Logs.log("Naming conflict with other experiment.", 0, this);
					return;
				}
				
				// Do the changes
				JEXStatics.jexDBManager.editHeirarchyForEntries(entries, expNameStr, expInfoStr, expDateStr);
				
				Logs.log("Closing the panel", 1, this);
				this.setVisible(false);
				this.dispose();
			}
			else if(arg0.getSource() == this.deleteButton)
			{
				// Get the entries
				if(JEXCreationPanel.this.hlevel == null)
				{
					return;
				}
				TreeSet<JEXEntry> entries = JEXCreationPanel.this.hlevel.getEntries();
				
				// Remove them
				Logs.log("Request to delete entries", 1, this);
				JEXStatics.jexDBManager.removeEntries(entries);
				
				// Hide the panel
				Logs.log("Closing the panel", 1, this);
				this.setVisible(false);
				this.dispose();
			}
		}
	}
}
package jex;

import Database.SingleUserDatabase.JEXDBInfo;
import Database.SingleUserDatabase.Repository;
import guiObject.JRoundedPanel;
import guiObject.SignalFlatRoundedButtonDeletable;
import guiObject.SignalMenuButton;
import icons.IconRepository;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import jex.statics.PrefsUtility;
import logs.Logs;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class JEXAvailableDatabases extends JPanel implements ActionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	protected JEXDatabaseChooser parent;
	protected TreeMap<Repository,JEXDBInfo[]> availableDatabases;
	
	// Hidden variables
	protected JPanel centerPane = new JPanel();
	protected JScrollPane centerScrollPane = new JScrollPane(centerPane);
	protected SignalFlatRoundedButtonDeletable newRepositoryButton;
	
	public JEXAvailableDatabases(JEXDatabaseChooser parent)
	{
		super();
		this.parent = parent;
		
		// Link to the signal from a database list change
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.DATABASES, this, "availableDatabaseChange", (Class[]) null);
		Logs.log("Connected to database change signal", 0, this);
		
		// initialize gui
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		this.setBackground(DisplayStatics.background);
		this.setLayout(new BorderLayout());
		centerScrollPane.setBackground(DisplayStatics.background);
		centerScrollPane.setBorder(BorderFactory.createEmptyBorder());
		centerScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		centerPane.setBackground(DisplayStatics.background);
		centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.PAGE_AXIS));
		
		// Create a new repository button
		newRepositoryButton = new SignalFlatRoundedButtonDeletable("Add Repository");
		newRepositoryButton.setMaximumSize(new Dimension(400, 40));
		newRepositoryButton.setMinimumSize(new Dimension(200, 40));
		newRepositoryButton.setPreferredSize(new Dimension(300, 40));
		newRepositoryButton.background = DisplayStatics.background;
		newRepositoryButton.normalBack = DisplayStatics.lightBackground;
		newRepositoryButton.mouseOverBack = DisplayStatics.lightMouseOverBackground;
		newRepositoryButton.selectedBack = DisplayStatics.menuBackground;
		newRepositoryButton.setLabelFont(FontUtility.boldFontl);
		newRepositoryButton.addMouseListener(this);
		
		this.add(centerScrollPane, BorderLayout.CENTER);
		this.add(newRepositoryButton, BorderLayout.PAGE_END);
		this.rebuild();
	}
	
	/**
	 * Called when a signal signifying a change in the available databases is made
	 * 
	 */
	public void availableDatabaseChange()
	{
		Logs.log("Database list change signal caught", 1, this);
		this.availableDatabases = JEXStatics.jexManager.getAvailableDatabases();
		rebuild();
	}
	
	/**
	 * Rebuid the repository list and database list
	 */
	private void rebuild()
	{
		Logs.log("Rebuilding database panel", 1, this);
		centerPane.removeAll();
		
		if(availableDatabases == null || availableDatabases.size() == 0)
		{
			JLabel label1 = new JLabel("No Repositories");
			label1.setFont(FontUtility.boldFontl);
			JLabel label2 = new JLabel("Add one below");
			label2.setFont(FontUtility.defaultFont);
			JPanel pane = new JPanel();
			pane.setBackground(DisplayStatics.background);
			pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
			pane.add(Box.createVerticalGlue());
			pane.add(label1);
			pane.add(Box.createVerticalStrut(5));
			pane.add(label2);
			pane.add(Box.createVerticalGlue());
			
			centerPane.add(Box.createVerticalGlue());
			centerPane.add(pane);
		}
		else
		{
			for (Repository rep : availableDatabases.keySet())
			{
				RepositoryPanel repPane = new RepositoryPanel(rep);
				centerPane.add(repPane);
				centerPane.add(Box.createVerticalStrut(10));
			}
		}
		
		centerPane.add(Box.createVerticalGlue());
		centerPane.invalidate();
		centerPane.validate();
		centerPane.repaint();
	}
	
	public void reAlign()
	{
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	/**
	 * Open a dialog to create a new repository
	 */
	public void openNewRepositoryPanel()
	{
		Logs.log("Repository creation selected", 1, this);
		
		// Creating file chooser to open user preferences
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		
		// Set the current directory
		String lastPath = PrefsUtility.getLastPath();
		File filepath = new File(lastPath);
		if(filepath.isDirectory())
			fc.setCurrentDirectory(filepath);
		else
		{
			File filefolder = filepath.getParentFile();
			fc.setCurrentDirectory(filefolder);
		}
		
		// Open dialog box
		int returnVal = fc.showOpenDialog(this);
		
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			// Get the file
			File file = fc.getSelectedFile();
			if(!file.isDirectory())
			{
				file = file.getParentFile();
			}
			Logs.log("Creating user file in folder " + file.getPath(), 0, this);
			
			// Set the last path opened to the path selected
			PrefsUtility.setLastPath(file.getPath());
			
			// Set the path field
			String path = file.getPath();
			String user = JEXStatics.jexManager.getUserName();
			String pswd = "";
			boolean done = JEXStatics.jexManager.createRepository(path, user, pswd);
			Logs.log("Repository creation returned " + done, 1, this);
		}
	}
	
	/**
	 * Open a panel to create a new database
	 */
	public void openNewDatabasePanel(Repository rep)
	{
		Logs.log("Creating new database", 0, this);
		
		// Check to see if this repository is even reachable
		if(!rep.exists())
		{
			JOptionPane.showMessageDialog(JEXStatics.main, "Repository doesn't appear to be connected to this computer.\n\nConnect and restart JEX or try a different repository.");
			return;
		}
		
		DatabaseCreationPane dbCreationPane = new DatabaseCreationPane(parent, rep);
		
		parent.setAlternatePanel(dbCreationPane);
	}
	
	/**
	 * Open the preferences panel of a database
	 * 
	 * @param db
	 */
	public void openEditDatabasePanel(JEXDBInfo db, Repository rep)
	{
		Logs.log("Editing database " + db.getDirectory(), 0, this);
		
		DatabaseEditingPane dbEditingPane = new DatabaseEditingPane(parent, db);
		
		parent.setAlternatePanel(dbEditingPane);
	}
	
	// /**
	// * Database clicked once
	// * @param dbItem
	// */
	// public void databaseClicked(JEXDatabaseInfo dbItem){
	// Logs.log("Selecting database "+dbItem.getDirectory(), 0,
	// this);
	// JEXStatics.jexManager.setClickedDatabase(dbItem);
	// }
	
	/**
	 * Open the clicked database
	 * 
	 * @param dbItem
	 */
	public void openDatabase(JEXDBInfo dbItem)
	{
		parent.openDatabase(dbItem);
		
		// Logs.log("Opening database "+dbItem.getDirectory(),
		// 0, this);
		//
		// boolean isdirty = JEXStatics.jexManager.isCurrentDatabaseModified();
		// if (isdirty) {
		// DialogGlassPane diagPanel = new DialogGlassPane("Warning");
		// diagPanel.setSize(400, 200);
		//
		// OpenAnywaysPane yesno = new OpenAnywaysPane(dbItem);
		// diagPanel.setCentralPanel(yesno);
		//
		// JEXStatics.main.displayGlassPane(diagPanel,true);
		// }
		// else {
		// boolean isUpToDate = JEXReader.isUpToDate(dbItem);
		//
		// if (!isUpToDate)
		// {
		// Logs.log("Database needs updating", 0, this);
		// int result = JEXReader.update(dbItem);
		// Logs.log("Database updating returned "+result, 0,
		// this);
		// }
		// else
		// {
		// Logs.log("Database checked and is ready to open", 0,
		// this);
		// JEXStatics.jexManager.setDatabaseInfo(dbItem);
		// }
		// }
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	public void actionPerformed(ActionEvent e)
	{}
	
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
		if(e.getSource() == this.newRepositoryButton)
		{
			openNewRepositoryPanel();
		}
	}
	
	class RepositoryPanel extends JPanel implements ActionListener, MouseListener {
		
		private static final long serialVersionUID = 1L;
		
		// Variables
		Repository rep;
		JEXDBInfo[] databases;
		boolean isExpanded = true;
		
		// gui
		JRoundedPanel headerPanel;
		JPanel centerPanel;
		
		SignalMenuButton expand = new SignalMenuButton();
		SignalMenuButton reduce = new SignalMenuButton();
		SignalMenuButton newDB = new SignalMenuButton();
		SignalMenuButton removeRep = new SignalMenuButton();
		
		public RepositoryPanel(Repository rep)
		{
			this.rep = rep;
			this.databases = availableDatabases.get(rep);
			
			initialize();
			rebuild();
		}
		
		/**
		 * Initialize
		 */
		private void initialize()
		{
			// make the expand reduce buttons
			Image iconexpand = JEXStatics.iconRepository.getImageWithName(IconRepository.ARROW_FORWARD, 20, 20);
			expand.setText(null);
			expand.setImage(iconexpand);
			expand.setBackgroundColor(DisplayStatics.lightBackground);
			expand.setToolTipText("Expand repository panel");
			expand.addActionListener(this);
			Image iconreduce = JEXStatics.iconRepository.getImageWithName(IconRepository.ARROW_DOWN, 20, 20);
			reduce.setText(null);
			reduce.setImage(iconreduce);
			reduce.setBackgroundColor(DisplayStatics.lightBackground);
			reduce.setToolTipText("Reduce repository panel");
			reduce.addActionListener(this);
			Image iconNewDB = JEXStatics.iconRepository.getImageWithName(IconRepository.SESSION_DATABASE_NEW, 20, 20);
			newDB.setText(null);
			newDB.setImage(iconNewDB);
			newDB.setBackgroundColor(DisplayStatics.lightBackground);
			newDB.setToolTipText("Create new Database in this repository");
			newDB.addActionListener(this);
			Image iconremoveRep = JEXStatics.iconRepository.getImageWithName(IconRepository.SESSION_REPOSITORY_DELETE, 20, 20);
			removeRep.setText(null);
			removeRep.setImage(iconremoveRep);
			removeRep.setBackgroundColor(DisplayStatics.lightBackground);
			removeRep.setToolTipText("Remove this repository from the list");
			removeRep.addActionListener(this);
			
			// make the header panel
			JLabel repName = new JLabel(rep.getPath());
			repName.setFont(FontUtility.boldFontl);
			repName.setBackground(DisplayStatics.lightBackground);
			JPanel headerpane = new JPanel();
			headerpane.setLayout(new BoxLayout(headerpane, BoxLayout.LINE_AXIS));
			headerpane.setBackground(DisplayStatics.lightBackground);
			headerpane.add(reduce);
			headerpane.add(Box.createHorizontalStrut(10));
			headerpane.add(repName);
			headerpane.add(Box.createHorizontalGlue());
			headerpane.add(newDB);
			headerpane.add(Box.createHorizontalStrut(5));
			headerpane.add(removeRep);
			
			headerPanel = new JRoundedPanel();
			headerPanel.setInnerBackgroundColor(DisplayStatics.lightBackground);
			headerPanel.setOutterBackgroundColor(DisplayStatics.background);
			headerPanel.setCenterPanel(headerpane);
			
			// make the center panel
			centerPanel = new JPanel();
			centerPanel.setBackground(DisplayStatics.background);
			centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
			JPanel contentPanel = new JPanel();
			contentPanel.setBackground(DisplayStatics.background);
			contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.LINE_AXIS));
			contentPanel.add(Box.createHorizontalStrut(20));
			contentPanel.add(centerPanel);
			contentPanel.add(Box.createHorizontalStrut(20));
			
			this.setLayout(new BorderLayout());
			this.setBackground(DisplayStatics.background);
			this.add(headerPanel, BorderLayout.PAGE_START);
			this.add(contentPanel, BorderLayout.CENTER);
		}
		
		/**
		 * Provoke reduction of repository panel
		 */
		public void reduce()
		{
			Logs.log("Reducing repository panel", 1, this);
			isExpanded = false;
			rebuild();
		}
		
		/**
		 * Provoke expansion of repository panel
		 */
		public void expand()
		{
			Logs.log("Expanding repository panel", 1, this);
			isExpanded = true;
			rebuild();
		}
		
		/**
		 * Rebuild database list panel
		 */
		private void rebuild()
		{
			Logs.log("Rebuilding repository panel", 1, this);
			
			// make the header panel
			JLabel repName = new JLabel(rep.getPath());
			repName.setFont(FontUtility.boldFontl);
			repName.setBackground(DisplayStatics.lightBackground);
			JPanel headerpane = new JPanel();
			headerpane.setLayout(new BoxLayout(headerpane, BoxLayout.LINE_AXIS));
			headerpane.setBackground(DisplayStatics.lightBackground);
			if(isExpanded)
				headerpane.add(reduce);
			else
				headerpane.add(expand);
			headerpane.add(Box.createHorizontalStrut(10));
			headerpane.add(repName);
			headerpane.add(Box.createHorizontalGlue());
			headerpane.add(newDB);
			headerpane.add(Box.createHorizontalStrut(5));
			headerpane.add(removeRep);
			
			headerPanel = new JRoundedPanel();
			headerPanel.setInnerBackgroundColor(DisplayStatics.lightBackground);
			headerPanel.setOutterBackgroundColor(DisplayStatics.background);
			headerPanel.setCenterPanel(headerpane);
			headerPanel.setMaximumSize(new Dimension(2000, 30));
			headerPanel.setMinimumSize(new Dimension(300, 30));
			headerPanel.setPreferredSize(new Dimension(300, 30));
			
			// make the center panel
			centerPanel = new JPanel();
			centerPanel.setBackground(DisplayStatics.background);
			centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
			centerPanel.add(Box.createVerticalStrut(10));
			
			if(isExpanded)
			{
				if(databases == null)
				{}
				else
				{
					for (JEXDBInfo db : databases)
					{
						DatabasePanel dbPane = new DatabasePanel(db, rep);
						centerPanel.add(dbPane);
						centerPanel.add(Box.createVerticalStrut(5));
					}
				}
				
				this.removeAll();
				this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
				this.add(headerPanel);
				this.add(centerPanel);
				this.invalidate();
				this.validate();
				this.repaint();
			}
			else
			{
				this.removeAll();
				this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
				this.add(headerPanel);
				this.invalidate();
				this.validate();
				this.repaint();
			}
			
			reAlign();
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
		{}
		
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == newDB)
			{
				Logs.log("Triggering new database creation", 1, this);
				openNewDatabasePanel(rep);
			}
			if(e.getSource() == removeRep)
			{
				Logs.log("Remove this repertory?", 1, this);
				JEXStatics.jexManager.removeRepository(rep);
			}
			if(e.getSource() == expand)
			{
				this.expand();
			}
			if(e.getSource() == reduce)
			{
				this.reduce();
			}
		}
		
	}
	
	class DatabasePanel extends JPanel implements ActionListener, MouseListener {
		
		private static final long serialVersionUID = 1L;
		// General variables
		JEXDBInfo database = null;
		Repository rep = null;
		String name = "";
		String info = "";
		boolean upToDate = true;
		
		// More options
		private JLabel nameLabel = new JLabel();
		private JLabel infoLabel = new JLabel();
		private SignalMenuButton dbButton = new SignalMenuButton();
		private SignalMenuButton editDB = new SignalMenuButton();
		private JPanel infoPane = new JPanel();
		
		/**
		 * Create a new GroupPanel regrouping all groupobjects with name title
		 */
		DatabasePanel(JEXDBInfo database, Repository rep)
		{
			this.database = database;
			this.rep = rep;
			this.name = (database == null || database.getDBName() == null) ? "Database is erronerous" : database.getDBName();
			this.info = (database == null || database.get(JEXDBInfo.DB_INFO) == null) ? "Database is erronerous" : database.get(JEXDBInfo.DB_INFO);
			
			initialize();
		}
		
		/**
		 * Set up the background and visual aspect of the panel
		 */
		private void initialize()
		{
			Logs.log("Initializing database " + name + " with info " + info, 1, this);
			this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			this.setBackground(DisplayStatics.lightBackground);
			this.addMouseListener(this);
			
			// Create the Database button
			Image dbImage = JEXStatics.iconRepository.getImageWithName(IconRepository.SESSION_DATABASE, 25, 25);
			dbButton.setImage(dbImage);
			dbButton.setMinimumSize(new Dimension(40, 35));
			dbButton.setMaximumSize(new Dimension(40, 35));
			dbButton.setPreferredSize(new Dimension(40, 35));
			dbButton.setBackgroundColor(DisplayStatics.lightBackground);
			dbButton.addActionListener(this);
			dbButton.addMouseListener(this);
			
			// Create the Information pane
			nameLabel = new JLabel(name);
			nameLabel.setFont(new Font("Serif", Font.BOLD, 14));
			nameLabel.setBackground(DisplayStatics.lightBackground);
			nameLabel.removeMouseListener(this);
			nameLabel.addMouseListener(this);
			
			infoLabel = new JLabel(info);
			infoLabel.setBackground(DisplayStatics.lightBackground);
			infoLabel.setForeground(Color.gray);
			infoLabel.removeMouseListener(this);
			infoLabel.addMouseListener(this);
			
			infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.PAGE_AXIS));
			infoPane.setBackground(DisplayStatics.lightBackground);
			infoPane.add(nameLabel);
			infoPane.add(infoLabel);
			infoPane.add(Box.createVerticalGlue());
			
			// make the preferences button
			Image iconeditDB = JEXStatics.iconRepository.getImageWithName(IconRepository.MISC_PREFERENCES, 20, 20);
			editDB.setText(null);
			editDB.setImage(iconeditDB);
			editDB.setBackgroundColor(DisplayStatics.lightBackground);
			editDB.addActionListener(this);
			
			refresh();
			
			this.add(Box.createHorizontalStrut(28));
			this.add(dbButton);
			this.add(Box.createHorizontalStrut(5));
			this.add(infoPane);
			this.add(Box.createHorizontalGlue());
			this.add(editDB);
			this.add(Box.createHorizontalStrut(28));
		}
		
		/**
		 * Refresh
		 */
		public void refresh()
		{
			infoPane.removeAll();
			infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.PAGE_AXIS));
			
			infoPane.add(Box.createVerticalStrut(3));
			infoPane.add(nameLabel);
			infoPane.add(infoLabel);
			infoPane.add(Box.createVerticalStrut(3));
			
			this.setMaximumSize(new Dimension(1000, 40));
			this.setMinimumSize(new Dimension(200, 40));
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
			// if (e.getClickCount() ==2) {
			// Logs.log("Double click on database", 0, this);
			// openDatabase(database);
			// }
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == editDB)
			{
				Logs.log("Edit this database", 1, this);
				openEditDatabasePanel(database, rep);
			}
			if(e.getSource() == dbButton)
			{
				Logs.log("Double click on database", 0, this);
				openDatabase(database);
			}
		}
		
		/**
		 * Paint this componement with cool colors
		 */
		@Override
		protected void paintComponent(Graphics g)
		{
			int w = getWidth();
			int h = getHeight();
			
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2.setColor(DisplayStatics.background);
			g2.fillRect(0, 0, getWidth(), getHeight());
			
			Color centerColor = DisplayStatics.lightBackground;
			if(!upToDate)
				centerColor = new Color(216, 177, 195);
			
			g2.setColor(centerColor);
			g2.fillRoundRect(20, 0, w - 45, h, 10, 10);
			
			g2.dispose();
		}
		
		@Override
		public void paint(Graphics g)
		{
			Color centerColor = DisplayStatics.lightBackground;
			if(!upToDate)
				centerColor = new Color(216, 177, 195);
			
			nameLabel.setBackground(centerColor);
			infoLabel.setBackground(centerColor);
			infoPane.setBackground(centerColor);
			dbButton.setBackgroundColor(centerColor);
			editDB.setBackgroundColor(centerColor);
			
			super.paint(g);
		}
	}
}

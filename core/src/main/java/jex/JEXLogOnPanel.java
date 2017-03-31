package jex;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import jex.statics.PrefsUtility;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;

public class JEXLogOnPanel extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// GUI variables
	private CreateUserPanel createUserPane;
	private OpenUserPanel openUserPane;
	private RecentlyOpenPanel openrecentPane;
	
	public JEXLogOnPanel()
	{
		initialize();
	}
	
	private void initialize()
	{
		Container c = this.getContentPane();
		c.setBackground(DisplayStatics.background);
		c.setLayout(new MigLayout("flowx,ins 0", "5[fill,grow]5[fill,grow]5[fill,grow]5", "5[fill,grow]5"));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		createUserPanel();
		openUserPanel();
		openRecentPanel();
		
		c.add(createUserPane, "width 33%");
		c.add(openUserPane, "width 33%");
		c.add(openrecentPane, "width 34%");
	}
	
	private void createUserPanel()
	{
		createUserPane = new CreateUserPanel();
	}
	
	private void openUserPanel()
	{
		openUserPane = new OpenUserPanel();
	}
	
	private void openRecentPanel()
	{
		openrecentPane = new RecentlyOpenPanel();
	}
	
	public void openUser(File file)
	{
		// open the user file
		Logs.log("Opening userfile " + file, 1, this);
		boolean done = JEXStatics.jexManager.logOn(file);
		
		if(!done)
		{
			// Do nothing... there was an error
			return;
		}
		else
		{
			// Switch the display view
			JEXStatics.main.showLogOnFrame(false);
			JEXStatics.main.showDatabaseChooserFrame(true);
		}
	}
	
	public void actionPerformed(ActionEvent arg0)
	{}
	
	// Create the recently opened panel
	class RecentlyOpenPanel extends JPanel implements MouseListener {
		
		private static final long serialVersionUID = 1L;
		Color backColor = DisplayStatics.lightBackground;
		
		// Two panels
		private JPanel contentPanel;
		
		JLabel[] labels;
		String[] files;
		
		public RecentlyOpenPanel()
		{
			initialize();
			this.addMouseListener(this);
		}
		
		private void initialize()
		{
			// Make the content panel
			makeContentPanel();
			
			// Make this panel
			this.setBackground(this.backColor);
			this.setLayout(new BorderLayout());
			this.add(contentPanel);
		}
		
		private void makeContentPanel()
		{
			contentPanel = new JPanel();
			contentPanel.setBackground(this.backColor);
			contentPanel.setLayout(new MigLayout("flowy,ins 0", "10[fill,grow]", "[fill,grow]5[15][5][5][5][5][5]5[fill,grow]"));
			
			// Set the welcome label
			// JLabel title1 = new JLabel("Option 3.");
			JLabel title2 = new JLabel("Select Recently Opened User");
			// title1.setFont(FontUtility.boldFont);
			title2.setFont(FontUtility.boldFont);
			// title1.setAlignmentX(Component.LEFT_ALIGNMENT);
			title2.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			// Display the welcome label
			contentPanel.add(Box.createRigidArea(new Dimension(10, 10)), "");
			contentPanel.add(title2, "height 15");
			
			// Display the recentrly opened files
			files = JEXStatics.jexManager.getRecentelyOpenedUserFiles();
			labels = new JLabel[files.length];
			for (int i = 0; i < files.length; i++)
			{
				String userFile = files[i];
				File f = new File(userFile);
				String fName = f.getName();
				
				JLabel userLabel = new JLabel(fName);
				userLabel.setFont(FontUtility.defaultFontl);
				userLabel.addMouseListener(this);
				labels[i] = userLabel;
				
				userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				
				contentPanel.add(userLabel, "height 15");
			}
		}
		
		public void mouseClicked(MouseEvent arg0)
		{}
		
		public void mouseEntered(MouseEvent arg0)
		{}
		
		public void mouseExited(MouseEvent arg0)
		{}
		
		public void mousePressed(MouseEvent arg0)
		{}
		
		public void mouseReleased(MouseEvent arg0)
		{
			Logs.log("Clicked to open user file", 1, this);
			for (int i = 0; i < files.length; i++)
			{
				JLabel label = labels[i];
				if(arg0.getSource() == label)
				{
					// Get the file
					String file = files[i];
					File user = new File(file);
					openUser(user);
				}
			}
		}
	}
	
	// Create the user creation panel
	class CreateUserPanel extends JPanel implements MouseListener {
		
		private static final long serialVersionUID = 1L;
		Color backColor = DisplayStatics.lightBackground;
		
		CreateUserPanel()
		{
			initialize();
			this.addMouseListener(this);
		}
		
		private void initialize()
		{
			// Create inner objects
			// JLabel title1 = new JLabel("Option 1.");
			JLabel title2 = new JLabel("Create a new user profile");
			// title1.setFont(FontUtility.boldFont);
			title2.setFont(FontUtility.boldFont);
			// title1.setAlignmentX(Component.CENTER_ALIGNMENT);
			title2.setAlignmentX(Component.CENTER_ALIGNMENT);
			
			// Place objects
			this.setBackground(this.backColor);
			this.setLayout(new MigLayout("flowy,ins 0", "10[fill,grow]", "[grow,fill]3[grow,fill]3[grow,fill]"));
			this.add(Box.createRigidArea(new Dimension(10, 10)));
			// this.add(title1,"");
			this.add(title2, "");
			this.add(Box.createRigidArea(new Dimension(10, 10)));
		}
		
		public void mouseClicked(MouseEvent arg0)
		{}
		
		public void mouseEntered(MouseEvent arg0)
		{}
		
		public void mouseExited(MouseEvent arg0)
		{}
		
		public void mousePressed(MouseEvent arg0)
		{}
		
		public void mouseReleased(MouseEvent arg0)
		{
			// Creating file chooser to open user preferences
			JFileChooser fc = new JFileChooser();
			// fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			
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
			int returnVal = fc.showSaveDialog(this);
			
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				// Get the file
				File file = fc.getSelectedFile();
				if(!file.isDirectory())
				{
					file = file.getParentFile();
				}
				Logs.log("Creating user file in folder " + file.getPath(), 0, this);
				Logs.log("Creating new user: " + fc.getSelectedFile().getName(), 0, this);
				
				// Set the last path opened to the path selected
				PrefsUtility.setLastPath(file.getPath());
				
				// Make the user file
				String path = fc.getSelectedFile().getPath();
				String extension = path.substring(path.length() - 4);
				if(!extension.equals(".jex"))
					path = path + ".jex";
				File userfile = new File(path);
				
				// Create the user
				JEXStatics.jexManager.createUser(userfile);
				openUser(userfile);
			}
		}
	}
	
	// Create the user creation panel
	class OpenUserPanel extends JPanel implements MouseListener {
		
		private static final long serialVersionUID = 1L;
		Color backColor = DisplayStatics.lightBackground;
		
		OpenUserPanel()
		{
			initialize();
			this.addMouseListener(this);
		}
		
		private void initialize()
		{
			// Create inner objects
			// JLabel title1 = new JLabel("Option 2.");
			JLabel title2 = new JLabel("Open a user profile");
			// title1.setFont(FontUtility.boldFont);
			title2.setFont(FontUtility.boldFont);
			// title1.setAlignmentX(Component.LEFT_ALIGNMENT);
			title2.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			// Place objects
			this.setBackground(this.backColor);
			this.setLayout(new MigLayout("flowy,ins 0", "10[fill,grow]", "[grow,fill]3[grow,fill]3[grow,fill]"));
			this.add(Box.createRigidArea(new Dimension(10, 10)));
			// this.add(title1,"");
			this.add(title2, "");
			this.add(Box.createRigidArea(new Dimension(10, 10)));
		}
		
		public void mouseClicked(MouseEvent arg0)
		{}
		
		public void mouseEntered(MouseEvent arg0)
		{}
		
		public void mouseExited(MouseEvent arg0)
		{}
		
		public void mousePressed(MouseEvent arg0)
		{}
		
		public void mouseReleased(MouseEvent arg0)
		{
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
				Logs.log("Opening user file in folder " + file.getPath(), 0, this);
				
				// Set the last path opened to the path selected
				PrefsUtility.setLastPath(file.getPath());
				
				// Create the user
				openUser(file);
			}
		}
	}
	
}

package jex;

import Database.SingleUserDatabase.JEXDBInfo;
import guiObject.DialogGlassPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;

public class JEXDatabaseChooser extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	public JEXAvailableDatabases databasePane;
	private JPanel alternatePanel;
	
	public JEXDatabaseChooser()
	{
		initialize();
	}
	
	private void initialize()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(DisplayStatics.background);
		
		databasePane = new JEXAvailableDatabases(this);
		
		JPanel p = (JPanel) this.getContentPane();
		p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		p.setBackground(DisplayStatics.background);
		
		this.add(databasePane, BorderLayout.CENTER);
		this.repaint();
	}
	
	public void rebuild()
	{
		Container c = this.getContentPane();
		c.removeAll();
		
		if(alternatePanel != null)
		{
			c.add(alternatePanel, BorderLayout.CENTER);
		}
		else
		{
			JLabel title = new JLabel("Choose a database in a repository");
			title.setFont(FontUtility.boldFont);
			title.setAlignmentX(Component.CENTER_ALIGNMENT);
			title.setPreferredSize(new Dimension(600, 30));
			title.setMinimumSize(new Dimension(200, 30));
			title.setForeground(Color.white);
			
			c.add(title, BorderLayout.PAGE_START);
			c.add(databasePane, BorderLayout.CENTER);
			databasePane.availableDatabaseChange();
		}
		
		c.invalidate();
		c.validate();
		c.repaint();
		this.repaint();
	}
	
	public void setAlternatePanel(JPanel alternatePanel)
	{
		this.alternatePanel = alternatePanel;
		rebuild();
	}
	
	public void openDatabase(JEXDBInfo dbItem)
	{
		Logs.log("Opening database " + dbItem.getDirectory(), 0, this);
		JEXStatics.main.setTitle("Viewing database: " + dbItem.getName());
		JEXStatics.main.showLogOnFrame(false);
		JEXStatics.main.showDatabaseChooserFrame(false);
		
		boolean isdirty = JEXStatics.jexManager.isCurrentDatabaseModified();
		if(isdirty)
		{
			DialogGlassPane diagPanel = new DialogGlassPane("Warning");
			diagPanel.setSize(400, 200);
			
			OpenAnywaysPane yesno = new OpenAnywaysPane(dbItem);
			diagPanel.setCentralPanel(yesno);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
		}
		else
		{
			Logs.log("Database checked and is ready to open", 0, this);
			JEXStatics.jexManager.setDatabaseInfo(dbItem);
		}
		
		JEXStatics.main.showMainJEXWindow(true);
		JEXStatics.main.displayCreationPane();
	}
	
	public void actionPerformed(ActionEvent arg0)
	{}
	
}

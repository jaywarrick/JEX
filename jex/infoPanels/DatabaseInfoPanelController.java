package jex.infoPanels;

import java.awt.Color;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.JEXStatics;
import miscellaneous.FileUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.SingleUserDatabase.JEXDBInfo;

public class DatabaseInfoPanelController extends InfoPanelController {
	
	// variables
	JEXDBInfo db;
	String repString = "";
	String dbString = "";
	
	public DatabaseInfoPanelController()
	{
		// Get info
		updateInfo();
		
		// Sign up to dbInfo change signals
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.DATASETS, this, "databaseChange");
	}
	
	private void updateInfo()
	{
		// pass variables
		this.db = JEXStatics.jexManager.getDatabaseInfo();
		if(db != null)
		{
			File dbFile = new File(db.getDirectory());
			File repFile = dbFile.getParentFile();
			
			repString = repFile.getAbsolutePath();
			dbString = db.getDirectory();
			String[] splitDbString = FileUtility.splitFilePathOnSeparator(dbString);
			dbString = splitDbString[splitDbString.length - 1];
		}
	}
	
	public void databaseChange()
	{
		// Get info
		updateInfo();
		
		// Set a new InfoPanel
		SSCenter.defaultCenter().emit(this, JEXManager.INFOPANELS_EXP, (Object[]) null);
		SSCenter.defaultCenter().emit(this, JEXManager.INFOPANELS_ARR, (Object[]) null);
	}
	
	public InfoPanel panel()
	{
		return new DatabaseInfoPanel();
	}
	
	class DatabaseInfoPanel extends InfoPanel {
		
		private static final long serialVersionUID = 1L;
		
		public DatabaseInfoPanel()
		{
			// Make the gui
			rebuild();
		}
		
		private void rebuild()
		{
			// make the repository label
			JLabel repLabel1 = new JLabel("Repository:");
			// repLabel1.setBackground(transparent);
			repLabel1.setForeground(Color.white);
			
			JLabel repLabel2 = new JLabel(repString);
			repLabel2.setToolTipText(repString);
			// repLabel2.setBackground(transparent);
			repLabel2.setForeground(Color.white);
			
			// Make the database label
			JLabel dbLabel1 = new JLabel("Database:");
			// dbLabel1.setBackground(transparent);
			dbLabel1.setForeground(Color.white);
			
			JLabel dbLabel2 = new JLabel(dbString);
			dbLabel2.setToolTipText(dbString);
			// dbLabel2.setBackground(transparent);
			dbLabel2.setForeground(Color.white);
			
			// make the center panel
			JPanel centerPane = new JPanel();
			centerPane.setLayout(new MigLayout("flowy, ins 0", "[]3[grow,fill]", "[]3[]3[]3[]3[]"));
			centerPane.setBackground(InfoPanel.centerPaneBGColor);
			
			centerPane.add(repLabel1, "height 15");
			centerPane.add(repLabel2, "height 15,wmin 50,growx");
			centerPane.add(dbLabel1, "height 15");
			centerPane.add(dbLabel2, "height 15,wmin 50,growx");
			
			// set the info panel
			this.setTitle("Database and repository");
			this.setCenterPanel(centerPane);
		}
	}
}

package jex.objectAndEntryPanels;

import guiObject.TypeNameButton;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.jexTabPanel.jexFunctionPanel.JEXFunctionPanel;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import net.miginfocom.swing.MigLayout;
import plugins.valueTable.ValueBrowserController;
import plugins.viewer.ImageBrowser;
import signals.SSCenter;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXWorkflow;
import Database.DataReader.FileReader;
import Database.DataReader.MovieReader;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;

public class JEXDataPanelLine extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;

	// Input Panel
	private TypeNameButton dataButton;

	// Class Variables
	private String objectName;
	private TypeName objectTN;

	private TypeName objectTypeName;
	private Color foregroundColor = DisplayStatics.lightBackground;
	private Color selectedbackground = DisplayStatics.menuBackground;

	public JEXDataPanelLine(TypeName objectTN, JEXDataPanel parent)
	{
		this.objectName = objectTN.getName();
		this.objectTN = objectTN;
		this.objectTypeName = objectTN;

		// Connect to the label selection listener
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTEDOBJ, this, "refresh", (Class[]) null);
		// Logs.log("Connected to database object selection signal", 2, this);

		initialize();
		refresh();
	}

	/**
	 * Initialize the panel
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[24]5[fill,grow]5[]", "[0:0,24]"));
		this.setBackground(DisplayStatics.lightBackground);
		this.addMouseListener(this);

		// viewButton = new FlatRoundedButton( "View" );
		// viewButton.enableUnselection(false);
		// viewButton.addActionListener(this);

		dataButton = new TypeNameButton(objectTypeName);
		dataButton.addMouseListener(this);
	}

	/**
	 * Refresh the panel
	 */
	public void refresh()
	{
		this.removeAll();
		// this.setMaximumSize(new Dimension(800,20));

		TypeName selectedTN = JEXStatics.jexManager.getSelectedObject();
		if(selectedTN == null || !selectedTN.equals(objectTN))
		{
			// viewButton.normalBack = foregroundColor;
			// viewButton.setPressed(false);

			this.setBackground(foregroundColor);
		}
		else
		{
			// viewButton.normalBack = DisplayStatics.dividerColor;
			// viewButton.setPressed(true);

			this.setBackground(selectedbackground);
		}
		this.removeAll();
		this.add(dataButton);
		this.add(new JLabel(objectName), "growx, width 25:25:");
		// this.add(viewButton.panel());

		this.revalidate();
		this.repaint();
	}

	/**
	 * Return the typename in this object panel
	 * 
	 * @return typename
	 */
	public TypeName getTypeName()
	{
		return this.objectTypeName;
	}

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
		if(e.getSource() == this)
		{
			TypeName selectedTN = JEXStatics.jexManager.getSelectedObject();
			if(selectedTN == null || !selectedTN.equals(this.objectTN))
			{
				JEXStatics.jexManager.setSelectedObject(objectTypeName);
			}
			else
			{
				JEXStatics.jexManager.setSelectedObject(null);
				Logs.log("Changing sub views to normal ", 1, this);
			}
		}
		else if(e.getSource() == dataButton && e.getClickCount() == 2)
		{
			if(this.objectTN == null)
				return;
			else if(this.objectTN.getType().matches(JEXData.IMAGE))
			{
				TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
				if(entries.size() > 0)
				{
					new ImageBrowser(entries, this.objectTN);
				}
			}
			else if(this.objectTN.getType().matches(JEXData.VALUE))
			{
				TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
				if(entries.size() > 0)
				{
					// new ValueBrowser(entries,this.objectTN);
					new ValueBrowserController(entries, this.objectTN);
				}
			}
			else if(this.objectTN.getType().matches(JEXData.FILE))
			{
				JEXEntry viewedEntry = JEXStatics.jexManager.getViewedEntry();
				if(viewedEntry != null)
				{
					this.openFileInEntry(viewedEntry);
				}
				else
				{
					TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
					if(entries.size() > 0)
					{
						this.openFileInEntry(entries.first());
					}
				}
			}
			else if(this.objectTN.getType().matches(JEXData.MOVIE))
			{
				JEXEntry viewedEntry = JEXStatics.jexManager.getViewedEntry();
				if(viewedEntry != null)
				{
					JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.objectTN, viewedEntry);
					String path = MovieReader.readMovieObject(data);
					try
					{
						FileUtility.openFileDefaultApplication(path);
					}
					catch (Exception e1)
					{
						JEXStatics.statusBar.setStatusText("Error opening file... Path = " + path);
						Logs.log("Error opening file... Path = " + path, 1, this);
					}
				}
				else
				{
					TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
					if(entries.size() > 0)
					{
						JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.objectTN, entries.first());
						String path = MovieReader.readMovieObject(data);
						try
						{
							FileUtility.openFileDefaultApplication(path);
						}
						catch (Exception e1)
						{
							JEXStatics.statusBar.setStatusText("Error opening file... Path = " + path);
							Logs.log("Error opening file... Path = " + path, 1, this);
						}
					}
				}
			}
			else if(this.objectTN.getType().matches(JEXData.WORKFLOW))// Load the
				// workflow
			{
				TreeSet<JEXEntry> entries = JEXStatics.jexManager.getSelectedEntries();
				JEXEntry appropriateEntry = null;
				for (JEXEntry entry : entries)
				{
					if(entry.getData(objectTN) != null)
					{
						appropriateEntry = entry;
						break;
					}
				}
				if(appropriateEntry == null)
				{
					return;
				}
				JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(objectTN, appropriateEntry);
				JEXWorkflow workflow = new JEXWorkflow(data);
				JEXStatics.main.displayFunctionPane();
				((JEXFunctionPanel) JEXStatics.functionPane.getMainPanel()).loadWorkflow(workflow);
			}
			else
				// Just open the xml of the object
			{
				JEXEntry viewedEntry = JEXStatics.jexManager.getViewedEntry();
				if(viewedEntry != null)
				{
					JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(objectTN, viewedEntry);
					String path = JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath();
					try
					{
						FileUtility.openFileDefaultApplication(path);
					}
					catch (Exception e1)
					{
						e1.printStackTrace();
						JEXStatics.statusBar.setStatusText("Error opening file... Path = " + path);
						Logs.log("Error opening file... Path = " + path, 1, this);
					}
					//					ArffViewer viewer = new ArffViewer();
					//					viewer.setFile(JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath());
					//					viewer.show();
				}
			}
		}
		else if(e.getSource() == dataButton)
		{
			return;
		}
	}

	private void openFileInEntry(JEXEntry viewedEntry)
	{
		JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(objectTN, viewedEntry);
		if(data != null)
		{
			TreeMap<DimensionMap,String> paths = FileReader.readObjectToFilePathTable(data);
			String path = paths.firstEntry().getValue();
			try
			{
				//				if(FileUtility.getFileNameExtension(path).equals("arff"))
				//				{
				//					ArffViewer viewer = new ArffViewer();
				//					viewer.setFile(path);
				//					viewer.show();
				//				}
				//				else
				//				{
				FileUtility.openFileDefaultApplication(path);
				//				}
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
				JEXStatics.statusBar.setStatusText("Error opening file... Path = " + path);
				Logs.log("Error opening file... Path = " + path, 1, this);
			}
		}
		else
		{
			JEXStatics.statusBar.setStatusText("Object does not exist in this entry.");
			Logs.log("Object does not exist in this entry.", 1, this);
		}
	}

}
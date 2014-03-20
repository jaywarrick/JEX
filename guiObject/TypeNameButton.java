//
//  ValueObject.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 6/18/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package guiObject;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.SwingConstants;

import jex.statics.JEXStatics;
import jex.statics.OsVersion;
import logs.Logs;
import transferables.TransferableTypeName;
import Database.Definition.TypeName;

public class TypeNameButton extends JButton implements ActionListener, MouseListener, DragGestureListener {
	
	private static final long serialVersionUID = 1L;
	
	// variable parameter
	private TypeName objectTN;
	
	// graphics parameter
	// private ImageIcon icon = null;
	
	public TypeNameButton(TypeName objectTN)
	{
		this.objectTN = objectTN;
		this.initialize();
		this.createDragSource();
	}
	
	/**
	 * Set-up the visual display and content (tool-tips, icon, text) of the button.
	 */
	private void initialize()
	{
		this.removeActionListener(this);
		this.addActionListener(this);
		this.addMouseListener(this);
		// other visual options
		this.setText(null);
		this.setPreferredSize(new Dimension(20, 20));
		this.setMaximumSize(new Dimension(20, 20));
		this.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.setHorizontalTextPosition(SwingConstants.CENTER);
		
		// set the icon
		resetIcon();
		
		// Set the text and the tools tip string
		String toolTip = "This object represents a copy of all objects of this name and type contained in the displayed dataset";
		this.setToolTipText(toolTip);
		
		// Set the popupmenu
		popupmenu = new JPopupMenu();
		ButtonObjectListener listener = new ButtonObjectListener(popupmenu);
		
		JMenuItem menuItem = new JMenuItem("DataBase Entry Info");
		menuItem.addMouseListener(listener);
		popupmenu.add(menuItem);
		
		menuItem = new JMenuItem("Load function in function panel");
		if(!this.isObjectAFunction())
		{
			menuItem.setForeground(Color.LIGHT_GRAY);
		}
		else
		{
			menuItem.addMouseListener(listener);
			menuItem.setForeground(Color.BLACK);
		}
		popupmenu.add(menuItem);
		
		menuItem = new JMenuItem("Open in default program");
		menuItem.addMouseListener(listener);
		popupmenu.add(menuItem);
		
		menuItem = new JMenuItem("Export array");
		menuItem.addMouseListener(listener);
		popupmenu.add(menuItem);
		
		menuItem = new JMenuItem("Remove");
		menuItem.addMouseListener(listener);
		popupmenu.add(menuItem);
		
		// MouseListener popupListener = new PopupListener(popupmenu);
		// TODO
		// this.addMouseListener(popupListener);
	}
	
	// ----------------------------------------------------
	// --------- EXCHANGE INFO FUNCTIONS ------------------
	// ----------------------------------------------------
	/**
	 * Return the type name of the object represented by this button
	 * 
	 * @return TypeName of the object
	 */
	public TypeName getObjectTypeName()
	{
		return this.objectTN;
	}
	
	/**
	 * Open a database info panel on this object
	 */
	public void openInfoPanel()
	{
		Logs.log("Opening info panel on the object " + objectTN.toString(), 1, this);
		// TODO
	}
	
	/**
	 * Open objects in default program
	 */
	public void openInDefaultProgram()
	{
		Logs.log("Opening object " + objectTN.toString() + " in default program", 1, this);
		if(objectTN == null)
			return;
		
		// TreeSet<JEXEntry> entries =
		// JEXStatics.jexManager.getSelectedEntries();
		// List<JEXData> datas = new ArrayList<JEXData>(0);
		// for (JEXEntry entry: entries){
		// JEXData data = entry.getData(objectTN);
		// if (data == null) continue;
		//
		// datas.add(data);
		// if (!data.openMulti()) break;
		// }
		//
		// for (JEXData data: datas){
		// data.openInDefaultProgram();
		// }
	}
	
	/**
	 * Load object in function panel
	 */
	public void loadInFunctionPanel()
	{
		Logs.log("Load object " + objectTN.toString() + " in function panel", 1, this);
		// TODO
	}
	
	/**
	 * Export object in array
	 */
	public void exportArray()
	{
		Logs.log("Export object " + objectTN.toString() + " array", 1, this);
		// TODO
	}
	
	/**
	 * Remove this object in the selected entries
	 */
	public void removeObjects()
	{
		int n = JOptionPane.showConfirmDialog(JEXStatics.main, "Delete selected object in each selected well?", "Sure?", JOptionPane.YES_NO_OPTION);
		if(n != JOptionPane.YES_OPTION)
			return;
		
		// TODO
	}
	
	/**
	 * Return true if the object codes for a function
	 * 
	 * @return true if function
	 */
	public boolean isObjectAFunction()
	{
		if(this.objectTN.getType().equals("Function"))
			return true;
		return false;
	}
	
	/**
	 * Reset the icon to the default
	 */
	private void resetIcon()
	{
		// Set the icon of the button
		// String iconPath = DisplayStatics.getJEXDataIcon(objectTN);
		// ImagePlus iconImage = new ImagePlus(iconPath);
		// ImageProcessor imp = iconImage.getProcessor();
		// imp = imp.resize(20,20);
		//
		ImageIcon icon = JEXStatics.iconRepository.getJEXDataIcon(objectTN, 20, 20);
		
		// iconImage = new ImagePlus("icon", imp);
		icon = new ImageIcon(icon.getImage());
		this.setIcon(icon);
	}
	
	/**
	 * Set up a drag source on this object
	 */
	private void createDragSource()
	{
		// create the drag source for the button
		DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, this);
	}
	
	class ButtonObjectListener extends MouseAdapter {
		
		ButtonObjectListener(JPopupMenu popupMenu)
		{}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			Logs.log("Mouse pressed", 2, this);
		}
		
		@Override
		public void mouseReleased(MouseEvent e)
		{
			Logs.log("Mouse released", 2, this);
			if(e.getSource() instanceof JMenuItem)
				doit(e);
		}
		
		private void doit(MouseEvent e)
		{
			String action = ((JMenuItem) e.getSource()).getText();
			if(action.equals("DataBase Entry Info"))
			{
				openInfoPanel();
			}
			if(action.equals("Open in default program"))
			{
				openInDefaultProgram();
			}
			if(action.equals("Load function in function panel"))
			{
				loadInFunctionPanel();
			}
			if(action.equals("Export array"))
			{
				exportArray();
			}
			if(action.equals("Remove"))
			{
				removeObjects();
			}
		}
	}
	
	// ----------------------------------------------------
	// --------- UTILITY FUNCTIONS ------------------------
	// ----------------------------------------------------
	// find and replace a string
	public String replaceSpaceForMac(String main)
	{
		String spaceStr = " ";
		String backSlashStr = "\\";
		Character space = spaceStr.toCharArray()[0];
		Character backSlash = backSlashStr.toCharArray()[0];
		
		int k = 0;
		char[] mainCharArray = main.toCharArray();
		Vector<Character> resultCharVector = new Vector<Character>(0);
		while (k < main.length())
		{
			if(Character.isWhitespace(mainCharArray[k]))
			{
				resultCharVector.add(backSlash);
				resultCharVector.add(space);
			}
			else
			{
				resultCharVector.add(mainCharArray[k]);
			}
			k++;
		}
		
		char[] newCharArray = new char[resultCharVector.size()];
		for (int i = 0, len = resultCharVector.size(); i < len; i++)
		{
			newCharArray[i] = resultCharVector.get(i);
		}
		String result = new String(newCharArray);
		return result;
	}
	
	public static void openFileDefaultApplication(String name) throws Exception
	{
		if(OsVersion.IS_OSX)
		{
			String[] commands = { "open", name };
			Runtime.getRuntime().exec(commands);
			// Process p = Runtime.getRuntime().exec(commands);
			// p.waitFor();
			System.out.println("Executed ! ");
		}
		else if(OsVersion.IS_WINDOWS)
		{
			String[] commands = { "cmd", name };
			Runtime.getRuntime().exec(commands);
			// Process p = Runtime.getRuntime().exec(commands);
			// p.waitFor();
			System.out.println("Executed ! ");
		}
	}
	
	// ----------------------------------------------------
	// --------- EVENT FUNCTIONS --------------------------
	// ----------------------------------------------------
	public Popup popup;
	public JPanel popPanel;
	public boolean popshow = false;
	public JPopupMenu popupmenu;
	
	public void actionPerformed(ActionEvent e)
	{}
	
	public void dragGestureRecognized(DragGestureEvent event)
	{
		Cursor cursor = null;
		// String name = this.getName();
		if(event.getDragAction() == DnDConstants.ACTION_COPY)
		{
			cursor = DragSource.DefaultCopyDrop;
		}
		event.startDrag(cursor, new TransferableTypeName(this.objectTN));
	}
	
	public void mouseDragged(MouseEvent e)
	{}
	
	public void mouseMoved(MouseEvent e)
	{}
	
	public void mouseClicked(MouseEvent e)
	{
		int nb = e.getClickCount();
		if(nb == 2)
		{
			this.openInDefaultProgram();
		}
	}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{}
}

package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.FontUtility;
import miscellaneous.Pair;
import net.miginfocom.swing.MigLayout;
import tables.DimensionMap;

public class FileDropCellController {
	
	public int row;
	public int column;
	public boolean selected;
	public Vector<Pair<DimensionMap,String>> theFiles;
	
	// GUI
	private JLabel title = new JLabel("");
	private FileDropCellPanel panel;
	private DistributorArray parentController;
	
	public FileDropCellController(DistributorArray parentController)
	{
		this.parentController = parentController;
	}
	
	public void setRowAndColumn(int row, int column)
	{
		this.row = row;
		this.column = column;
		title.setText("X" + row + ".Y" + column);
		panel().rebuild();
	}
	
	public void setSelected(boolean selected)
	{
		this.selected = selected;
		panel().rebuild();
	}
	
	public void setFiles(Vector<Pair<DimensionMap,String>> theFiles)
	{
		this.theFiles = theFiles;
		panel().rebuild();
	}
	
	public FileDropCellPanel panel()
	{
		if(panel == null)
		{
			panel = new FileDropCellPanel();
		}
		return panel;
	}
	
	class FileDropCellPanel extends JPanel implements MouseListener {
		
		private static final long serialVersionUID = 1L;
		
		FileDropCellPanel()
		{
			rebuild();
			this.addMouseListener(this);
			
			new FileListDropArea(this, this);
		}
		
		public void addFile(File f)
		{
			parentController.addFile(row, column, f);
		}
		
		public void rebuild()
		{
			title.setMaximumSize(new Dimension(80, 20));
			title.setAlignmentX(LEFT_ALIGNMENT);
			
			// Set the background
			this.setBackground(DisplayStatics.lightBackground);
			
			// rebuild the gui
			this.removeAll();
			this.setLayout(new MigLayout("ins 2 2 2 2", "[fill,grow]", "2[20]2[fill,grow]2"));
			
			// The title and the label color
			this.add(title, "growx, wrap, wmin 0");
			
			// The data view
			if(theFiles == null)
			{
				JPanel nullPane = new JPanel();
				nullPane.setBackground(DisplayStatics.background);
				this.add(nullPane, "width 100%, height 100%");
			}
			else
			{
				JList jlist = new JList();
				jlist.setBackground(DisplayStatics.background);
				jlist.setForeground(Color.WHITE);
				jlist.setFont(FontUtility.defaultFonts);
				jlist.setCellRenderer(new FileListCellRenderer());
				
				DefaultListModel newModel = new DefaultListModel();
				// Make a string with the dimension name for the files
				for (Pair<DimensionMap,String> pair : theFiles)
				{
					newModel.addElement(pair.p1.toString() + " " + FileUtility.getFileNameWithExtension(pair.p2));
				}
				
				jlist.setModel(newModel);
				
				JScrollPane fileListScroll = new JScrollPane(jlist);
				fileListScroll.setBackground(DisplayStatics.background);
				fileListScroll.setBorder(BorderFactory.createEmptyBorder());
				
				this.add(fileListScroll, "width 100%, height 100%");
			}
			
			// The selection status
			if(selected)
			{
				this.setBorder(BorderFactory.createLineBorder(Color.red, 2));
			}
			else
			{
				this.setBorder(BorderFactory.createLineBorder(DisplayStatics.background, 2));
			}
			
			this.invalidate();
			this.validate();
			this.repaint();
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
			parentController.setSelectionForCellAtIndexXY(row, column, !selected);
		}
		
	}
	
	class FileListDropArea extends DropTargetAdapter {
		
		@SuppressWarnings("unused")
		private DropTarget dropTarget;
		private JPanel area;
		private FileDropCellPanel parent;
		
		public FileListDropArea(FileDropCellPanel parent, JPanel area)
		{
			this.area = area;
			this.parent = parent;
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
					for (File f : files)
						parent.addFile(f);
					
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

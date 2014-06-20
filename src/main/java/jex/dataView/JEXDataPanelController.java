package jex.dataView;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.jexTabPanel.jexDistributionPanel.FileListCellRenderer;
import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataReader.LabelReader;
import Database.DataReader.MovieReader;
import Database.DataReader.ValueReader;
import Database.SingleUserDatabase.JEXWriter;

public class JEXDataPanelController {
	
	public JEXData data;
	public JEXEntry entry;
	
	public JEXDataPanelController()
	{   
		
	}
	
	/**
	 * Set the data of this JEXDataView
	 */
	public void setData(JEXData data)
	{
		this.data = data;
	}
	
	/**
	 * Set the data of this JEXDataView
	 */
	public void setEntry(JEXEntry entry)
	{
		this.entry = entry;
	}
	
	public JPanel panel()
	{
		if(this.data != null && this.data.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return new JEXAlternateImageView();
			// return new JEXImageView();
		}
		else if(this.data != null && this.data.getTypeName().getType().equals(JEXData.FILE))
		{
			return new JEXFileView();
		}
		else if(this.data != null && this.data.getTypeName().getType().equals(JEXData.MOVIE))
		{
			return new JEXMovieView();
		}
		else if(this.data != null && this.data.getTypeName().getType().equals(JEXData.VALUE))
		{
			return new JEXValueView();
		}
		else if(this.data != null && this.data.getTypeName().getType().equals(JEXData.LABEL))
		{
			return new JEXLabelView();
		}
		else
		{
			return new JEXGenericDataView();
		}
	}
	
	class JEXValueView extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		JEXValueView()
		{
			this.setBackground(DisplayStatics.lightBackground);
			this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			
			String valueStr = ValueReader.readValueObject(JEXDataPanelController.this.data);
			JLabel valueLabel = new JLabel(valueStr);
			this.add(valueLabel);
		}
	}
	
	class JEXLabelView extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		JEXLabelView()
		{
			this.setBackground(DisplayStatics.lightBackground);
			this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			
			String valueStr = "" + LabelReader.readLabelValue(JEXDataPanelController.this.data);
			JLabel valueLabel = new JLabel(valueStr);
			this.add(valueLabel);
		}
	}
	
	class JEXGenericDataView extends ClickableFileListPanel {
		
		private static final long serialVersionUID = 1L;
		
		JEXGenericDataView()
		{
			String jxdRelativePath = JEXDataPanelController.this.data.getDetachedRelativePath();
			Vector<String> fileList = new Vector<String>();
			if(jxdRelativePath == null)
			{
				fileList.add("Data not saved to DB yet.");
			}
			else
			{
				fileList.add(JEXWriter.getDatabaseFolder() + File.separator + jxdRelativePath);
			}
			fileList.add(JEXDataPanelController.this.data.toString());
			JEXDataPanelController.addFileListScrollPane(this, fileList);
		}
	}
	
	class JEXFileView extends ClickableFileListPanel {
		
		private static final long serialVersionUID = 1L;
		
		JEXFileView()
		{
			TreeMap<DimensionMap,String> files = FileReader.readObjectToFilePathTable(JEXDataPanelController.this.data);
			JEXDataPanelController.addFileListScrollPane(this, files.values());
		}
	}
	
	class JEXMovieView extends ClickableFileListPanel {
		
		private static final long serialVersionUID = 1L;
		
		JEXMovieView()
		{
			String path = MovieReader.readMovieObject(JEXDataPanelController.this.data);
			Vector<String> fileList = new Vector<String>();
			fileList.add(path);
			JEXDataPanelController.addFileListScrollPane(this, fileList);
		}
	}
	
	class JEXAlternateImageView extends ClickableFileListPanel {
		
		private static final long serialVersionUID = 1L;
		
		JEXAlternateImageView()
		{
			TreeMap<DimensionMap,String> files = ImageReader.readObjectToImagePathTable(JEXDataPanelController.this.data);
			JEXDataPanelController.addFileListScrollPane(this, files.values());
		}
	}
	
	class JEXImageView extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private double scale = 1.0;
		
		JEXImageView()
		{
			this.repaint();
		}
		
		@Override
		public void paint(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			
			int wpane = this.getWidth();
			int hpane = this.getHeight();
			g2.setColor(DisplayStatics.lightBackground);
			g2.setColor(DisplayStatics.dividerColor);
			g2.fillRect(0, 0, wpane, hpane);
			
			if(JEXDataPanelController.this.data == null)
			{
				return;
			}
			ImagePlus source = ImageReader.readObjectToImagePlus(JEXDataPanelController.this.data);
			
			if(source != null && source.getProcessor() != null)
			{
				// Find the new scale of the image
				int w = source.getWidth();
				int h = source.getHeight();
				double scaleX = ((double) wpane) / ((double) w);
				double scaleY = ((double) hpane) / ((double) h);
				this.scale = Math.min(scaleX, scaleY);
				int newW = (int) (this.scale * w);
				int newH = (int) (this.scale * h);
				
				// Center the image
				int yPos = hpane / 2 - newH / 2;
				int xPos = wpane / 2 - newW / 2;
				
				// resize the image
				ImageProcessor imp = source.getProcessor();
				if(imp == null)
				{
					return;
				}
				imp = imp.resize(newW);
				Image image = imp.getBufferedImage();
				
				// draw the image
				g2.setColor(DisplayStatics.lightBackground);
				g2.fillRect(0, 0, wpane, hpane);
				g2.drawImage(image, xPos, yPos, this);
				
			}
		}
	}
	
	public static void addFileListScrollPane(ClickableFileListPanel toAddTo, Collection<String> paths)
	{
		toAddTo.setLayout(new MigLayout("flowy, ins 10, gapy 0", "[fill,grow]", "[fill,grow]"));
		toAddTo.setBackground(DisplayStatics.lightBackground);
		
		JList displayListOfFiles = new JList();
		displayListOfFiles.setBackground(DisplayStatics.lightBackground);
		displayListOfFiles.setFont(FontUtility.defaultFonts);
		displayListOfFiles.setCellRenderer(new FileListCellRenderer());
		displayListOfFiles.addMouseListener(toAddTo);
		
		DefaultListModel newModel = new DefaultListModel();
		for (String path : paths)
		{
			newModel.addElement(new File(path));
		}
		displayListOfFiles.setModel(newModel);
		
		JScrollPane fileListScroll = new JScrollPane(displayListOfFiles);
		fileListScroll.setBackground(DisplayStatics.lightBackground);
		fileListScroll.setBorder(BorderFactory.createLineBorder(DisplayStatics.dividerColor));
		
		toAddTo.add(fileListScroll, "grow");
	}
	
}

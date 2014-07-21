package plugins.imageAligner2;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.TypeName;
import guiObject.PixelComponentDisplay;
import ij.ImagePlus;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import plugins.viewer.DataBrowser;
import plugins.viewer.ImageDelegate;
import signals.SSCenter;
import tables.DimTable;
import tables.DimensionMap;

public class ImageAligner implements PlugInController {
	
	public PlugIn dialog;
	public DataBrowser backDataBrowser, frontDataBrowser;
	public InfoPanel infoPanel;
	public ImageAlignerInterface leftBack, leftFront, middleBack, middleFront, rightBack, rightFront;
	public PixelComponentDisplay left, middle, right;
	public JPanel panel;
	public JPanel controls;
	public JTextField alignmentName;
	
	public Vector<JEXEntry> entries;
	public TypeName imageTN;
	public String frontImageFilePath, backImageFilePath;
	public ImagePlus frontImage, backImage;
	
	public double alpha = 0.5, leftZoom = 1, middleZoom = 5, rightZoom = 10;
	public int dx = 0, dy = 0;
	public boolean clickable = true, draggable = true, paintsBackground = false;
	
	public ImageAligner()
	{
		this.dialog = new PlugIn(this);
		this.dialog.setBounds(100, 100, 1000, 400);
		this.initializeDisplayPanels();
		this.initializeControls();
		this.initializeMain();
		
		this.dialog.setVisible(true);
	}
	
	private void initializeControls()
	{
		this.controls = new JPanel();
		this.controls.setBackground(DisplayStatics.menuBackground);
		this.controls.setLayout(new MigLayout("flowy, ins 3", "[fill,grow]", "[]3[]3[]3[]3[fill,grow]3[]3[]"));
		this.backDataBrowser = new DataBrowser();
		this.backDataBrowser.setScrollBars(1, null, null);
		this.backDataBrowser.setToggleEnabled(false);
		this.frontDataBrowser = new DataBrowser();
		this.frontDataBrowser.setScrollBars(1, null, null);
		this.frontDataBrowser.setToggleEnabled(false);
		this.infoPanel = new InfoPanel();
		JPanel spacer = new JPanel();
		JPanel alignmentNamePanel = new JPanel();
		alignmentNamePanel.setBackground(DisplayStatics.menuBackground);
		alignmentNamePanel.setLayout(new MigLayout("flowx, ins 3", "[]5[left,grow]", "[]"));
		JLabel alignmentLabel = new JLabel("Name");
		this.alignmentName = new JTextField("Alignment");
		alignmentNamePanel.add(alignmentLabel);
		alignmentNamePanel.add(this.alignmentName, "growx");
		spacer.setBackground(DisplayStatics.menuBackground);
		this.controls.add(new JLabel("Background Image:"), "growx");
		this.controls.add(this.backDataBrowser.panel(), "growx");
		this.controls.add(new JLabel("Foreground Image:"), "growx");
		this.controls.add(this.frontDataBrowser.panel(), "growx");
		this.controls.add(spacer, "grow");
		this.controls.add(alignmentNamePanel, "growx");
		this.controls.add(this.infoPanel.panel(), "growx");
	}
	
	private void initializeDisplayPanels()
	{
		this.leftBack = new ImageAlignerInterface();
		this.leftBack.setZoom(leftZoom, null);
		this.leftFront = new ImageAlignerInterface();
		this.leftFront.setZoom(leftZoom, null);
		this.leftFront.setAlpha(alpha);
		this.leftFront.setClickable(clickable);
		this.leftFront.setDraggable(draggable);
		this.leftFront.setPaintsBackground(paintsBackground);
		this.middleBack = new ImageAlignerInterface();
		this.middleBack.setZoom(middleZoom, null);
		this.middleFront = new ImageAlignerInterface();
		this.middleFront.setZoom(middleZoom, null);
		this.middleFront.setAlpha(alpha);
		this.middleFront.setClickable(clickable);
		this.middleFront.setDraggable(draggable);
		this.middleFront.setPaintsBackground(paintsBackground);
		this.rightBack = new ImageAlignerInterface();
		this.rightBack.setZoom(rightZoom, null);
		this.rightFront = new ImageAlignerInterface();
		this.rightFront.setZoom(rightZoom, null);
		this.rightFront.setAlpha(alpha);
		this.rightFront.setClickable(clickable);
		this.rightFront.setDraggable(draggable);
		this.rightFront.setPaintsBackground(paintsBackground);
		
		this.left = new PixelComponentDisplay(leftBack, leftFront);
		this.middle = new PixelComponentDisplay(middleBack, middleFront);
		this.right = new PixelComponentDisplay(rightBack, rightFront);
		
		this.leftBack.setDisplay(this.left);
		this.leftFront.setDisplay(this.left);
		this.middleBack.setDisplay(this.middle);
		this.middleFront.setDisplay(this.middle);
		this.rightBack.setDisplay(this.right);
		this.rightFront.setDisplay(this.right);
	}
	
	private void initializeMain()
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.background);
		this.panel.setLayout(new MigLayout("flowx, ins 3", "[fill,grow 20]3[fill,grow 10]3[fill,grow 10]3[200]", "[fill,grow]"));
		this.panel.add(this.left, "grow");
		this.panel.add(this.middle, "grow");
		this.panel.add(this.right, "grow");
		this.panel.add(this.controls, "grow");
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this.panel, BorderLayout.CENTER);
		
		SSCenter.defaultCenter().connect(this.backDataBrowser, DataBrowser.SIG_EntryChanged_NULL, this, "entryChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.backDataBrowser, DataBrowser.SIG_ImageDimensionMapChanged_NULL, this, "imageChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.frontDataBrowser, DataBrowser.SIG_EntryChanged_NULL, this, "entryChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.frontDataBrowser, DataBrowser.SIG_ImageDimensionMapChanged_NULL, this, "imageChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.leftFront, ImageAlignerInterface.SIG_DoubleClick_ImagedDelegate_Point, this, "doubleClick", new Class[] { ImageDelegate.class, Point.class });
		SSCenter.defaultCenter().connect(this.middleFront, ImageAlignerInterface.SIG_DoubleClick_ImagedDelegate_Point, this, "doubleClick", new Class[] { ImageDelegate.class, Point.class });
		SSCenter.defaultCenter().connect(this.rightFront, ImageAlignerInterface.SIG_DoubleClick_ImagedDelegate_Point, this, "doubleClick", new Class[] { ImageDelegate.class, Point.class });
		SSCenter.defaultCenter().connect(this.leftFront, ImageAlignerInterface.SIG_Dragged_Rectangle, this, "imageDragged", new Class[] { Rectangle.class });
		SSCenter.defaultCenter().connect(this.middleFront, ImageAlignerInterface.SIG_Dragged_Rectangle, this, "imageDragged", new Class[] { Rectangle.class });
		SSCenter.defaultCenter().connect(this.rightFront, ImageAlignerInterface.SIG_Dragged_Rectangle, this, "imageDragged", new Class[] { Rectangle.class });
		SSCenter.defaultCenter().connect(this.infoPanel, InfoPanel.SIG_OkClicked_NULL, this, "saveSpacing", (Class[]) null);
	}
	
	public void setDBSelection(TreeSet<JEXEntry> entries, TypeName tn)
	{
		this.entries = new Vector<JEXEntry>(entries);
		this.imageTN = tn;
		
		DimTable imageDimTable = this.getImageDimTable(entries.first());
		
		this.backDataBrowser.setScrollBars(this.entries.size(), imageDimTable, new DimTable());
		this.frontDataBrowser.setScrollBars(1, imageDimTable, new DimTable());
		
		this.dialog.invalidate();
		this.dialog.setTitle(imageTN.getName());
		this.entryChanged();
		this.dialog.validate();
		this.dialog.repaint();
		// this.setZooms();
		
	}
	
	public void entryChanged()
	{
		JEXEntry entry = this.currentEntry();
		DimTable imageDimTable = this.getImageDimTable(entry);
		this.backDataBrowser.storeDimState();
		this.backDataBrowser.setImageDimTable(imageDimTable);
		this.backDataBrowser.recallDimState(); // set what we can
		this.backDataBrowser.repaint();
		this.imageChanged();
	}
	
	public JEXEntry currentEntry()
	{
		int entryIndex = this.backDataBrowser.currentEntry();
		if(this.entries == null)
			return null;
		return this.entries.get(entryIndex);
	}
	
	private void setBackImage(ImagePlus image)
	{
		this.leftBack.setImage(image);
		this.middleBack.setImage(image);
		this.rightBack.setImage(image);
	}
	
	private void setFrontImage(ImagePlus image)
	{
		this.leftFront.setImage(image);
		this.middleFront.setImage(image);
		this.rightFront.setImage(image);
	}
	
	@SuppressWarnings("unused")
	private void setZooms()
	{
		this.leftZoom = this.leftFront.delegate.getZoom();
		this.leftFront.setZoom(this.leftZoom, null);
		this.leftBack.setZoom(this.leftZoom, null);
		this.middleFront.setZoom(this.middleZoom, null);
		this.middleBack.setZoom(this.middleZoom, null);
		this.rightFront.setZoom(this.rightZoom, null);
		this.rightBack.setZoom(this.rightZoom, null);
	}
	
	private void setBackIntensities(int min, int max)
	{
		this.leftBack.setDisplayLimits(min, max);
		this.middleBack.setDisplayLimits(min, max);
		this.rightBack.setDisplayLimits(min, max);
	}
	
	private void setFrontIntensities(int min, int max)
	{
		this.leftFront.setDisplayLimits(min, max);
		this.middleFront.setDisplayLimits(min, max);
		this.rightFront.setDisplayLimits(min, max);
	}
	
	public void imageChanged() // Called each time the entry, DimensionMap;
	{
		// Load the appropriate image according to the DimensionMap and entry
		JEXData imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(imageTN, currentEntry());
		if(imageData == null)
		{
			this.setBackImage(null);
			this.setFrontImage(null);
			return;
		}
		
		JEXDataSingle frontds = imageData.getData(this.frontDataBrowser.currentImageDimMap());
		if(frontds != null)
		{
			this.frontImageFilePath = ImageReader.readImagePath(frontds);
			// this.imageFilePath = ds.get(JEXDataSingle.FOLDERNAME) +
			// File.separator + ds.get(JEXDataSingle.FILENAME);
			
			Logs.log("Opening image at path " + this.frontImageFilePath, 1, this);
			this.frontImage = new ImagePlus(this.frontImageFilePath);
			this.setFrontImage(frontImage);
			this.statusUpdated();
			
			// Set the limits of the image so that you can see something
			double frontImageMin = this.leftFront.delegate.minImageIntensity();
			double frontImageMax = this.leftFront.delegate.maxImageIntensity();
			this.setFrontIntensities((int) frontImageMin, (int) frontImageMax);
		}
		else
		{
			this.frontImageFilePath = null;
			this.setFrontImage(null);
		}
		
		JEXDataSingle backds = imageData.getData(this.backDataBrowser.currentImageDimMap());
		if(backds != null)
		{
			this.backImageFilePath = ImageReader.readImagePath(backds);
			// this.imageFilePath = ds.get(JEXDataSingle.FOLDERNAME) +
			// File.separator + ds.get(JEXDataSingle.FILENAME);
			
			Logs.log("Opening image at path " + this.backImageFilePath, 1, this);
			this.backImage = new ImagePlus(this.backImageFilePath);
			this.setBackImage(backImage);
			this.statusUpdated();
			
			// Set the limits of the image so that you can see something
			double backImageMin = this.leftBack.delegate.minImageIntensity();
			double backImageMax = this.leftBack.delegate.maxImageIntensity();
			this.setBackIntensities((int) backImageMin, (int) backImageMax);
		}
		else
		{
			this.backImageFilePath = null;
			this.setBackImage(null);
		}
		this.statusUpdated();
		this.panel.repaint();
	}
	
	public void imageDragged(Rectangle r)
	{
		this.leftFront.imageDragged(r);
		this.middleFront.imageDragged(r);
		this.rightFront.imageDragged(r);
		this.left.repaint();
		this.right.repaint();
		this.middle.repaint();
		this.statusUpdated();
	}
	
	public void doubleClick(ImageDelegate delegate, Point displayPoint)
	{
		Point frontImagePoint = null;
		Point backImagePoint = null;
		
		// Grab the value of the default zoom that fits the image in the display
		// for later use;
		this.leftZoom = this.leftFront.delegate.getZoom();
		
		if(delegate == this.leftFront.delegate)
		{
			frontImagePoint = this.leftFront.delegate.displayToImage(this.left, displayPoint);
			backImagePoint = this.leftBack.delegate.displayToImage(this.left, displayPoint);
		}
		else if(delegate == this.middleFront.delegate)
		{
			frontImagePoint = this.middleFront.delegate.displayToImage(this.middle, displayPoint);
			backImagePoint = this.middleBack.delegate.displayToImage(this.middle, displayPoint);
		}
		else
		{
			frontImagePoint = this.rightFront.delegate.displayToImage(this.right, displayPoint);
			backImagePoint = this.rightBack.delegate.displayToImage(this.right, displayPoint);
		}
		this.leftFront.setZoom(leftZoom, frontImagePoint);
		this.leftBack.setZoom(leftZoom, backImagePoint);
		this.middleFront.setZoom(middleZoom, frontImagePoint);
		this.middleBack.setZoom(middleZoom, backImagePoint);
		this.rightFront.setZoom(rightZoom, frontImagePoint);
		this.rightBack.setZoom(rightZoom, backImagePoint);
		this.left.repaint();
		this.right.repaint();
		this.middle.repaint();
		// Setting the zoom in the image delegates causes a signal of
		// imageUpdated and statusUpdated
	}
	
	public void statusUpdated()
	{
		if(this.backImageFilePath == null || this.frontImageFilePath == null)
		{
			this.infoPanel.setInfo("?", "?");
		}
		else
		{
			Point pBack = this.rightBack.delegate.getSrcCenter();
			Point pFront = this.rightFront.delegate.getSrcCenter();
			this.dx = -1 * (pFront.x - pBack.x);
			this.dy = -1 * (pFront.y - pBack.y);
			this.infoPanel.setInfo(this.dx, this.dy);
		}
	}
	
	public void saveSpacing()
	{
		Object[] options = { "OK", "Cancel" };
		boolean foundDuplicate = false;
		for (JEXEntry e : this.entries)
		{
			if(JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.VALUE, this.alignmentName.getText()), e) != null)
			{
				foundDuplicate = true;
				break;
			}
		}
		if(foundDuplicate)
		{
			int n = JOptionPane.showOptionDialog(JEXStatics.main, "An object with the name \"" + this.alignmentName.getText() + "\" was found in at least \none of the selected entries. Should these be overwritten?", "Creating alignment object: Overwrite?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if(n == 1)
			{
				Logs.log("Canceling edit to prevent overwriting existing data.", 0, this);
				JEXStatics.statusBar.setStatusText("Canceling object edit to prevent overwriting existing data.");
				return;
			}
		}
		TypeName newValue = new TypeName(JEXData.VALUE, this.alignmentName.getText());
		TreeMap<JEXEntry,Set<JEXData>> dataMap = new TreeMap<JEXEntry,Set<JEXData>>();
		DimensionMap frontMap = this.frontDataBrowser.currentImageDimMap();
		DimensionMap backMap = this.backDataBrowser.currentImageDimMap();
		JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(imageTN, this.currentEntry());
		List<DimensionMap> dims = data.getDimTable().getDimensionMaps();
		int frontIndex = 0;
		int backIndex = 0;
		for (int i = 0; i < dims.size(); i++)
		{
			if(dims.get(i).equals(frontMap))
			{
				frontIndex = i;
			}
			if(dims.get(i).equals(backMap))
			{
				backIndex = i;
			}
		}
		
		String alignment = "" + this.dx + "," + this.dy + "," + backIndex + "," + frontIndex;
		;
		HashSet<JEXData> dataSet;
		JEXData valueData;
		for (JEXEntry e : JEXStatics.jexManager.getSelectedEntries())
		{
			valueData = ValueWriter.makeValueObject(newValue.getName(), alignment);
			dataSet = new HashSet<JEXData>();
			dataSet.add(valueData);
			dataMap.put(e, dataSet);
		}
		JEXStatics.jexDBManager.saveDataListInEntries(dataMap, true);
	}
	
	private DimTable getImageDimTable(JEXEntry e)
	{
		DimTable ret = null;
		JEXData imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.imageTN, e);
		if(imageData != null)
			ret = imageData.getDimTable();
		else
			ret = null;
		return ret;
	}
	
	public void finalizePlugIn()
	{}
	
	public PlugIn plugIn()
	{
		return this.dialog;
	}
	
}

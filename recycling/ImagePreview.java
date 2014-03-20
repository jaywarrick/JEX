package recycling;

import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.ROIPlusSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import jex.statics.DisplayStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import plugins.viewer.DataBrowser;
import plugins.viewer.ImageDisplayController;
import plugins.viewer.LimitAdjuster;
import plugins.viewer.StatusBar;
import signals.SSCenter;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;

public class ImagePreview extends PreviewPanel {
	
	// Variables
	private JEXData imageData;
	private JEXData roiData;
	private boolean smashMode = true;
	private ROIPlusSet roi = null;
	private String imageFilePath = null;
	private Rectangle imageRect = new Rectangle();
	
	// Gui
	private JSplitPane main;
	private DataBrowser dataBrowser;
	private ImageDisplayController display;
	private LimitAdjuster limitAdjuster;
	private StatusBar statusBar;
	// Organizational JPanels
	private JPanel masterDisplayPane, masterControlPane;
	
	public JPanel panel()
	{
		if(this.imageData == null)
		{
			return this.blankPanel();
		}
		JPanel ret = new JPanel();
		ret.setLayout(new BorderLayout());
		ret.add(this.main, BorderLayout.CENTER);
		return ret;
	}
	
	// //////////////////////////////////////
	// ////////// Constructors //////////////
	// //////////////////////////////////////
	
	public ImagePreview(JEXData imageData, JEXData roiData)
	{
		this.imageData = imageData;
		this.roiData = roiData;
		
		this.initializeLimitAdjuster();
		this.initializeDisplay();
		this.initializeDataBrowser();
		this.initializeMain();
		this.setQuickKeys();
		
		this.setImage(imageData);
		this.setRoi(roiData);
		
		this.imageChanged();
		this.pointsChanged();
		
		// Make appropriate connections
		SSCenter.defaultCenter().connect(this.limitAdjuster, LimitAdjuster.SIG_limitsChanged_NULL, this, "setLimits", (Class[]) null);
		SSCenter.defaultCenter().connect(this.display, ImageDisplayController.SIG_StatusUpdated_NULL, this, "statusUpdated", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_SmashedEntriesChanged_NULL, this, "pointsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ImageDimensionMapChanged_NULL, this, "imageDimChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ImageSmashedDimTableChanged_NULL, this, "pointsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_RoiDimensionMapChanged_NULL, this, "roiDimChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_RoiSmashedDimTableChanged_NULL, this, "pointsChanged", (Class[]) null);
		
	}
	
	// //////////////////////////////////////
	// //////////Initializers //////////////
	// //////////////////////////////////////
	
	private void initializeLimitAdjuster()
	{
		this.limitAdjuster = new LimitAdjuster();
		this.limitAdjuster.setBounds(0, 4095);
		this.limitAdjuster.setLowerAndUpper(0, 4095);
		this.limitAdjuster.panel().setBackground(Color.YELLOW);
	}
	
	private void initializeDisplay()
	{
		this.display = new ImageDisplayController();
		this.display.panel().requestFocusInWindow();
		// this.display.panel().setBackground(Color.RED);
	}
	
	private void initializeDataBrowser()
	{
		this.dataBrowser = new DataBrowser();
		this.dataBrowser.setScrollBars(1, this.getImageDimTable(), this.getRoiDimTable());
		// this.dataBrowser.panel().setBackground(Color.BLUE);
	}
	
	private DimTable getImageDimTable()
	{
		if(this.imageData == null)
		{
			return null;
		}
		return this.imageData.getDimTable();
	}
	
	private DimTable getRoiDimTable()
	{
		if(this.roiData == null)
		{
			return null;
		}
		return this.roiData.getDimTable();
	}
	
	private void initializeMain()
	{
		this.main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.main.setResizeWeight(1);
		this.main.setBackground(DisplayStatics.background);
		this.main.setBorder(BorderFactory.createEmptyBorder());
		this.statusBar = new StatusBar();
		this.masterDisplayPane = new JPanel();// new VerticalBoxLayoutPane(new
		// JPanel[]{this.statusBar.panel(),
		// this.display.panel()});
		this.masterDisplayPane.setBackground(DisplayStatics.background);
		this.masterDisplayPane.setLayout(new MigLayout("flowy,ins 0", "[fill,grow]", "[]0[fill,grow]0[]"));
		this.masterDisplayPane.add(this.statusBar.panel(), "gap 5, growx, height 15:15:15");
		this.masterDisplayPane.add(this.display.panel(), "grow");
		this.masterDisplayPane.add(this.limitAdjuster.panel(), "growx");
		this.masterDisplayPane.setMinimumSize(new Dimension(50, 50));
		this.masterControlPane = new JPanel();// new VerticalBoxLayoutPane(new
		// JPanel[]{this.dataBrowser.panel(),this.limitAdjuster.panel()});
		this.masterControlPane.setBackground(DisplayStatics.background);
		this.masterControlPane.setLayout(new MigLayout("flowy, ins 0, bottom", "[fill,grow]", "[]15[]"));
		this.masterControlPane.add(this.dataBrowser.panel(), "growx");
		this.masterControlPane.add(this.limitAdjuster.panel(), "growx");
		this.masterControlPane.setMinimumSize(new Dimension(50, 50));
		this.main.setLeftComponent(this.masterDisplayPane);
		this.main.setRightComponent(this.masterControlPane);
	}
	
	// //////////////////////////////////////
	// ////////// Actions //////////////
	// //////////////////////////////////////
	
	public void setLimits()
	{
		if(this.display.hasImage())
		{
			this.display.setLimits(this.limitAdjuster.lowerIndex(), this.limitAdjuster.upperIndex());
		}
	}
	
	private void setImage(JEXData imageData)
	{
		this.imageData = imageData;
		this.dataBrowser.setImageDimTable(this.getImageDimTable());
		this.dataBrowser.repaint();
	}
	
	private void setRoi(JEXData roiData)
	{
		this.roiData = roiData;
		ROIPlusSet ret;
		
		// Get a set of ROIPlus's in the form of a ROIPlusSet from the JEXData
		// if possible
		if(roiData == null)
		{
			ret = new ROIPlusSet();
			ret.setName(null);
			this.roi = ret;
			return;
		}
		
		TreeMap<DimensionMap,ROIPlus> potentialRois = new TreeMap<DimensionMap,ROIPlus>();
		potentialRois = RoiReader.readObjectToRoiMap(roiData);
		ret = new ROIPlusSet(potentialRois);
		ret.setName(this.roiData.getTypeName().getName());
		this.roi = ret;
		this.dataBrowser.setRoiDimTable(this.getImageDimTable());
		this.dataBrowser.repaint();
	}
	
	// //////////////////////////////////////
	// ////////// Reactions //////////////
	// //////////////////////////////////////
	
	public void repaint()
	{
		this.dataBrowser.setImageDimTable(this.getImageDimTable());
		this.dataBrowser.setRoiDimTable(this.getImageDimTable());
		this.imageChanged();
		this.pointsChanged();
	}
	
	public void imageDimChanged()
	{
		this.imageChanged();
		this.pointsChanged();
	}
	
	public void roiDimChanged()
	{
		this.pointsChanged();
	}
	
	public void pointsChanged()
	{
		List<PointList> smashedPoints = new Vector<PointList>();
		if(this.smashMode)
		{
			// get the pattern for the first
			smashedPoints.addAll(this.roi.getPatternedRoisForDimension(this.dataBrowser.smashedRoiDimTable().getDimensionMaps().get(0)));
			// show the prototype rois for the rest
			smashedPoints.addAll(this.roi.getEditablePointsForDimensions(this.dataBrowser.smashedRoiDimTable().getDimensionMaps()));
		}
		PointList editablePoints = this.roi.getEditablePointsForDimension(this.dataBrowser.currentRoiDimMap());
		this.display.setMode(ImageDisplayController.MODE_VIEW);
		this.display.setRoiDisplay(this.roi.type(), editablePoints, smashedPoints); // causes
		// repaint
		// of
		// display
	}
	
	public void imageChanged() // Called each time the entry, DimensionMap;
	{
		// Load the appropriate image according to the DimensionMap and entry
		if(this.imageData == null)
		{
			this.display.setImage(null);
			this.statusUpdated();
			return;
		}
		
		JEXDataSingle ds = this.imageData.getData(this.dataBrowser.currentImageDimMap());
		if(ds != null)
		{
			this.imageFilePath = ImageReader.readImagePath(ds);
			// this.imageFilePath = ds.get(JEXDataSingle.FOLDERNAME) +
			// File.separator + ds.get(JEXDataSingle.FILENAME);
			
			Logs.log("Opening image at path " + this.imageFilePath, 1, this);
			this.display.setImage(this.imageFilePath);
			this.imageRect = this.display.imageRect();
			this.statusUpdated();
			
			// Set the limits of the image so that you can see something
			double displayMin = this.display.minDisplayIntensity();
			double displayMax = this.display.maxDisplayIntensity();
			double imageMax = this.display.maxImageIntensity();
			int bitDepth = this.display.bitDepth();
			if(bitDepth >= 12 && imageMax <= 4095)
			{
				bitDepth = 12;
			}
			if(imageMax <= 255)
			{
				bitDepth = 8;
			}
			this.limitAdjuster.setBounds(0, (int) (Math.pow(2, bitDepth) - 1));
			this.limitAdjuster.setLowerAndUpper(displayMin, displayMax);
			this.display.setLimits(this.limitAdjuster.lowerIndex(), this.limitAdjuster.upperIndex());
		}
		else
		{
			this.display.setImage(null);
			this.statusUpdated();
			return;
		}
		
	}
	
	public void statusUpdated()
	{
		if(!this.display.hasImage())
		{
			this.statusBar.setText("");
		}
		else
		{
			Point p = this.display.currentImageLocation();
			Rectangle roi = this.display.roiRect();
			String roiRect = "";
			if(roi != null)
			{
				roiRect = "Roi:" + roi.x + "," + roi.y + "," + roi.width + "," + roi.height;
			}
			this.statusBar.setText("[" + this.imageRect.width + "X" + this.imageRect.height + "]  " + roiRect + "  loc:" + (p.x + 1) + "," + (p.y + 1) + "  Int:" + this.display.getPixelIntensity());
		}
	}
	
	// //////////////////////////////////////
	// ////////// PlugIn Stuff //////////////
	// //////////////////////////////////////
	
	private void setQuickKeys()
	{
		// Always add the actions to the action map of the menuPane because that
		// is the one that never changes
		// If it does ever change, call setQuickKeys again to reconnect quick
		// keys
		KeyStroke stroke;
		InputMap inputs = this.statusBar.panel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actions = this.statusBar.panel().getActionMap();
		
		// Indexing Actions
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false);
		inputs.put(stroke, "index dim 0 forward");
		actions.put("index dim 0 forward", new ActionIndexDim(this, 0, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 0 backward");
		actions.put("index dim 0 backward", new ActionIndexDim(this, 0, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_2, 0, false);
		inputs.put(stroke, "index dim 1 forward");
		actions.put("index dim 1 forward", new ActionIndexDim(this, 1, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 1 backward");
		actions.put("index dim 1 backward", new ActionIndexDim(this, 1, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, 0, false);
		inputs.put(stroke, "index dim 2 forward");
		actions.put("index dim 2 forward", new ActionIndexDim(this, 2, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 2 backward");
		actions.put("index dim 2 backward", new ActionIndexDim(this, 2, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_4, 0, false);
		inputs.put(stroke, "index dim 3 forward");
		actions.put("index dim 3 forward", new ActionIndexDim(this, 3, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 3 backward");
		actions.put("index dim 3 backward", new ActionIndexDim(this, 3, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_5, 0, false);
		inputs.put(stroke, "index dim 4 forward");
		actions.put("index dim 4 forward", new ActionIndexDim(this, 4, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_5, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 4 backward");
		actions.put("index dim 4 backward", new ActionIndexDim(this, 4, -1));
		
	}
	
	@SuppressWarnings("serial")
	public class ActionIndexDim extends AbstractAction {
		
		private ImagePreview im;
		private int direction;
		private int dim;
		
		public ActionIndexDim(ImagePreview im, int dim, int direction)
		{
			this.im = im;
			this.direction = direction;
			this.dim = dim;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			int amount = 0;
			if(this.direction > 0)
			{
				amount = 1;
			}
			else if(this.direction < 0)
			{
				amount = -1;
			}
			this.im.dataBrowser.indexImageDim(this.dim, amount);
			this.im.imageDimChanged();
		}
	}
}

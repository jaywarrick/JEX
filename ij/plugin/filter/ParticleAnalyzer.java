package ij.plugin.filter;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.Macro;
import ij.Prefs;
import ij.Undo;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ByteStatistics;
import ij.process.ColorProcessor;
import ij.process.ColorStatistics;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.PolygonFiller;
import ij.process.ShortProcessor;
import ij.process.ShortStatistics;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.util.Tools;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Properties;

import logs.Logs;

/**
 * Implements ImageJ's Analyze Particles command.
 * <p>
 * 
 * <pre>
 * 	for each line do
 * 		for each pixel in this line do
 * 			if the pixel value is "inside" the threshold range then
 * 				trace the edge to mark the object
 * 				do the measurement
 * 				fill the object with a color outside the threshold range
 * 			else
 * 				continue the scan
 * </pre>
 */
public class ParticleAnalyzer implements PlugInFilter, Measurements {
	
	/** Display results in the ImageJ console. */
	public static final int SHOW_RESULTS = 1;
	
	/** Obsolete */
	public static final int SHOW_SUMMARY = 2;
	
	/** Display image containing outlines of measured particles. */
	public static final int SHOW_OUTLINES = 4;
	
	/** Do not measure particles touching edge of image. */
	public static final int EXCLUDE_EDGE_PARTICLES = 8;
	
	/**
	 * Display image containing grayscales masks that identify measured particles.
	 */
	public static final int SHOW_ROI_MASKS = 16;
	
	/** Display a progress bar. */
	public static final int SHOW_PROGRESS = 32;
	
	/** Clear ImageJ console before starting. */
	public static final int CLEAR_WORKSHEET = 64;
	
	/**
	 * Record starting coordinates so outline can be recreated later using doWand(x,y).
	 */
	public static final int RECORD_STARTS = 128;
	
	/** Display a summary. */
	public static final int DISPLAY_SUMMARY = 256;
	
	/** Do not display particle outline image. */
	public static final int SHOW_NONE = 512;
	
	/** Flood fill to ignore interior holes. */
	public static final int INCLUDE_HOLES = 1024;
	
	/** Add particles to ROI Manager. */
	public static final int ADD_TO_MANAGER = 2048;
	
	/** Display image containing binary masks of measured particles. */
	public static final int SHOW_MASKS = 4096;
	
	/** Use 4-connected particle tracing. */
	public static final int FOUR_CONNECTED = 8192;
	
	/** Replace original image with masks. */
	public static final int IN_SITU_SHOW = 16384;
	
	/** Display particle outlines as an overlay. */
	public static final int SHOW_OVERLAY_OUTLINES = 32768;
	
	/** Display filled particle as an overlay. */
	public static final int SHOW_OVERLAY_MASKS = 65536;
	
	static final String OPTIONS = "ap.options";
	
	static final int BYTE = 0, SHORT = 1, FLOAT = 2, RGB = 3;
	static final double DEFAULT_MIN_SIZE = 0.0;
	static final double DEFAULT_MAX_SIZE = Double.POSITIVE_INFINITY;
	
	private static double staticMinSize = 0.0;
	private static double staticMaxSize = DEFAULT_MAX_SIZE;
	private static boolean pixelUnits;
	private static int staticOptions = Prefs.getInt(OPTIONS, CLEAR_WORKSHEET);
	private static String[] showStrings = { "Nothing", "Outlines", "Masks", "Ellipses", "Count Masks", "Overlay Outlines", "Overlay Masks" };
	private static double staticMinCircularity = 0.0, staticMaxCircularity = 1.0;
	private static String prevHdr;
	
	protected static final int NOTHING = 0, OUTLINES = 1, MASKS = 2, ELLIPSES = 3, ROI_MASKS = 4, OVERLAY_OUTLINES = 5, OVERLAY_MASKS = 6;
	protected static int staticShowChoice;
	protected ImagePlus imp;
	protected ResultsTable rt;
	protected Analyzer analyzer;
	protected int slice;
	protected boolean processStack;
	protected boolean showResults, excludeEdgeParticles, showSizeDistribution, resetCounter, showProgress, recordStarts, displaySummary, floodFill, addToManager, inSituShow;
	
	private String summaryHdr = "Slice\tCount\tTotal Area\tAverage Size\tArea Fraction";
	private double level1, level2;
	private double minSize, maxSize;
	private double minCircularity, maxCircularity;
	private int showChoice;
	private int options;
	private int measurements;
	private Calibration calibration;
	// private String arg;
	private double fillColor;
	// private boolean thresholdingLUT;
	private ImageProcessor drawIP;
	private int width, height;
	private boolean canceled;
	private ImageStack outlines;
	private IndexColorModel customLut;
	private int particleCount;
	private int maxParticleCount = 0;
	// private int totalCount;
	private TextWindow tw;
	private Wand wand;
	private int imageType, imageType2;
	private boolean roiNeedsImage;
	private int minX, maxX, minY, maxY;
	private ImagePlus redirectImp;
	private ImageProcessor redirectIP;
	private PolygonFiller pf;
	private Roi saveRoi;
	private int beginningCount;
	private Rectangle r;
	private ImageProcessor mask;
	private double totalArea;
	private FloodFiller ff;
	private Polygon polygon;
	private RoiManager roiManager;
	private ImagePlus outputImage;
	private boolean hideOutputImage;
	private int roiType;
	private int wandMode = Wand.LEGACY_MODE;
	private Overlay overlay;
	boolean blackBackground;
	
	// Output data
	public ArrayList<Roi> foundRois;
	public ImagePlus      outlineImage;
	
	/**
	 * Constructs a ParticleAnalyzer.
	 * 
	 * @param options
	 *            a flag word created by Oring SHOW_RESULTS, EXCLUDE_EDGE_PARTICLES, etc.
	 * @param measurements
	 *            a flag word created by ORing constants defined in the Measurements interface
	 * @param rt
	 *            a ResultsTable where the measurements will be stored
	 * @param minSize
	 *            the smallest particle size in pixels
	 * @param maxSize
	 *            the largest particle size in pixels
	 * @param minCirc
	 *            minimum circularity
	 * @param maxCirc
	 *            maximum circularity
	 */
	public ParticleAnalyzer(int options, int measurements, ResultsTable rt, double minSize, double maxSize, double minCirc, double maxCirc)
	{
		this.options = options;
		this.measurements = measurements;
		this.rt = rt;
		if(this.rt == null)
		{
			this.rt = new ResultsTable();
		}
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.minCircularity = minCirc;
		this.maxCircularity = maxCirc;
		this.slice = 1;
		if((options & SHOW_ROI_MASKS) != 0)
		{
			this.showChoice = ROI_MASKS;
		}
		if((options & SHOW_OVERLAY_OUTLINES) != 0)
		{
			this.showChoice = OVERLAY_OUTLINES;
		}
		if((options & SHOW_OVERLAY_MASKS) != 0)
		{
			this.showChoice = OVERLAY_MASKS;
		}
		if((options & SHOW_OUTLINES) != 0)
		{
			this.showChoice = OUTLINES;
		}
		if((options & SHOW_MASKS) != 0)
		{
			this.showChoice = MASKS;
		}
		if((options & SHOW_NONE) != 0)
		{
			this.showChoice = NOTHING;
		}
		if((options & FOUR_CONNECTED) != 0)
		{
			this.wandMode = Wand.FOUR_CONNECTED;
			options |= INCLUDE_HOLES;
		}
	}
	
	/**
	 * Constructs a ParticleAnalyzer using the default min and max circularity values (0 and 1).
	 */
	public ParticleAnalyzer(int options, int measurements, ResultsTable rt, double minSize, double maxSize)
	{
		this(options, measurements, rt, minSize, maxSize, 0.0, 1.0);
	}
	
	/** Default constructor */
	public ParticleAnalyzer()
	{
		this.slice = 1;
	}
	
	@Override
	public int setup(String arg, ImagePlus imp)
	{
		// this.arg = arg;
		this.imp = imp;
		IJ.register(ParticleAnalyzer.class);
		if(imp == null)
		{
			IJ.noImage();
			return DONE;
		}
		if(imp.getBitDepth() == 24 && !this.isBinaryRGB(imp))
		{
			IJ.error("Particle Analyzer", "RGB images must be converted to binary using\n" + "Process>Binary>Make Binary or thresholded\n" + "using Image>Adjust>Color Threshold.");
			return DONE;
		}
		if(!this.showDialog())
		{
			return DONE;
		}
		int baseFlags = DOES_ALL + NO_CHANGES + NO_UNDO;
		int flags = IJ.setupDialog(imp, baseFlags);
		this.processStack = (flags & DOES_STACKS) != 0;
		this.slice = 0;
		this.saveRoi = imp.getRoi();
		if(this.saveRoi != null && this.saveRoi.getType() != Roi.RECTANGLE && this.saveRoi.isArea())
		{
			this.polygon = this.saveRoi.getPolygon();
		}
		imp.startTiming();
		return flags;
	}
	
	@Override
	public void run(ImageProcessor ip)
	{
		if(this.canceled)
		{
			return;
		}
		this.slice++;
		if(this.imp.getStackSize() > 1 && this.processStack)
		{
			this.imp.setSlice(this.slice);
		}
		if(this.imp.getType() == ImagePlus.COLOR_RGB)
		{
			ip = ip.convertToByte(false);
			int t = Prefs.blackBackground ? 255 : 0;
			ip.setThreshold(t, t, ImageProcessor.NO_LUT_UPDATE);
		}
		if(!this.analyze(this.imp, ip))
		{
			this.canceled = true;
		}
		if(this.slice == this.imp.getStackSize())
		{
			this.imp.updateAndDraw();
			if(this.saveRoi != null)
			{
				this.imp.setRoi(this.saveRoi);
			}
		}
	}
	
	/** Displays a modal options dialog. */
	public boolean showDialog()
	{
		Calibration cal = this.imp != null ? this.imp.getCalibration() : (new Calibration());
		double unitSquared = cal.pixelWidth * cal.pixelHeight;
		if(pixelUnits)
		{
			unitSquared = 1.0;
		}
		if(Macro.getOptions() != null)
		{
			boolean oldMacro = this.updateMacroOptions();
			if(oldMacro)
			{
				unitSquared = 1.0;
			}
			staticMinSize = 0.0;
			staticMaxSize = DEFAULT_MAX_SIZE;
			staticMinCircularity = 0.0;
			staticMaxCircularity = 1.0;
			staticShowChoice = NOTHING;
		}
		GenericDialog gd = new GenericDialog("Analyze Particles");
		this.minSize = staticMinSize;
		this.maxSize = staticMaxSize;
		this.minCircularity = staticMinCircularity;
		this.maxCircularity = staticMaxCircularity;
		this.showChoice = staticShowChoice;
		if(this.maxSize == 999999)
		{
			this.maxSize = DEFAULT_MAX_SIZE;
		}
		this.options = staticOptions;
		String unit = cal.getUnit();
		boolean scaled = cal.scaled();
		if(unit.equals("inch"))
		{
			unit = "pixel";
			unitSquared = 1.0;
			scaled = false;
			pixelUnits = true;
		}
		String units = unit + "^2";
		int places = 0;
		double cmin = this.minSize * unitSquared;
		if((int) cmin != cmin)
		{
			places = 2;
		}
		double cmax = this.maxSize * unitSquared;
		if((int) cmax != cmax && cmax != DEFAULT_MAX_SIZE)
		{
			places = 2;
		}
		String minStr = ResultsTable.d2s(cmin, places);
		if(minStr.indexOf("-") != -1)
		{
			for (int i = places; i <= 6; i++)
			{
				minStr = ResultsTable.d2s(cmin, i);
				if(minStr.indexOf("-") == -1)
				{
					break;
				}
			}
		}
		String maxStr = ResultsTable.d2s(cmax, places);
		if(maxStr.indexOf("-") != -1)
		{
			for (int i = places; i <= 6; i++)
			{
				maxStr = ResultsTable.d2s(cmax, i);
				if(maxStr.indexOf("-") == -1)
				{
					break;
				}
			}
		}
		if(scaled)
		{
			gd.setInsets(5, 0, 0);
		}
		gd.addStringField("Size (" + units + "):", minStr + "-" + maxStr, 12);
		if(scaled)
		{
			gd.setInsets(0, 40, 5);
			gd.addCheckbox("Pixel units", pixelUnits);
		}
		gd.addStringField("Circularity:", IJ.d2s(this.minCircularity) + "-" + IJ.d2s(this.maxCircularity), 12);
		gd.addChoice("Show:", showStrings, showStrings[this.showChoice]);
		String[] labels = new String[8];
		boolean[] states = new boolean[8];
		labels[0] = "Display results";
		states[0] = (this.options & SHOW_RESULTS) != 0;
		labels[1] = "Exclude on edges";
		states[1] = (this.options & EXCLUDE_EDGE_PARTICLES) != 0;
		labels[2] = "Clear results";
		states[2] = (this.options & CLEAR_WORKSHEET) != 0;
		labels[3] = "Include holes";
		states[3] = (this.options & INCLUDE_HOLES) != 0;
		labels[4] = "Summarize";
		states[4] = (this.options & DISPLAY_SUMMARY) != 0;
		labels[5] = "Record starts";
		states[5] = (this.options & RECORD_STARTS) != 0;
		labels[6] = "Add to Manager";
		states[6] = (this.options & ADD_TO_MANAGER) != 0;
		labels[7] = "In_situ Show";
		states[7] = (this.options & IN_SITU_SHOW) != 0;
		gd.addCheckboxGroup(4, 2, labels, states);
		gd.addHelp(IJ.URL + "/docs/menus/analyze.html#ap");
		gd.showDialog();
		if(gd.wasCanceled())
		{
			return false;
		}
		
		String size = gd.getNextString(); // min-max size
		if(scaled)
		{
			pixelUnits = gd.getNextBoolean();
		}
		if(pixelUnits)
		{
			unitSquared = 1.0;
		}
		else
		{
			unitSquared = cal.pixelWidth * cal.pixelHeight;
		}
		String[] minAndMax = Tools.split(size, " -");
		double mins = Tools.parseDouble(minAndMax[0]);
		double maxs = minAndMax.length == 2 ? Tools.parseDouble(minAndMax[1]) : Double.NaN;
		this.minSize = Double.isNaN(mins) ? DEFAULT_MIN_SIZE : mins / unitSquared;
		this.maxSize = Double.isNaN(maxs) ? DEFAULT_MAX_SIZE : maxs / unitSquared;
		if(this.minSize < DEFAULT_MIN_SIZE)
		{
			this.minSize = DEFAULT_MIN_SIZE;
		}
		if(this.maxSize < this.minSize)
		{
			this.maxSize = DEFAULT_MAX_SIZE;
		}
		staticMinSize = this.minSize;
		staticMaxSize = this.maxSize;
		
		minAndMax = Tools.split(gd.getNextString(), " -"); // min-max
		// circularity
		double minc = Tools.parseDouble(minAndMax[0]);
		double maxc = minAndMax.length == 2 ? Tools.parseDouble(minAndMax[1]) : Double.NaN;
		this.minCircularity = Double.isNaN(minc) ? 0.0 : minc;
		this.maxCircularity = Double.isNaN(maxc) ? 1.0 : maxc;
		if(this.minCircularity < 0.0 || this.minCircularity > 1.0)
		{
			this.minCircularity = 0.0;
		}
		if(this.maxCircularity < this.minCircularity || this.maxCircularity > 1.0)
		{
			this.maxCircularity = 1.0;
		}
		if(this.minCircularity == 1.0 && this.maxCircularity == 1.0)
		{
			this.minCircularity = 0.0;
		}
		staticMinCircularity = this.minCircularity;
		staticMaxCircularity = this.maxCircularity;
		
		if(gd.invalidNumber())
		{
			IJ.error("Bins invalid.");
			this.canceled = true;
			return false;
		}
		this.showChoice = gd.getNextChoiceIndex();
		staticShowChoice = this.showChoice;
		if(gd.getNextBoolean())
		{
			this.options |= SHOW_RESULTS;
		}
		else
		{
			this.options &= ~SHOW_RESULTS;
		}
		if(gd.getNextBoolean())
		{
			this.options |= EXCLUDE_EDGE_PARTICLES;
		}
		else
		{
			this.options &= ~EXCLUDE_EDGE_PARTICLES;
		}
		if(gd.getNextBoolean())
		{
			this.options |= CLEAR_WORKSHEET;
		}
		else
		{
			this.options &= ~CLEAR_WORKSHEET;
		}
		if(gd.getNextBoolean())
		{
			this.options |= INCLUDE_HOLES;
		}
		else
		{
			this.options &= ~INCLUDE_HOLES;
		}
		if(gd.getNextBoolean())
		{
			this.options |= DISPLAY_SUMMARY;
		}
		else
		{
			this.options &= ~DISPLAY_SUMMARY;
		}
		if(gd.getNextBoolean())
		{
			this.options |= RECORD_STARTS;
		}
		else
		{
			this.options &= ~RECORD_STARTS;
		}
		if(gd.getNextBoolean())
		{
			this.options |= ADD_TO_MANAGER;
		}
		else
		{
			this.options &= ~ADD_TO_MANAGER;
		}
		if(gd.getNextBoolean())
		{
			this.options |= IN_SITU_SHOW;
		}
		else
		{
			this.options &= ~IN_SITU_SHOW;
		}
		staticOptions = this.options;
		this.options |= SHOW_PROGRESS;
		if((this.options & DISPLAY_SUMMARY) != 0)
		{
			Analyzer.setMeasurements(Analyzer.getMeasurements() | AREA);
		}
		return true;
	}
	
	private boolean isBinaryRGB(ImagePlus imp)
	{
		ImageProcessor ip = imp.getProcessor();
		int[] pixels = (int[]) ip.getPixels();
		int size = imp.getWidth() * imp.getHeight();
		for (int i = 0; i < size; i++)
		{
			if((pixels[i] & 0xffffff) != 0 && (pixels[i] & 0xffffff) != 0xffffff)
			{
				return false;
			}
		}
		return true;
	}
	
	boolean updateMacroOptions()
	{
		String options = Macro.getOptions();
		int index = options.indexOf("maximum=");
		if(index == -1)
		{
			return false;
		}
		index += 8;
		int len = options.length();
		while (index < len - 1 && options.charAt(index) != ' ')
		{
			index++;
		}
		if(index == len - 1)
		{
			return false;
		}
		int min = (int) Tools.parseDouble(Macro.getValue(options, "minimum", "1"));
		int max = (int) Tools.parseDouble(Macro.getValue(options, "maximum", "999999"));
		options = "size=" + min + "-" + max + options.substring(index, len);
		Macro.setOptions(options);
		return true;
	}
	
	/**
	 * Performs particle analysis on the specified image. Returns false if there is an error.
	 */
	public boolean analyze(ImagePlus imp)
	{
		return this.analyze(imp, imp.getProcessor());
	}
	
	/**
	 * Performs particle analysis on the specified ImagePlus and ImageProcessor. Returns false if there is an error.
	 */
	public boolean analyze(ImagePlus imp, ImageProcessor ip)
	{
		if(this.imp == null)
		{
			this.imp = imp;
		}
		this.showResults = (this.options & SHOW_RESULTS) != 0;
		this.excludeEdgeParticles = (this.options & EXCLUDE_EDGE_PARTICLES) != 0;
		this.resetCounter = (this.options & CLEAR_WORKSHEET) != 0;
		this.showProgress = (this.options & SHOW_PROGRESS) != 0;
		this.floodFill = (this.options & INCLUDE_HOLES) == 0;
		this.recordStarts = (this.options & RECORD_STARTS) != 0;
		this.addToManager = (this.options & ADD_TO_MANAGER) != 0;
		this.displaySummary = (this.options & DISPLAY_SUMMARY) != 0;
		this.inSituShow = (this.options & IN_SITU_SHOW) != 0;
		this.outputImage = null;
		this.foundRois = new ArrayList<Roi>(0);
		ip.snapshot();
		ip.setProgressBar(null);
		if(Analyzer.isRedirectImage())
		{
			this.redirectImp = Analyzer.getRedirectImage(imp);
			if(this.redirectImp == null)
			{
				return false;
			}
			int depth = this.redirectImp.getStackSize();
			if(depth > 1 && depth == imp.getStackSize())
			{
				ImageStack redirectStack = this.redirectImp.getStack();
				this.redirectIP = redirectStack.getProcessor(imp.getCurrentSlice());
			}
			else
			{
				this.redirectIP = this.redirectImp.getProcessor();
			}
		}
		if(!this.setThresholdLevels(imp, ip))
		{
			return false;
		}
		this.width = ip.getWidth();
		this.height = ip.getHeight();
		if(!(this.showChoice == NOTHING || this.showChoice == OVERLAY_OUTLINES || this.showChoice == OVERLAY_MASKS))
		{
			this.blackBackground = Prefs.blackBackground && this.inSituShow;
			if(this.slice == 1)
			{
				this.outlines = new ImageStack(this.width, this.height);
			}
			if(this.showChoice == ROI_MASKS)
			{
				this.drawIP = new ShortProcessor(this.width, this.height);
			}
			else
			{
				this.drawIP = new ByteProcessor(this.width, this.height);
			}
			if(this.showChoice == ROI_MASKS)
			{} // Place holder for now...
			else if(this.showChoice == MASKS && !this.blackBackground)
			{
				this.drawIP.invertLut();
			}
			else if(this.showChoice == OUTLINES)
			{
				if(!this.inSituShow)
				{
					if(this.customLut == null)
					{
						this.makeCustomLut();
					}
					this.drawIP.setColorModel(this.customLut);
				}
				this.drawIP.setFont(new Font("SansSerif", Font.PLAIN, 9));
			}
			this.outlines.addSlice(null, this.drawIP);
			
			if(this.showChoice == ROI_MASKS || this.blackBackground)
			{
				this.drawIP.setColor(Color.black);
				this.drawIP.fill();
				this.drawIP.setColor(Color.white);
			}
			else
			{
				this.drawIP.setColor(Color.white);
				this.drawIP.fill();
				this.drawIP.setColor(Color.black);
			}
		}
		this.calibration = this.redirectImp != null ? this.redirectImp.getCalibration() : imp.getCalibration();
		
		if(this.rt == null)
		{
			this.rt = Analyzer.getResultsTable();
			this.analyzer = new Analyzer(imp);
		}
		else
		{
			this.analyzer = new Analyzer(imp, this.measurements, this.rt);
		}
		if(this.resetCounter && this.slice == 1)
		{
			if(!Analyzer.resetCounter())
			{
				return false;
			}
		}
		this.beginningCount = Analyzer.getCounter();
		
		byte[] pixels = null;
		if(ip instanceof ByteProcessor)
		{
			pixels = (byte[]) ip.getPixels();
		}
		if(this.r == null)
		{
			this.r = ip.getRoi();
			this.mask = ip.getMask();
			if(this.displaySummary)
			{
				if(this.mask != null)
				{
					this.totalArea = ImageStatistics.getStatistics(ip, AREA, this.calibration).area;
				}
				else
				{
					this.totalArea = this.r.width * this.calibration.pixelWidth * this.r.height * this.calibration.pixelHeight;
				}
			}
		}
		this.minX = this.r.x;
		this.maxX = this.r.x + this.r.width;
		this.minY = this.r.y;
		this.maxY = this.r.y + this.r.height;
		if(this.r.width < this.width || this.r.height < this.height || this.mask != null)
		{
			if(!this.eraseOutsideRoi(ip, this.r, this.mask))
			{
				return false;
			}
		}
		int offset;
		double value;
		int inc = Math.max(this.r.height / 25, 1);
		// int mi = 0;
		ImageWindow win = imp.getWindow();
		if(win != null)
		{
			win.running = true;
		}
		if(this.measurements == 0)
		{
			this.measurements = Analyzer.getMeasurements();
		}
		if(this.showChoice == ELLIPSES)
		{
			this.measurements |= ELLIPSE;
		}
		this.measurements &= ~LIMIT; // ignore "Limit to Threshold"
		this.roiNeedsImage = (this.measurements & PERIMETER) != 0 || (this.measurements & SHAPE_DESCRIPTORS) != 0 || (this.measurements & FERET) != 0;
		this.particleCount = 0;
		this.wand = new Wand(ip);
		this.pf = new PolygonFiller();
		if(this.floodFill)
		{
			ImageProcessor ipf = ip.duplicate();
			ipf.setValue(this.fillColor);
			this.ff = new FloodFiller(ipf);
		}
		this.roiType = Wand.allPoints() ? Roi.FREEROI : Roi.TRACED_ROI;
		
		for (int y = this.r.y; y < (this.r.y + this.r.height); y++)
		{
			offset = y * this.width;
			for (int x = this.r.x; x < (this.r.x + this.r.width); x++)
			{
				if(pixels != null)
				{
					value = pixels[offset + x] & 255;
				}
				else if(this.imageType == SHORT)
				{
					value = ip.getPixel(x, y);
				}
				else
				{
					value = ip.getPixelValue(x, y);
				}
				if(value >= this.level1 && value <= this.level2)
				{
					this.analyzeParticle(x, y, imp, ip);
				}
			}
			if(this.showProgress && ((y % inc) == 0))
			{
				IJ.showProgress((double) (y - this.r.y) / this.r.height);
			}
			if(win != null)
			{
				this.canceled = !win.running;
			}
			if(this.canceled)
			{
				Macro.abort();
				break;
			}
		}
		if(this.showProgress)
		{
			IJ.showProgress(1.0);
		}
		if(this.showResults)
		{
			this.rt.updateResults();
		}
		imp.killRoi();
		ip.resetRoi();
		ip.reset();
		if(this.displaySummary && IJ.getInstance() != null)
		{
			this.updateSliceSummary();
		}
		if(this.addToManager && this.roiManager != null)
		{
			this.roiManager.setEditMode(imp, true);
		}
		this.maxParticleCount = (this.particleCount > this.maxParticleCount) ? this.particleCount : this.maxParticleCount;
		// this.totalCount += this.particleCount;
		if(!this.canceled)
		{
			this.showResults();
		}
		return true;
	}
	
	void updateSliceSummary()
	{
		int slices = this.imp.getStackSize();
		float[] areas = this.rt.getColumn(ResultsTable.AREA);
		String label = this.imp.getTitle();
		if(slices > 1)
		{
			label = this.imp.getStack().getShortSliceLabel(this.slice);
			label = label != null && !label.equals("") ? label : "" + this.slice;
		}
		String aLine = null;
		if(areas == null)
		{
			return;
		}
		double sum = 0.0;
		int start = areas.length - this.particleCount;
		if(start < 0)
		{
			return;
		}
		for (int i = start; i < areas.length; i++)
		{
			sum += areas[i];
		}
		int places = Analyzer.getPrecision();
		// Calibration cal = imp.getCalibration();
		String total = "\t" + ResultsTable.d2s(sum, places);
		String average = "\t" + ResultsTable.d2s(sum / this.particleCount, places);
		String fraction = "\t" + ResultsTable.d2s(sum * 100.0 / this.totalArea, 1);
		aLine = label + "\t" + this.particleCount + total + average + fraction;
		aLine = this.addMeans(aLine, start);
		if(slices == 1)
		{
			Frame frame = WindowManager.getFrame("Summary");
			if(frame != null && (frame instanceof TextWindow) && this.summaryHdr.equals(prevHdr))
			{
				this.tw = (TextWindow) frame;
			}
		}
		if(this.tw == null)
		{
			String title = slices == 1 ? "Summary" : "Summary of " + this.imp.getTitle();
			this.tw = new TextWindow(title, this.summaryHdr, aLine, 450, 300);
			prevHdr = this.summaryHdr;
		}
		else
		{
			this.tw.append(aLine);
		}
	}
	
	String addMeans(String line, int start)
	{
		if((this.measurements & MEAN) != 0)
		{
			line = this.addMean(ResultsTable.MEAN, line, start);
		}
		if((this.measurements & MODE) != 0)
		{
			line = this.addMean(ResultsTable.MODE, line, start);
		}
		if((this.measurements & PERIMETER) != 0)
		{
			line = this.addMean(ResultsTable.PERIMETER, line, start);
		}
		if((this.measurements & ELLIPSE) != 0)
		{
			line = this.addMean(ResultsTable.MAJOR, line, start);
			line = this.addMean(ResultsTable.MINOR, line, start);
			line = this.addMean(ResultsTable.ANGLE, line, start);
		}
		if((this.measurements & SHAPE_DESCRIPTORS) != 0)
		{
			line = this.addMean(ResultsTable.CIRCULARITY, line, start);
			line = this.addMean(ResultsTable.SOLIDITY, line, start);
		}
		if((this.measurements & FERET) != 0)
		{
			line = this.addMean(ResultsTable.FERET, line, start);
			line = this.addMean(ResultsTable.FERET_X, line, start);
			line = this.addMean(ResultsTable.FERET_Y, line, start);
			line = this.addMean(ResultsTable.FERET_ANGLE, line, start);
			line = this.addMean(ResultsTable.MIN_FERET, line, start);
		}
		if((this.measurements & INTEGRATED_DENSITY) != 0)
		{
			line = this.addMean(ResultsTable.INTEGRATED_DENSITY, line, start);
		}
		if((this.measurements & MEDIAN) != 0)
		{
			line = this.addMean(ResultsTable.MEDIAN, line, start);
		}
		if((this.measurements & SKEWNESS) != 0)
		{
			line = this.addMean(ResultsTable.SKEWNESS, line, start);
		}
		if((this.measurements & KURTOSIS) != 0)
		{
			line = this.addMean(ResultsTable.KURTOSIS, line, start);
		}
		return line;
	}
	
	private String addMean(int column, String line, int start)
	{
		float[] c = column >= 0 ? this.rt.getColumn(column) : null;
		if(c != null)
		{
			ImageProcessor ip = new FloatProcessor(c.length, 1, c, null);
			// if (ip==null) return line;
			ip.setRoi(start, 0, ip.getWidth() - start, 1);
			ip = ip.crop();
			ImageStatistics stats = new FloatStatistics(ip);
			// if (stats==null)
			// return line;
			line += this.n(stats.mean);
		}
		else
		{
			line += "-\t";
		}
		this.summaryHdr += "\t" + this.rt.getColumnHeading(column);
		return line;
	}
	
	String n(double n)
	{
		String s;
		if(Math.round(n) == n)
		{
			s = ResultsTable.d2s(n, 0);
		}
		else
		{
			s = ResultsTable.d2s(n, Analyzer.getPrecision());
		}
		return "\t" + s;
	}
	
	boolean eraseOutsideRoi(ImageProcessor ip, Rectangle r, ImageProcessor mask)
	{
		int width = ip.getWidth();
		int height = ip.getHeight();
		ip.setRoi(r);
		if(this.excludeEdgeParticles && this.polygon != null)
		{
			ImageStatistics stats = ImageStatistics.getStatistics(ip, MIN_MAX, null);
			if(this.fillColor >= stats.min && this.fillColor <= stats.max)
			{
				double replaceColor = this.level1 - 1.0;
				if(replaceColor < 0.0 || replaceColor == this.fillColor)
				{
					replaceColor = this.level2 + 1.0;
					int maxColor = this.imageType == BYTE ? 255 : 65535;
					if(replaceColor > maxColor || replaceColor == this.fillColor)
					{
						IJ.error("Particle Analyzer", "Unable to remove edge particles");
						return false;
					}
				}
				for (int y = this.minY; y < this.maxY; y++)
				{
					for (int x = this.minX; x < this.maxX; x++)
					{
						int v = ip.getPixel(x, y);
						if(v == this.fillColor)
						{
							ip.putPixel(x, y, (int) replaceColor);
						}
					}
				}
			}
		}
		ip.setValue(this.fillColor);
		if(mask != null)
		{
			mask = mask.duplicate();
			mask.invert();
			ip.fill(mask);
		}
		ip.setRoi(0, 0, r.x, height);
		ip.fill();
		ip.setRoi(r.x, 0, r.width, r.y);
		ip.fill();
		ip.setRoi(r.x, r.y + r.height, r.width, height - (r.y + r.height));
		ip.fill();
		ip.setRoi(r.x + r.width, 0, width - (r.x + r.width), height);
		ip.fill();
		ip.resetRoi();
		// IJ.log("erase: "+fillColor+"	"+level1+"	"+level2+"	"+excludeEdgeParticles);
		// (new ImagePlus("ip2", ip.duplicate())).show();
		return true;
	}
	
	boolean setThresholdLevels(ImagePlus imp, ImageProcessor ip)
	{
		double t1 = ip.getMinThreshold();
		double t2 = ip.getMaxThreshold();
		boolean invertedLut = imp.isInvertedLut();
		// boolean byteImage = ip instanceof ByteProcessor;
		if(ip instanceof ShortProcessor)
		{
			this.imageType = SHORT;
		}
		else if(ip instanceof FloatProcessor)
		{
			this.imageType = FLOAT;
		}
		else
		{
			this.imageType = BYTE;
		}
		if(t1 == ImageProcessor.NO_THRESHOLD)
		{
			ImageStatistics stats = imp.getStatistics();
			if(this.imageType != BYTE || (stats.histogram[0] + stats.histogram[255] != stats.pixelCount))
			{
				IJ.error("Particle Analyzer", "A thresholded image or 8-bit binary image is\n" + "required. Threshold levels can be set using\n" + "the Image->Adjust->Threshold tool.");
				this.canceled = true;
				return false;
			}
			if(invertedLut)
			{
				this.level1 = 255;
				this.level2 = 255;
				this.fillColor = 64;
			}
			else
			{
				this.level1 = 0;
				this.level2 = 0;
				this.fillColor = 192;
			}
		}
		else
		{
			this.level1 = t1;
			this.level2 = t2;
			if(this.imageType == BYTE)
			{
				if(this.level1 > 0)
				{
					this.fillColor = 0;
				}
				else if(this.level2 < 255)
				{
					this.fillColor = 255;
				}
			}
			else if(this.imageType == SHORT)
			{
				if(this.level1 > 0)
				{
					this.fillColor = 0;
				}
				else if(this.level2 < 65535)
				{
					this.fillColor = 65535;
				}
			}
			else if(this.imageType == FLOAT)
			{
				this.fillColor = -Float.MAX_VALUE;
			}
			else
			{
				return false;
			}
		}
		this.imageType2 = this.imageType;
		if(this.redirectIP != null)
		{
			if(this.redirectIP instanceof ShortProcessor)
			{
				this.imageType2 = SHORT;
			}
			else if(this.redirectIP instanceof FloatProcessor)
			{
				this.imageType2 = FLOAT;
			}
			else if(this.redirectIP instanceof ColorProcessor)
			{
				this.imageType2 = RGB;
			}
			else
			{
				this.imageType2 = BYTE;
			}
		}
		return true;
	}
	
	int counter = 0;
	
	void analyzeParticle(int x, int y, ImagePlus imp, ImageProcessor ip)
	{
		// Wand wand = new Wand(ip);
		ImageProcessor ip2 = this.redirectIP != null ? this.redirectIP : ip;
		this.wand.autoOutline(x, y, this.level1, this.level2, this.wandMode);
		if(this.wand.npoints == 0)
		{
			IJ.log("wand error: " + x + " " + y);
			return;
		}
		Roi roi = new PolygonRoi(this.wand.xpoints, this.wand.ypoints, this.wand.npoints, this.roiType);
		Rectangle r = roi.getBounds();
		if(r.width > 1 && r.height > 1)
		{
			PolygonRoi proi = (PolygonRoi) roi;
			this.pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(), proi.getNCoordinates());
			ip2.setMask(this.pf.getMask(r.width, r.height));
			if(this.floodFill)
			{
				this.ff.particleAnalyzerFill(x, y, this.level1, this.level2, ip2.getMask(), r);
			}
		}
		ip2.setRoi(r);
		ip.setValue(this.fillColor);
		ImageStatistics stats = this.getStatistics(ip2, this.measurements, this.calibration);
		boolean include = true;
		if(this.excludeEdgeParticles)
		{
			if(r.x == this.minX || r.y == this.minY || r.x + r.width == this.maxX || r.y + r.height == this.maxY)
			{
				include = false;
			}
			if(this.polygon != null)
			{
				Rectangle bounds = roi.getBounds();
				int x1 = bounds.x + this.wand.xpoints[this.wand.npoints - 1];
				int y1 = bounds.y + this.wand.ypoints[this.wand.npoints - 1];
				int x2, y2;
				for (int i = 0; i < this.wand.npoints; i++)
				{
					x2 = bounds.x + this.wand.xpoints[i];
					y2 = bounds.y + this.wand.ypoints[i];
					if(!this.polygon.contains(x2, y2))
					{
						include = false;
						break;
					}
					if((x1 == x2 && ip.getPixel(x1, y1 - 1) == this.fillColor) || (y1 == y2 && ip.getPixel(x1 - 1, y1) == this.fillColor))
					{
						include = false;
						break;
					}
					x1 = x2;
					y1 = y2;
				}
			}
		}
		ImageProcessor mask = ip2.getMask();
		if(this.minCircularity > 0.0 || this.maxCircularity < 1.0)
		{
			double perimeter = roi.getLength();
			double circularity = perimeter == 0.0 ? 0.0 : 4.0 * Math.PI * (stats.pixelCount / (perimeter * perimeter));
			if(circularity > 1.0)
			{
				circularity = 1.0;
			}
			// IJ.log(circularity+"	"+perimeter+"  "+stats.area);
			if(circularity < this.minCircularity || circularity > this.maxCircularity)
			{
				include = false;
			}
		}
		if(stats.pixelCount >= this.minSize && stats.pixelCount <= this.maxSize && include)
		{
			this.particleCount++;
			if(this.roiNeedsImage)
			{
				roi.setImage(imp);
			}
			stats.xstart = x;
			stats.ystart = y;
			this.saveResults(stats, roi);
			this.foundRois.add((Roi) roi.clone());
			// foundRois.add(roi);
			if(this.showChoice != NOTHING)
			{
				this.drawParticle(this.drawIP, roi, stats, mask);
			}
		}
		if(this.redirectIP != null)
		{
			ip.setRoi(r);
		}
		ip.fill(mask);
	}
	
	ImageStatistics getStatistics(ImageProcessor ip, int mOptions, Calibration cal)
	{
		switch (this.imageType2)
		{
			case BYTE:
				return new ByteStatistics(ip, mOptions, cal);
			case SHORT:
				return new ShortStatistics(ip, mOptions, cal);
			case FLOAT:
				return new FloatStatistics(ip, mOptions, cal);
			case RGB:
				return new ColorStatistics(ip, mOptions, cal);
			default:
				return null;
		}
	}
	
	/**
	 * Saves statistics for one particle in a results table. This is a method subclasses may want to override.
	 */
	protected void saveResults(ImageStatistics stats, Roi roi)
	{
		this.analyzer.saveResults(stats, roi);
		if(this.recordStarts)
		{
			this.rt.addValue("XStart", stats.xstart);
			this.rt.addValue("YStart", stats.ystart);
		}
		if(this.addToManager)
		{
			if(this.roiManager == null)
			{
				if(Macro.getOptions() != null && Interpreter.isBatchMode())
				{
					this.roiManager = Interpreter.getBatchModeRoiManager();
				}
				if(this.roiManager == null)
				{
					Frame frame = WindowManager.getFrame("ROI Manager");
					if(frame == null)
					{
						IJ.run("ROI Manager...");
					}
					frame = WindowManager.getFrame("ROI Manager");
					if(frame == null || !(frame instanceof RoiManager))
					{
						this.addToManager = false;
						return;
					}
					this.roiManager = (RoiManager) frame;
				}
				if(this.resetCounter)
				{
					this.roiManager.runCommand("reset");
				}
			}
			this.roiManager.add(this.imp, roi, this.rt.getCounter());
		}
		if(this.showResults)
		{
			this.rt.addResults();
		}
	}
	
	/**
	 * Draws a selected particle in a separate image. This is another method subclasses may want to override.
	 */
	protected void drawParticle(ImageProcessor drawIP, Roi roi, ImageStatistics stats, ImageProcessor mask)
	{
		switch (this.showChoice)
		{
			case MASKS:
				this.drawFilledParticle(drawIP, roi, mask);
				break;
			case OUTLINES:
			case OVERLAY_OUTLINES:
			case OVERLAY_MASKS:
				this.drawOutline(drawIP, roi, this.rt.getCounter());
				break;
			case ELLIPSES:
				this.drawEllipse(drawIP, stats, this.rt.getCounter());
				break;
			case ROI_MASKS:
				this.drawRoiFilledParticle(drawIP, roi, mask, this.rt.getCounter());
				break;
			default:
		}
	}
	
	void drawFilledParticle(ImageProcessor ip, Roi roi, ImageProcessor mask)
	{
		// IJ.write(roi.getBounds()+" "+mask.length);
		ip.setRoi(roi.getBounds());
		ip.fill(mask);
	}
	
	void drawOutline(ImageProcessor ip, Roi roi, int count)
	{
		if(this.showChoice == OVERLAY_OUTLINES || this.showChoice == OVERLAY_MASKS)
		{
			if(this.overlay == null)
			{
				this.overlay = new Overlay();
				this.overlay.drawLabels(true);
			}
			roi.setStrokeColor(Color.cyan);
			if(this.showChoice == OVERLAY_MASKS)
			{
				roi.setFillColor(Color.cyan);
			}
			this.overlay.add((Roi) roi.clone());
		}
		else
		{
			Rectangle r = roi.getBounds();
			int nPoints = ((PolygonRoi) roi).getNCoordinates();
			int[] xp = ((PolygonRoi) roi).getXCoordinates();
			int[] yp = ((PolygonRoi) roi).getYCoordinates();
			int x = r.x, y = r.y;
			if(!this.inSituShow)
			{
				ip.setValue(0.0);
			}
			ip.moveTo(x + xp[0], y + yp[0]);
			for (int i = 1; i < nPoints; i++)
			{
				ip.lineTo(x + xp[i], y + yp[i]);
			}
			ip.lineTo(x + xp[0], y + yp[0]);
			String s = ResultsTable.d2s(count, 0);
			ip.moveTo(r.x + r.width / 2 - ip.getStringWidth(s) / 2, r.y + r.height / 2 + 4);
			if(!this.inSituShow)
			{
				ip.setValue(1.0);
			}
			ip.drawString(s);
		}
	}
	
	void drawEllipse(ImageProcessor ip, ImageStatistics stats, int count)
	{
		stats.drawEllipse(ip);
	}
	
	void drawRoiFilledParticle(ImageProcessor ip, Roi roi, ImageProcessor mask, int count)
	{
		int grayLevel = (count < 65535) ? count : 65535;
		ip.setValue(grayLevel);
		ip.setRoi(roi.getBounds());
		ip.fill(mask);
	}
	
	void showResults()
	{
		int count = this.rt.getCounter();
		if(count == 0)
		{
			return;
		}
		boolean lastSlice = !this.processStack || this.slice == this.imp.getStackSize();
		if((this.showChoice == OVERLAY_OUTLINES || this.showChoice == OVERLAY_MASKS) && this.slice == 1)
		{
			this.imp.setOverlay(this.overlay);
			//this.imp.show();
			Logs.log("Overlay Made", 1, this);
			
			// Make output figure
			this.outlineImage = this.imp.duplicate();
		}
		else if(this.outlines != null && lastSlice)
		{
			String title = this.imp != null ? this.imp.getTitle() : "Outlines";
			String prefix;
			if(this.showChoice == MASKS)
			{
				prefix = "Mask of ";
			}
			else if(this.showChoice == ROI_MASKS)
			{
				prefix = "Count Masks of ";
			}
			else
			{
				prefix = "Drawing of ";
			}
			this.outlines.update(this.drawIP);
			this.outputImage = new ImagePlus(prefix + title, this.outlines);

			// Make output figure
			if(this.inSituShow)
			{
				if(this.imp.getStackSize() == 1)
				{
					Undo.setup(Undo.TRANSFORM, this.imp);
				}
				this.imp.setStack(null, this.outputImage.getStack());
			}
			else if(!this.hideOutputImage)
			{
				this.outputImage.show();
			}
		}
		if(this.showResults && !this.processStack)
		{
			TextPanel tp = IJ.getTextPanel();
			if(this.beginningCount > 0 && tp != null && tp.getLineCount() != count)
			{
				this.rt.show("Results");
			}
			Analyzer.firstParticle = this.beginningCount;
			Analyzer.lastParticle = Analyzer.getCounter() - 1;
		}
		else
		{
			Analyzer.firstParticle = Analyzer.lastParticle = 0;
		}
	}
	
	/**
	 * Returns the "Outlines", "Masks", "Elipses" or "Count Masks" image, or null if "Nothing" is selected in the "Show:" menu.
	 */
	public ImagePlus getOutputImage()
	{
		return this.outlineImage;
		//return this.outputImage;
	}
	
	public ArrayList<Roi> getFoundRois()
	{
		return this.foundRois;
	}
	
	/** Set 'hideOutputImage' true to not display the "Show:" image. */
	public void setHideOutputImage(boolean hideOutputImage)
	{
		this.hideOutputImage = hideOutputImage;
	}
	
	int getColumnID(String name)
	{
		int id = this.rt.getFreeColumn(name);
		if(id == ResultsTable.COLUMN_IN_USE)
		{
			id = this.rt.getColumnIndex(name);
		}
		return id;
	}
	
	void makeCustomLut()
	{
		IndexColorModel cm = (IndexColorModel) LookUpTable.createGrayscaleColorModel(false);
		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];
		cm.getReds(reds);
		cm.getGreens(greens);
		cm.getBlues(blues);
		reds[1] = (byte) 255;
		greens[1] = (byte) 0;
		blues[1] = (byte) 0;
		this.customLut = new IndexColorModel(8, 256, reds, greens, blues);
	}
	
	/** Called once when ImageJ quits. */
	public static void savePreferences(Properties prefs)
	{
		prefs.put(OPTIONS, Integer.toString(staticOptions));
	}
	
}

package plugins.imageAligner;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;

public class ImageAligner implements PlugInController, ActionListener {
	
	// Variables of the class
	public List<JEXEntry> entries;
	public TypeName typeName;
	public JEXData imageData = null;
	public int image1Index = 0;
	public int image2Index = 0;
	public ImagePlus image1;
	public ImagePlus image2;
	public List<JPanel> internalPanelList = new ArrayList<JPanel>(0);
	public boolean valid = false;
	
	// Variables of the GUI
	private PlugIn dialog = new PlugIn(this);
	public JPanel desktop = new JPanel();
	public JPanel buttonPane = new JPanel();
	// public JButton validate = new JButton("Alignement done");
	// public JButton cancel = new JButton("Cancel");
	// public JCheckBox horizontal = new JCheckBox("Horizontal",true);
	
	public JRadioButton horizontal = new JRadioButton("Same row", true);
	public JRadioButton vertical = new JRadioButton("Same column", true);
	public JLabel dxLab = new JLabel("");
	public JLabel dyLab = new JLabel("");
	public JLabel dx = new JLabel("0");
	public JLabel dy = new JLabel("0");
	public JButton ok = new JButton("OK"), cancel = new JButton("CANCEL");
	// public JTextField dx = new JTextField("0",4);
	// public JTextField dy = new JTextField("0",4);
	ButtonGroup group = new ButtonGroup();
	
	public JScrollPane workScroll = new JScrollPane();
	public JLayeredPane workInternalPane = new JLayeredPane();
	public JScrollPane workZoomScroll = new JScrollPane();
	public JLayeredPane workInternalZoomPane = new JLayeredPane();
	public JScrollPane workZoomMaxScroll = new JScrollPane();
	public JLayeredPane workInternalZoomMaxPane = new JLayeredPane();
	
	public ImageAlignerDisplay alignerGeneral;
	public ImageAlignerDisplay alignerZoom;
	public ImageAlignerDisplay alignerZoomMax;
	
	public ImageAligner()
	{
		// initialize gui parts
		this.initializeWaitingForFiles();
		
		// Trigger any needed refreshing
		
		// set visibile
		this.setVisible(true);
		
		// Make appropriate connections
	}
	
	public void setVisible(boolean visible)
	{
		this.dialog.setVisible(visible);
	}
	
	public ImageAligner(TreeSet<JEXEntry> entries, TypeName tn)
	{
		this();
		this.entries = new Vector<JEXEntry>(entries);
		this.typeName = tn;
		this.load();
	}
	
	public void start(String path1, String path2)
	{
		// pass the arguments
		this.image1 = new ImagePlus(path1);
		this.image2 = new ImagePlus(path2);
		
		// image1.show();
		// image2.show();
		
		// initialize the windows
		this.reset();
		this.initializeMain();
		this.initialize();
		this.refresh();
	}
	
	public void load()
	{
		this.imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.typeName, this.entries.get(0));
		this.load(this.imageData);
	}
	
	/**
	 * Load files from an instace of iimageset
	 * 
	 * @param images
	 */
	public void load(JEXData imageData)
	{
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return;
		}
		List<String> imageList = ImageReader.readObjectToImagePathList(imageData);
		File[] files = new File[imageList.size()];
		for (int i = 0, len = imageList.size(); i < len; i++)
		{
			files[i] = new File(imageList.get(i));
		}
		this.load(files);
	}
	
	/**
	 * Load files from a file array
	 * 
	 * @param files
	 */
	public void load(File[] files)
	{
		File file1 = null;
		File file2 = null;
		File f = (File) JOptionPane.showInputDialog(this.dialog, "Choose the first image:\n", "Select images to align", JOptionPane.PLAIN_MESSAGE, null, files, files[0]);
		// If a string was returned, say so.
		if(f == null)
		{
			return;
		}
		file1 = f;
		
		int i = 0;
		for (File temp : files)
		{
			if(file1 == temp)
			{
				break;
			}
			i = i + 1;
		}
		this.image1Index = i;
		
		f = (File) JOptionPane.showInputDialog(this.dialog, "Choose the second image:\n", "Select images to align", JOptionPane.PLAIN_MESSAGE, null, files, files[1]);
		
		// If a string was returned, say so.
		if(f != null)
		{
			file2 = f;
		}
		
		i = 0;
		for (File temp : files)
		{
			if(file2 == temp)
			{
				break;
			}
			i = i + 1;
		}
		this.image2Index = i;
		
		this.start(file1.getPath(), file2.getPath());
	}
	
	// ----------- START AND INITIALIZE
	public void reset()
	{
		this.workInternalPane.removeAll();
		this.workInternalZoomPane.removeAll();
		this.workInternalZoomMaxPane.removeAll();
		this.desktop.removeAll();
		this.dialog.getContentPane().removeAll();
	}
	
	private void initializeWaitingForFiles()
	{
		this.dialog.getContentPane().removeAll();
		this.dialog.setBounds(new Rectangle(0, 0, 150, 100));
		this.dialog.setBackground(Color.white);
		this.dialog.setLayout(new BorderLayout());
		
		JPanel waitPanel = new JPanel();
		waitPanel.setBackground(Color.LIGHT_GRAY);
		waitPanel.add(new JLabel("Waiting for files..."));
		this.dialog.getContentPane().add(waitPanel, BorderLayout.CENTER);
	}
	
	private void initializeMain()
	{
		this.dialog.getContentPane().removeAll();
		this.dialog.setBounds(new Rectangle(50, 50, 700, 700));
		this.dialog.setBackground(Color.white);
		this.setVisible(true);
		
		this.buttonPane.setLayout(new BoxLayout(this.buttonPane, BoxLayout.LINE_AXIS));
		// buttonPane.add(validate) ;
		// buttonPane.add(cancel) ;
		// buttonPane.add(horizontal) ;
		// validate.addActionListener(this);
		// cancel.addActionListener(this);
		
		this.ok.addActionListener(this);
		this.cancel.addActionListener(this);
		JPanel okPanel = new JPanel();
		okPanel.setBackground(Color.WHITE);
		okPanel.setLayout(new BoxLayout(okPanel, BoxLayout.Y_AXIS));
		okPanel.add(this.ok);
		okPanel.add(this.cancel);
		okPanel.setMaximumSize(new Dimension(50, Integer.MAX_VALUE));
		
		this.group.add(this.horizontal);
		this.group.add(this.vertical);
		this.horizontal.addActionListener(this);
		this.vertical.addActionListener(this);
		JPanel radioPane = new JPanel();
		radioPane.setBackground(Color.WHITE);
		radioPane.setLayout(new BoxLayout(radioPane, BoxLayout.Y_AXIS));
		radioPane.add(this.horizontal);
		radioPane.add(this.vertical);
		this.horizontal.setSelected(true);
		
		JPanel hPane = new JPanel();
		hPane.setBackground(Color.WHITE);
		hPane.setLayout(new BoxLayout(hPane, BoxLayout.Y_AXIS));
		hPane.add(this.dxLab);
		hPane.add(Box.createVerticalStrut(20));
		hPane.add(this.dyLab);
		this.dxLab.setFont(FontUtility.boldFont);
		this.dyLab.setFont(FontUtility.boldFont);
		this.dxLab.setText("Horizontal spacing");
		this.dyLab.setText("Vertical drift");
		
		JPanel vPane = new JPanel();
		vPane.setBackground(Color.WHITE);
		vPane.setLayout(new BoxLayout(vPane, BoxLayout.Y_AXIS));
		vPane.add(this.dx);
		vPane.add(Box.createVerticalStrut(20));
		vPane.add(this.dy);
		
		this.buttonPane.add(radioPane);
		this.buttonPane.add(Box.createHorizontalStrut(20));
		this.buttonPane.add(hPane);
		this.buttonPane.add(Box.createHorizontalStrut(20));
		this.buttonPane.add(vPane);
		this.buttonPane.add(Box.createHorizontalStrut(20));
		this.buttonPane.add(okPanel);
		
		this.desktop.setLayout(null);
		this.desktop.setPreferredSize(new Dimension(this.dialog.getWidth(), this.dialog.getHeight()));
		this.desktop.setMaximumSize(new Dimension(this.dialog.getWidth(), this.dialog.getHeight()));
		this.desktop.setMinimumSize(new Dimension(this.dialog.getWidth(), this.dialog.getHeight()));
		this.desktop.setBackground(Color.WHITE);
		
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this.desktop, BorderLayout.CENTER);
		this.dialog.getContentPane().add(this.buttonPane, BorderLayout.PAGE_END);
		// this.setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
		// this.add(desktop);
		// this.add(buttonPane);
	}
	
	private void initialize()
	{
		this.dialog.getContentPane().removeAll();
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this.desktop, BorderLayout.CENTER);
		this.dialog.getContentPane().add(this.buttonPane, BorderLayout.PAGE_END);
		
		int width = 640;
		int imageWidth = this.image1.getWidth();
		int imageHeight = this.image1.getHeight();
		int height = (int) (((float) width / (float) imageWidth) * (imageHeight));
		
		this.workInternalPane.setLayout(null);
		this.workInternalPane.setBounds(new Rectangle(0, 0, width, height));
		this.workScroll.setBounds(new Rectangle(0, 0, width, height));
		this.workInternalPane.add(this.workScroll);
		
		this.workInternalZoomPane.setLayout(null);
		this.workInternalZoomPane.setBounds(new Rectangle(width, 0, width + 300, height));
		this.workZoomScroll.setBounds(new Rectangle(0, 0, 300, height));
		this.workInternalZoomPane.add(this.workZoomScroll);
		
		this.workInternalZoomMaxPane.setLayout(null);
		this.workInternalZoomMaxPane.setBounds(new Rectangle(width + 300, 0, width + 600, height));
		this.workZoomMaxScroll.setBounds(new Rectangle(0, 0, 300, height));
		this.workInternalZoomMaxPane.add(this.workZoomMaxScroll);
		
		this.dialog.setBounds(new Rectangle(10, 50, 600 + width, 70 + height));
		this.desktop.setPreferredSize(new Dimension(this.dialog.getWidth(), this.dialog.getHeight()));
		this.desktop.setMaximumSize(new Dimension(this.dialog.getWidth(), this.dialog.getHeight()));
		this.desktop.setMinimumSize(new Dimension(this.dialog.getWidth(), this.dialog.getHeight()));
		this.desktop.add(this.workInternalPane);
		this.desktop.add(this.workInternalZoomPane);
		this.desktop.add(this.workInternalZoomMaxPane);
		
		this.alignerGeneral = new ImageAlignerDisplay(this, this.workScroll, new Point(0, 0), 1.0, width, height);
		this.alignerGeneral.setCenterable(false);
		this.alignerZoom = new ImageAlignerDisplay(this, this.workZoomScroll, new Point(0, 0), -1, 300, height);
		this.alignerZoomMax = new ImageAlignerDisplay(this, this.workZoomMaxScroll, new Point(0, 0), 12, 300, height);
		this.internalPanelList.add(this.alignerGeneral);
		this.internalPanelList.add(this.alignerZoom);
		this.internalPanelList.add(this.alignerZoomMax);
		
		// this.pack();
		
		this.workInternalPane.updateUI();
		this.workInternalZoomPane.updateUI();
		this.workInternalZoomMaxPane.updateUI();
		this.desktop.updateUI();
		this.dialog.update(this.dialog.getGraphics());
	}
	
	public void refresh()
	{
		this.alignerGeneral.refresh();
		this.alignerZoom.refresh();
		this.alignerZoomMax.refresh();
		this.workInternalPane.updateUI();
		this.workInternalZoomPane.updateUI();
		this.workInternalZoomMaxPane.updateUI();
		this.dialog.update(this.dialog.getGraphics());
	}
	
	public JEXEntry getFirstEntry()
	{
		return this.entries.get(0);
	}
	
	// ------------ RESULT
	public ParameterSet getParameters()
	{
		ParameterSet parameters = new ParameterSet();
		if(this.horizontal.isSelected())
		{
			int valueX = this.alignerZoom.getDispX();
			int valueDY = this.alignerZoom.getDispY();
			parameters.addParameter(new Parameter("X-axis Displacement", "Lateral pixel distance from a column to another", "" + valueX));
			parameters.addParameter(new Parameter("Vertical Drift", "Vertical offset between two images in a same row", "" + valueDY));
		}
		else
		{
			int valueY = this.alignerZoom.getDispY();
			int valueDX = this.alignerZoom.getDispX();
			parameters.addParameter(new Parameter("Y-axis Displacement", "Vertical pixel distance from a row to another", "" + valueY));
			parameters.addParameter(new Parameter("Horizontal Drift", "Lateral offset between two images in a same row", "" + valueDX));
		}
		return parameters;
	}
	
	// ------------ EVENTS
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.horizontal)
		{
			this.dxLab.setText("Horizontal spacing");
			this.dyLab.setText("Vertical drift");
		}
		else if(e.getSource() == this.vertical)
		{
			this.dxLab.setText("Vertical spacing");
			this.dyLab.setText("Horizontal drift");
		}
		if(e.getSource() == this.ok)
		{
			this.saveSpacing();
			this.dialog.dispose();
		}
		else if(e.getSource() == this.cancel)
		{
			this.dialog.dispose();
		}
	}
	
	private void saveSpacing()
	{
		TypeName newValue = JEXStatics.jexManager.getNextAvailableTypeNameInEntries(new TypeName(JEXData.VALUE, "Image Alignment"), this.entries);
		TreeMap<JEXEntry,Set<JEXData>> dataMap = new TreeMap<JEXEntry,Set<JEXData>>();
		String alignment = "" + this.dx.getText() + "," + this.dy.getText() + "," + this.image1Index + "," + this.image2Index;
		;
		HashSet<JEXData> dataSet;
		JEXData valueData;
		for (JEXEntry e : this.entries)
		{
			valueData = ValueWriter.makeValueObject(newValue.getName(), alignment);
			dataSet = new HashSet<JEXData>();
			dataSet.add(valueData);
			dataMap.put(e, dataSet);
		}
		// JEXStatics.jexManager.addDataListToEntries(dataMap, true);
		JEXStatics.jexDBManager.saveDataListInEntries(dataMap, true);
	}
	
	public void receiveNewFocusPoint(Point p)
	{
		for (int i = 0, len = this.internalPanelList.size(); i < len; i++)
		{
			ImageAlignerDisplay newAligner = (ImageAlignerDisplay) this.internalPanelList.get(i);
			newAligner.setFocusPoint(p);
		}
	}
	
	public void cancelAllSelections()
	{
		for (int i = 0, len = this.internalPanelList.size(); i < len; i++)
		{
			ImageAlignerDisplay newAligner = (ImageAlignerDisplay) this.internalPanelList.get(i);
			newAligner.setSelected(false);
			newAligner.setFocusable(false);
		}
	}
	
	public void translate(int x, int y, double pixelPerPixel)
	{
		int locX = (int) (x * (pixelPerPixel));
		int locY = (int) (y * (pixelPerPixel));
		if(this.horizontal.isSelected())
		{
			this.dx.setText("" + locX);
			this.dy.setText("" + locY);
		}
		else
		{
			this.dx.setText("" + locY);
			this.dy.setText("" + locX);
		}
		
		for (int i = 0, len = this.internalPanelList.size(); i < len; i++)
		{
			ImageAlignerDisplay newAligner = (ImageAlignerDisplay) this.internalPanelList.get(i);
			newAligner.translateAbsolute(x, y, pixelPerPixel);
		}
	}
	
	// ------------ CLASSES
	class ImageContainer extends JComponent {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private BufferedImage image;
		
		public ImageContainer(Image image)
		{
			this.image = this.toBufferedImage(image);
			this.setBounds(new Rectangle(0, 0, this.image.getWidth(), this.image.getHeight()));
		}
		
		@Override
		public void paint(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g;
			// g2.drawImage(image, 0, 0,this);
			g2.drawImage(this.image, new AffineTransform(), this);
		}
		
		public BufferedImage toBufferedImage(Image i)
		{
			if(i instanceof BufferedImage)
			{
				return (BufferedImage) i;
			}
			
			BufferedImage bimage = null;
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			bimage = new BufferedImage(i.getWidth(null), i.getHeight(null), type);
			
			// Copy image to buffered image
			Graphics g = bimage.createGraphics();
			
			// Paint the image onto the buffered image
			g.drawImage(i, 0, 0, null);
			g.dispose();
			
			return bimage;
		}
		
		public void setOpacity(Rectangle seeThrough, int opacity)
		{
			final int[] submask = new int[seeThrough.width * seeThrough.height];
			for (int k = 0, K = submask.length; (k < K); k++)
			{
				submask[k] = opacity;
			}
			this.image.getAlphaRaster().setPixels(seeThrough.x - this.getLocation().x, seeThrough.y - this.getLocation().y, seeThrough.width, seeThrough.height, submask);
		} /* end setOpacity */
	}
	
	class ImageAlignerDisplay extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// class variables
		private Dimension playFieldSize = null;
		public Image imageThumbFinal1;
		private BufferedImage bimageTransparent2;
		
		// location variables
		private boolean isCenterable = true;
		private double zoom = 1.0;
		private Point mouse = null;
		private int DX = 0;
		private int DY = 0;
		private Point focusPoint = new Point(0, 0);
		private double pixelPerPixel = 1.0;
		// private Point pixelMoved = new Point(0,0);
		private boolean isSelected = false;
		
		// GUI variables
		private JScrollPane parentScrollPane = null;
		private ImageAligner mainPane = null;
		private BufferedImageOp identityOp;
		private JLabel scaleLabel = new JLabel();
		private Font font;
		private String scaleStr = "";
		
		// --------- MAIN FUNCTIONS
		public ImageAlignerDisplay(ImageAligner mainPane, JScrollPane parent, Point focusPoint, double zoom, int minWidth, int minHeight)
		{
			this.mouse = focusPoint;
			this.zoom = zoom;
			this.parentScrollPane = parent;
			this.playFieldSize = new Dimension(minWidth, minHeight);
			this.focusPoint = focusPoint;
			this.mainPane = mainPane;
			this.identityOp = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			this.addKeyListener(this);
			
			this.initialize();
		}
		
		private void initialize()
		{
			this.font = new Font("sans serif", Font.PLAIN, 11);
			this.setBounds(new Rectangle(0, 0, this.parentScrollPane.getWidth(), this.parentScrollPane.getHeight()));
			this.parentScrollPane.setViewportView(this);
			
			// System.out.println("width ..."+parentScrollPane.getWidth()+" and other width "+this.getWidth());
			
			// creating image 1
			ImageProcessor imp = ImageAligner.this.image1.getProcessor();
			if(this.zoom != -1)
			{
				imp = imp.resize((int) (this.playFieldSize.getWidth() * this.zoom));
				this.pixelPerPixel = (ImageAligner.this.image1.getWidth()) / (this.playFieldSize.getWidth() * this.zoom);
			}
			ImagePlus imageThumb1 = new ImagePlus("icon 1", imp);
			this.imageThumbFinal1 = imageThumb1.getImage();
			
			// creating image 2
			imp = ImageAligner.this.image2.getProcessor();
			if(this.zoom != -1)
			{
				imp = imp.resize((int) (this.playFieldSize.getWidth() * this.zoom));
			}
			ImagePlus imageThumb2 = new ImagePlus("icon 2", imp);
			Image imageThumbFinal2 = imageThumb2.getImage();
			
			BufferedImage bimage2 = this.toBufferedImage(imageThumbFinal2);
			this.bimageTransparent2 = this.createTransparent(bimage2, (float) 0.5);
			
			int newFocusX = (int) ((this.focusPoint.x) / this.pixelPerPixel);
			int newFocusY = (int) ((this.focusPoint.y) / this.pixelPerPixel);
			this.focusPoint.setLocation(newFocusX, newFocusY);
			this.paint(this.getGraphics());
			
			double scaleValue = 1 / this.pixelPerPixel;
			this.scaleStr = "Scale " + scaleValue;
			this.scaleLabel.setText(this.scaleStr);
			// translate(focusPoint.x, focusPoint.y);
		}
		
		public void setFocusPoint(int Xth, int Yth)
		{
			if(!this.isCenterable)
			{
				return;
			}
			int newFocusX = (int) ((-Xth) / this.pixelPerPixel);
			int newFocusY = (int) ((-Yth) / this.pixelPerPixel);
			this.focusPoint.setLocation(newFocusX, newFocusY);
			this.paint(this.getGraphics());
		}
		
		public void setFocusPoint(Point p)
		{
			if(!this.isCenterable)
			{
				return;
			}
			int newFocusX = (int) ((-p.x) / this.pixelPerPixel) + this.getWidth() / 2;
			int newFocusY = (int) ((-p.y) / this.pixelPerPixel) + this.getHeight() / 2;
			this.focusPoint.setLocation(newFocusX, newFocusY);
			this.paint(this.getGraphics());
		}
		
		public void setCenterable(boolean b)
		{
			this.isCenterable = b;
		}
		
		public void setSelected(boolean b)
		{
			this.isSelected = b;
		}
		
		public boolean getSelected()
		{
			boolean result = this.isSelected;
			return result;
		}
		
		public Point getAbsolutePoint(Point p)
		{
			int X = (int) ((p.x) * this.pixelPerPixel);
			int Y = (int) ((p.y) * this.pixelPerPixel);
			return new Point(X, Y);
		}
		
		public int getDispX()
		{
			int result = (int) ((this.DX) * this.pixelPerPixel);
			return result;
		}
		
		public int getDispY()
		{
			int result = (int) ((this.DY) * this.pixelPerPixel);
			return result;
		}
		
		private BufferedImage toBufferedImage(Image i)
		{
			if(i instanceof BufferedImage)
			{
				return (BufferedImage) i;
			}
			
			BufferedImage bimage = null;
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			bimage = new BufferedImage(i.getWidth(null), i.getHeight(null), type);
			
			// Copy image to buffered image
			Graphics g = bimage.createGraphics();
			
			// Paint the image onto the buffered image
			g.drawImage(i, 0, 0, null);
			g.dispose();
			
			return bimage;
		}
		
		private BufferedImage createTransparent(BufferedImage src, float alpha)
		{
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_ARGB;
			BufferedImage result = new BufferedImage(src.getWidth(null), src.getHeight(null), type);
			
			// Copy image to buffered image
			Graphics2D g2 = result.createGraphics();
			
			// Create the transparency
			int rule = AlphaComposite.SRC_OVER;
			AlphaComposite ac = AlphaComposite.getInstance(rule, alpha);
			g2.setComposite(ac);
			g2.drawImage(src, null, 0, 0);
			g2.dispose();
			
			return result;
		}
		
		// ------- GRAPHICS
		@Override
		public void paint(Graphics g)
		{
			if(g == null)
			{
				return;
			}
			// Dimension dimension = getSize();
			// int width = dimension.width;
			// int height = dimension.height;
			Rectangle viewRect = this.parentScrollPane.getViewport().getViewRect();
			g.setColor(Color.white);
			g.fillRect(viewRect.x, viewRect.y, viewRect.width - 1, viewRect.height - 1);
			
			Graphics2D g2 = (Graphics2D) g;
			
			g2.drawImage(this.imageThumbFinal1, this.focusPoint.x, this.focusPoint.y, this);
			
			g2.drawImage(this.bimageTransparent2, this.identityOp, this.focusPoint.x + this.DX, this.focusPoint.y + this.DY);
			
			g.setFont(this.font);
			g.setColor(Color.white);
			g.fillRect(0, 0, 100, 15);
			g.setColor(Color.black);
			g.drawString(this.scaleStr, 4, 10);
			
			// System.out.println("viewRect.x ..."+viewRect.x);
			// System.out.println("parentScrollPane.getWidth() ..."+parentScrollPane.getWidth());
			// System.out.println("playFieldSize.getWidth() ..."+playFieldSize.getWidth());
			// System.out.println("this.getWidth() ..."+this.getWidth());
			
			if(this.isSelected)
			{
				g.setColor(Color.blue);
				g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
				g.drawRect(1, 1, this.getWidth() - 3, this.getHeight() - 3);
			}
		} /* end paint */
		
		private void refresh()
		{
			this.repaint();
		}
		
		// private void translateLocal(int x, int y){
		// DX = DX+x;
		// DY = DY+y;
		// paint(this.getGraphics());
		// }
		
		public void translateAbsolute(int x, int y, double pPerP)
		{
			int locX = (int) (x * (pPerP / this.pixelPerPixel));
			int locY = (int) (y * (pPerP / this.pixelPerPixel));
			this.DX = locX;
			this.DY = locY;
			this.paint(this.getGraphics());
			// System.out.println("DX ..."+DX+" and DY "+DY);
		}
		
		// --------- EVENT MANAGER
		@Override
		public void mouseClicked(MouseEvent me)
		{
			if(me.getClickCount() == 2)
			{
				Point focus = me.getPoint();
				int deltaX = focus.x - this.focusPoint.x;
				int deltaY = focus.y - this.focusPoint.y;
				Point newFocusPoint = this.getAbsolutePoint(new Point(deltaX, deltaY));
				this.mainPane.receiveNewFocusPoint(newFocusPoint);
			}
		}
		
		@Override
		public void mousePressed(MouseEvent me)
		{
			this.mouse = me.getPoint();
			this.mainPane.cancelAllSelections();
			this.isSelected = true;
			this.setFocusable(true);
			this.requestFocusInWindow();
			// System.out.println("isSelected ..."+isSelected);
		}
		
		@Override
		public void mouseReleased(MouseEvent me)
		{
			this.refresh();
		}
		
		@Override
		public void mouseEntered(MouseEvent me)
		{}
		
		@Override
		public void mouseExited(MouseEvent me)
		{}
		
		@Override
		public void mouseMoved(MouseEvent e)
		{}
		
		@Override
		public void mouseDragged(MouseEvent e)
		{
			if(this.mouse == null)
			{
				return;
			}
			final Point p = e.getPoint();
			int locX = p.x - this.mouse.x;
			int locY = p.y - this.mouse.y;
			this.mainPane.translate(this.DX + locX, this.DY + locY, this.pixelPerPixel);
			this.mouse = p;
		}
		
		@Override
		public void keyTyped(KeyEvent e)
		{
			// System.out.println("Key typed ...");
		}
		
		@Override
		public void keyPressed(KeyEvent e)
		{
			// System.out.println("Key pressed ...");
			if((e.getKeyCode() == KeyEvent.VK_KP_UP) || (e.getKeyCode() == KeyEvent.VK_UP))
			{
				// System.out.println("Up arrow detected ...");
				this.mainPane.translate(this.DX, this.DY - 1, this.pixelPerPixel);
			}
			if((e.getKeyCode() == KeyEvent.VK_KP_DOWN) || (e.getKeyCode() == KeyEvent.VK_DOWN))
			{
				// System.out.println("Down arrow detected ...");
				this.mainPane.translate(this.DX, this.DY + 1, this.pixelPerPixel);
			}
			if((e.getKeyCode() == KeyEvent.VK_KP_LEFT) || (e.getKeyCode() == KeyEvent.VK_LEFT))
			{
				// System.out.println("Left arrow detected ...");
				this.mainPane.translate(this.DX - 1, this.DY, this.pixelPerPixel);
			}
			if((e.getKeyCode() == KeyEvent.VK_KP_RIGHT) || (e.getKeyCode() == KeyEvent.VK_RIGHT))
			{
				// System.out.println("Right arrow detected ...");
				this.mainPane.translate(this.DX + 1, this.DY, this.pixelPerPixel);
			}
		}
		
		@Override
		public void keyReleased(KeyEvent e)
		{
			// System.out.println("Key released ...");
		}
	}
	
	/**
	 * Called upon closing of the window
	 */
	@Override
	public void finalizePlugIn()
	{
		// Don't need to do anything
	}
	
	@Override
	public PlugIn plugIn()
	{
		return this.dialog;
	}
}

// class ImageAlignerTargetListener extends DropTargetAdapter {
//
// private DropTarget dropTarget;
// private ImageAligner parent;
//
// public ImageAlignerTargetListener(ImageAligner parent) {
// this.parent = parent;
// dropTarget = new DropTarget(parent.plugIn(), DnDConstants.ACTION_COPY, this,
// true, null);
// }
//
// public void drop(DropTargetDropEvent event) {
// try {
// if (event.isDataFlavorSupported(TransferableTypeName.jexDataFlavor)) {
// Transferable tr = event.getTransferable();
// TypeName tn = (TypeName)
// tr.getTransferData(TransferableTypeName.jexDataFlavor);
// JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, parent.getFirstEntry())
// XObject bObject = (XObject) Utility.string2Element(name);
//
// System.out.println("   ImageExtractorTargetListener ---> Loading a basicobject");
// parent.load(bObject);
//
// event.acceptDrop(DnDConstants.ACTION_COPY);
// event.dropComplete(true);
// return;
// }
// else if
// (event.isDataFlavorSupported(TransferableButtonDataObject.dataObjectFlavor))
// {
// Transferable tr = event.getTransferable();
// String valueStr = (String)
// tr.getTransferData(TransferableButtonDataObject.dataObjectFlavor);
// XData bDO = (XData) Utility.string2Element(valueStr);
// if (bDO instanceof IImageSet){
// System.out.println("   ImageExtractorTargetListener ---> Loading a dataobject");
// parent.load((IImageSet)bDO);
// }
//
// event.acceptDrop(DnDConstants.ACTION_COPY);
// event.dropComplete(true);
// return;
// }
// else if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
// Transferable tr = event.getTransferable();
// System.out.println("   ImageExtractorTargetListener ---> Recieved a FileListFlavor");
// event.acceptDrop(DnDConstants.ACTION_COPY);
//
// List l = (List) tr.getTransferData(DataFlavor.javaFileListFlavor);
// File[] fileNames = new File[l.size()];
// for (int i=0, len=l.size(); i<len; i++){
// fileNames[i] = (File)l.get(i);
// }
// Arrays.sort(fileNames);
//
// System.out.println("   ImageExtractorTargetListener ---> Loading a file list dataobject");
// parent.load(fileNames);
// return;
// }
// event.rejectDrop();
// } catch (Exception e) {
// e.printStackTrace();
// event.rejectDrop();
// }
// }
// }
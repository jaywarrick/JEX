package jex.experimentalTreeAndViewer;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.DataReader.LabelReader;
import Database.Definition.Experiment;
import Database.Definition.TypeName;
import guiObject.PaintComponentDelegate;
import guiObject.PixelComponentDisplay;
import image.roi.PointList;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class ArrayPreviewController {
	
	public static Color GRID = Color.BLACK, BACKGROUND = Color.WHITE, SELECTOR_RC = Color.LIGHT_GRAY, SELECTOR_UNSELECTED = Color.WHITE, SELECTOR_SELECTED = DisplayStatics.selectedButtonBorder;
	public static final String SIG_SelectionChanged_NULL = "SIG_SelectionChanged_NULL";
	public static final int ALL = 0, ROW = 1, COL = 2, SINGLE = 3;
	
	int rows, cols;
	public JPanel panel;
	public JPanel legendPanel;
	public ArrayPreviewPanel arrayPreview;
	public Experiment tray;
	public HashMap<Point,JEXLabel> labels = new HashMap<Point,JEXLabel>();
	public HashMap<String,Color> labelLegend = new HashMap<String,Color>();
	
	public ArrayPreviewController()
	{   
		
	}
	
	public void setTray(Experiment tray)
	{
		this.tray = tray;
		
		int width = 0, height = 0;
		if(this.tray != null)
		{
			for (Integer x : this.tray.keySet())
			{
				if((x + 1) > width)
				{
					width = x + 1;
				}
				for (Integer y : this.tray.get(x).keySet())
				{
					if((y + 1) > height)
					{
						height = y + 1;
					}
				}
			}
		}
		this.setRowsAndCols(height, width);
		// rebuildModel();
	}
	
	public void setRowsAndCols(int rows, int cols)
	{
		this.rows = rows;
		this.cols = cols;
	}
	
	public JPanel panel()
	{
		if(panel == null)
		{
			rebuildModel();
		}
		return panel;
	}
	
	public void rebuildModel()
	{
		if(tray == null)
		{
			arrayPreview = null;
			panel = new JPanel();
		}
		else
		{
			if(arrayPreview == null)
				arrayPreview = new ArrayPreviewPanel();
			showLegend();
			
			int cellSizeX = 30;
			int cellSizey = 25;
			int width = cellSizeX * tray.size() + cellSizeX;
			int height = cellSizey * tray.get(0).size() + cellSizey;
			panel = new JPanel();
			panel.setBackground(DisplayStatics.lightBackground);
			panel.setLayout(new MigLayout("ins 5, center", "", ""));
			panel.add(arrayPreview.panel(), "height " + height + ", width " + width);
			panel.add(legendPanel, "");
		}
		panel.revalidate();
		panel.repaint();
	}
	
	public void showLegend()
	{
		// Make a legend panel
		legendPanel = new JPanel();
		legendPanel.setLayout(new MigLayout("ins 4", "", ""));
		legendPanel.setBackground(DisplayStatics.lightBackground);
		
		// Display the selected object if one is selected
		TypeName selectedObject = JEXStatics.jexManager.getSelectedObject();
		
		// If null show the current selection
		if(selectedObject == null)
		{
			makeSelectionLegend();
		}
		// Else show the objects
		else
		{
			makeLabelLegend();
		}
	}
	
	private void makeSelectionLegend()
	{
		// Unselect the array cells
		this.arrayPreview.deselectAll();
		
		// Set their color to null to force selection color display
		this.arrayPreview.setColors(null);
		
		// Set the selected cells
		for (JEXEntry e : this.tray.entries)
		{
			if(JEXStatics.jexManager.isSelected(e))
			{
				this.arrayPreview._select(new Point(e.getTrayX(), e.getTrayY()));
			}
		}
		
		// Create a panel and fill it with the legend
		JLabel label1 = new JLabel("Values previewed in the array:");
		label1.setFont(FontUtility.italicFonts);
		legendPanel.add(label1, "wrap");
		
		Icon selectedbox = JEXStatics.iconRepository.boxIcon(20, 20, Color.red);
		JLabel label2 = new JLabel("Selected");
		label2.setIcon(selectedbox);
		label2.setFont(FontUtility.italicFonts);
		legendPanel.add(label2, "wrap");
		
		Icon unSelectedbox = JEXStatics.iconRepository.boxIcon(20, 20, DisplayStatics.lightBackground);
		JLabel label3 = new JLabel("Unselected");
		label3.setIcon(unSelectedbox);
		label3.setFont(FontUtility.italicFonts);
		legendPanel.add(label3, "wrap");
	}
	
	private void makeLabelLegend()
	{
		// make the color map for the array
		getCurrentLabelsInTray();
		HashMap<Point,Color> colorMap = getColorMap();
		
		// Color the array
		arrayPreview.setColors(colorMap);
		
		// Create a panel and fill it with the legend
		String labelName = JEXStatics.jexManager.getSelectedObject().getName();
		JLabel label1 = new JLabel("Preview of the values of " + labelName);
		label1.setFont(FontUtility.italicFonts);
		legendPanel.add(label1, "wrap");
		
		for (String value : this.labelLegend.keySet())
		{
			Color color = this.labelLegend.get(value);
			Icon labelColor = JEXStatics.iconRepository.boxIcon(20, 20, color);
			JLabel labelLabel = new JLabel(value);
			labelLabel.setIcon(labelColor);
			labelLabel.setFont(FontUtility.italicFonts);
			legendPanel.add(labelLabel, "wrap");
		}
	}
	
	private HashMap<Point,Color> getColorMap()
	{
		HashMap<Point,Color> ret = new HashMap<Point,Color>();
		for (Point p : this.labels.keySet())
		{
			ret.put(p, JEXStatics.labelColorCode.getColorForLabel(this.labels.get(p)));
		}
		return ret;
	}
	
	private void getCurrentLabelsInTray()
	{
		this.labels.clear();
		this.labelLegend.clear();
		if(this.tray == null)
			return;
		for (int x = 0; x < cols; x++)
		{
			for (int y = 0; y < rows; y++)
			{
				// Fill the label point map
				JEXLabel label = this.getCurrentLabelInEntry(this.tray.get(x).get(y));
				if(label != null)
				{
					this.labels.put(new Point(x, y), label);
				}
				else
				{
					continue;
				}
				
				// Fill the label legend map
				String value = LabelReader.readLabelValue(label);
				Color color = JEXStatics.labelColorCode.getColorForLabel(label);
				labelLegend.put(value, color);
			}
		}
	}
	
	private JEXLabel getCurrentLabelInEntry(JEXEntry e)
	{
		TypeName selectedObj = JEXStatics.jexManager.getSelectedObject();
		if(selectedObj == null)
			return null;
		if(!selectedObj.getType().equals(JEXData.LABEL))
			return null;
		
		JEXData currentLabel = JEXStatics.jexManager.getDataOfTypeNameInEntry(selectedObj, e);
		if(currentLabel == null)
			return null;
		
		JEXLabel ret = new JEXLabel(LabelReader.readLabelName(currentLabel), LabelReader.readLabelValue(currentLabel), LabelReader.readLabelUnit(currentLabel));
		return ret;
	}
	
	public void openArray()
	{   
		
	}
	
	class ArrayPreviewPanel implements PaintComponentDelegate, MouseListener, MouseMotionListener {
		
		public PixelComponentDisplay display;
		public PointList selected;
		public Point pressed, released;
		public HashMap<Point,Color> colors;
		
		public ArrayPreviewPanel()
		{
			this.selected = new PointList();
			this.display = new PixelComponentDisplay(this);
			this.display.addMouseListener((MouseListener) this);
			this.display.addMouseMotionListener((MouseMotionListener) this);
			this.display.setFocusable(true);
		}
		
		public PixelComponentDisplay panel()
		{
			return this.display;
		}
		
		public void setColors(HashMap<Point,Color> colors)
		{
			this.colors = colors;
			this.display.repaint();
		}
		
		public void deselectAll()
		{
			this.selected.clear();
		}
		
		public void select(Point index)
		{
			if((rows == 0 && cols == 0) || index.x < -1 || index.y < -1 || index.x > cols || index.y > rows)
				return;
			if(index.x == -1 || index.y == -1)
			{
				if(index.x == -1 && index.y == -1)
				{
					if(this.selected.size() >= rows * cols)
					{
						this.selected.clear();
						this.display.repaint();
						SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
					}
					else
					{
						for (int y = 0; y < rows; y++)
						{
							for (int x = 0; x < cols; x++)
							{
								this._select(new Point(x, y));
							}
						}
						this.display.repaint();
						SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
					}
				}
				else if(index.x == -1)
				{
					if(!this.isSelected(new Point(0, index.y)))
					{
						for (int x = 0; x < cols; x++)
						{
							this._select(new Point(x, index.y));
						}
					}
					else
					{
						for (int x = 0; x < cols; x++)
						{
							this._unselect(new Point(x, index.y));
						}
					}
					this.display.repaint();
					SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
				}
				else if(index.y == -1)
				{
					if(!this.isSelected(new Point(index.x, 0)))
					{
						for (int y = 0; y < rows; y++)
						{
							this._select(new Point(index.x, y));
						}
					}
					else
					{
						for (int y = 0; y < rows; y++)
						{
							this._unselect(new Point(index.x, y));
						}
					}
					this.display.repaint();
					SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
				}
			}
			else
			{
				if(!this.isSelected(index))
				{
					this._select(index);
				}
				else
				{
					this._unselect(index);
				}
				this.display.repaint();
				SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
			}
		}
		
		public void _select(Point index)
		{
			if(!this.isSelected(index))
			{
				this.selected.add(index);
			}
		}
		
		public void _unselect(Point index)
		{
			for (Point p : selected)
			{
				if(p.x == index.x && p.y == index.y)
				{
					this.selected.remove(p);
					return;
				}
			}
		}
		
		public PointList getSelected()
		{
			return this.selected;
		}
		
		public boolean isSelected(Point index)
		{
			for (Point p : selected)
			{
				if(p.x == index.x && p.y == index.y)
				{
					return true;
				}
			}
			return false;
		}
		
		// ////////////////////////////////////
		// /////// Drawing Methods ////////////
		// ////////////////////////////////////
		
		public void paintComponent(Graphics2D g2)
		{
			g2.setColor(BACKGROUND);
			Rectangle r = this.display.getBoundsLocal();
			g2.fill(r);
			
			if(rows > 0 && cols > 0)
			{
				this.drawTop(g2);
				this.drawLeft(g2);
				this.drawMiddle(g2);
				this.drawGrid(g2);
			}
			
			g2.setColor(Color.BLACK);
			g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
			g2.dispose();
		}
		
		private void drawTop(Graphics2D g)
		{
			g.setColor(SELECTOR_RC);
			int x, y = 3, w, h = this.getRowYStart(0) - 6;
			for (int c = -1; c < cols; c++)
			{
				x = this.getColXStart(c) + 3;
				w = this.getColXStart(c + 1) - x - 3;
				g.fillRect(x, y, w, h);
			}
		}
		
		private void drawLeft(Graphics2D g)
		{
			g.setColor(SELECTOR_RC);
			int x = 3, y, w = this.getColXStart(0) - 6, h;
			for (int r = 0; r < rows; r++)
			{
				y = this.getRowYStart(r) + 3;
				h = this.getRowYStart(r + 1) - y - 3;
				g.fillRect(x, y, w, h);
			}
		}
		
		private void drawMiddle(Graphics2D g)
		{
			if(this.colors == null)
			{
				g.setColor(SELECTOR_SELECTED);
				int x, y, w, h;
				for (Point index : this.selected)
				{
					y = this.getRowYStart(index.y);
					h = this.getRowYStart(index.y + 1) - y;
					x = this.getColXStart(index.x);
					w = this.getColXStart(index.x + 1) - x;
					g.fillRect(x, y, w, h);
				}
			}
			else
			{
				int x, y, w, h;
				for (Point index : this.colors.keySet())
				{
					y = this.getRowYStart(index.y);
					h = this.getRowYStart(index.y + 1) - y;
					x = this.getColXStart(index.x);
					w = this.getColXStart(index.x + 1) - x;
					g.setColor(this.colors.get(index));
					g.fillRect(x, y, w, h);
				}
			}
		}
		
		private void drawGrid(Graphics2D g)
		{
			g.setColor(GRID);
			int x1 = 0;
			int x2 = this.display.getBoundsLocal().width + 1;
			int y1, y2;
			for (int r = 0; r < rows; r++)
			{
				y1 = this.getRowYStart(r);
				y2 = y1;
				g.drawLine(x1, y1, x2, y2);
			}
			y1 = 0;
			y2 = this.display.getBoundsLocal().height + 1;
			for (int c = 0; c < cols; c++)
			{
				x1 = this.getColXStart(c);
				x2 = x1;
				g.drawLine(x1, y1, x2, y2);
			}
		}
		
		private double getColWidth()
		{
			Rectangle r = this.display.getBoundsLocal();
			double total = r.width;
			return (total) / ((double) (cols + 1));
		}
		
		private int getColXStart(int col)
		{
			return (int) Math.ceil((this.getColWidth()) * ((double) (col + 1)));
		}
		
		private double getRowHeight()
		{
			Rectangle r = this.display.getBoundsLocal();
			double total = r.height;
			return (total) / ((double) (rows + 1));
		}
		
		private int getRowYStart(int row)
		{
			return (int) Math.ceil((this.getRowHeight()) * ((double) (row + 1)));
		}
		
		// /////////////////////////////////////////////////////////
		// ////////////// handle MouseEvents ///////////////////////
		// /////////////////////////////////////////////////////////
		public void mouseClicked(MouseEvent e)
		{}
		
		public void mouseEntered(MouseEvent e)
		{
			this.display.requestFocusInWindow();
		}
		
		public void mouseExited(MouseEvent e)
		{}
		
		public void mousePressed(MouseEvent e)
		{
			this.pressed = e.getPoint();
		}
		
		public void mouseReleased(MouseEvent e)
		{
			this.released = e.getPoint();
			Point releasedIndex = this.indexOfPoint(this.released);
			Point pressedIndex = this.indexOfPoint(this.pressed);
			if(releasedIndex.x == pressedIndex.x && releasedIndex.y == pressedIndex.y)
			{
				this.select(releasedIndex);
			}
			
		}
		
		public void mouseDragged(MouseEvent e)
		{}
		
		public void mouseMoved(MouseEvent e)
		{}
		
		private Point indexOfPoint(Point xy)
		{
			Point ret = new Point(-1, -1); // selectAll location
			double posX = Math.floor(((double) xy.x) / this.getColWidth());
			double posY = Math.floor(((double) xy.y) / this.getRowHeight());
			ret.x = (int) posX - 1;
			ret.y = (int) posY - 1;
			return ret;
		}
	}
}

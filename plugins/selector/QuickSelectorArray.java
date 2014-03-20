package plugins.selector;

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

import jex.statics.DisplayStatics;
import signals.SSCenter;

public class QuickSelectorArray implements PaintComponentDelegate, MouseListener, MouseMotionListener {
	
	public static Color GRID = Color.BLACK, BACKGROUND = Color.WHITE, SELECTOR_RC = Color.LIGHT_GRAY, SELECTOR_UNSELECTED = Color.WHITE, SELECTOR_SELECTED = DisplayStatics.selectedButtonBorder;
	
	public static final String SIG_SelectionChanged_NULL = "SIG_SelectionChanged_NULL";
	public static final int ALL = 0, ROW = 1, COL = 2, SINGLE = 3;
	
	public PixelComponentDisplay display;
	int rows, cols;
	public PointList selected;
	public Point pressed, released;
	
	public QuickSelectorArray()
	{
		this.rows = 0;
		this.cols = 0;
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
	
	public void setRowsAndCols(int rows, int cols)
	{
		this.rows = rows;
		this.cols = cols;
	}
	
	public void setBlank()
	{
		this.setRowsAndCols(0, 0);
	}
	
	public void deselectAll()
	{
		this.selected.clear();
	}
	
	public void select(Point index)
	{
		if((this.rows == 0 && this.cols == 0) || index.x < -1 || index.y < -1 || index.x > cols || index.y > rows)
			return;
		if(index.x == -1 || index.y == -1)
		{
			if(index.x == -1 && index.y == -1)
			{
				if(this.selected.size() >= this.rows * this.cols)
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
	
	// public void selectAllOrNone(boolean all)
	// {
	// if(all)
	// {
	// this._select(new Point(-1,-1));
	// }
	// else
	// {
	// this.selected.clear();
	// }
	// }
	
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
		
		if(this.rows > 0 && this.cols > 0)
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
		for (int c = -1; c < this.cols; c++)
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
		for (int r = 0; r < this.rows; r++)
		{
			y = this.getRowYStart(r) + 3;
			h = this.getRowYStart(r + 1) - y - 3;
			g.fillRect(x, y, w, h);
		}
	}
	
	private void drawMiddle(Graphics2D g)
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

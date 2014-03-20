package plugins.valueTable;

import guiObject.PaintComponentDelegate;
import guiObject.PixelComponentDisplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.ImageObserver;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class ScrollPainter implements PaintComponentDelegate, MouseListener, MouseMotionListener, MouseWheelListener, ImageObserver {
	
	public static final String SIG_IndexChanged_NULL = "SIG_IndexChanged_NULL";
	public static final String SIG_Toggled_NULL = "SIG_Toggled_NULL";
	public static final String SIG_ScrollFinished_NULL = "SIG_ScrollFinished_NULL";
	public static final String SIG_ScrollStarted_NULL = "SIG_ScrollStarted_NULL";
	
	public static final int HERE = 0, ALL = 1, LEFT = 2, RIGHT = 3;
	
	private PixelComponentDisplay panel;
	private int numPositions;
	private int index, edge, handleRadius;
	private Line2D lineShape;
	private Ellipse2D handleShape;
	private String nameText, valueText, endText;
	private int toggleState = HERE;
	private Image hereIcon, leftIcon, rightIcon, allIcon;
	private boolean hasToggle;
	private int toggleWidth;
	private FontMetrics fm;
	private int lineWidth;
	private int verticalOffset;
	public boolean listenToAllWheelEvents = false;
	
	private static final BasicStroke strokeLine = new BasicStroke(15, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke strokeHandle = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	public Color backColor = DisplayStatics.background, leftColor = Color.BLACK, rightColor = new Color(75, 75, 75);
	
	public ScrollPainter(int lineWidth, int numPositions, boolean hasToggle)
	{
		
		this.hasToggle = hasToggle;
		this.lineWidth = lineWidth;
		this.handleRadius = lineWidth / 2;
		this.toggleWidth = lineWidth + 8;
		this.numPositions = numPositions;
		this.verticalOffset = 0;
		this.setEdge(3);
		
		this.panel = new PixelComponentDisplay(this);
		this.panel.setBackground(backColor);
		this.setHeight(toggleWidth);
		this.panel.addMouseListener((MouseListener) this);
		this.panel.addMouseMotionListener((MouseMotionListener) this);
		this.panel.addMouseWheelListener((MouseWheelListener) this);
		
		this.lineShape = new Line2D.Float(0, 0, 0, 0);
		this.handleShape = new Ellipse2D.Float(0, 0, 0, 0);
		this.index = 0;
		this.nameText = "";
		this.endText = "";
		this.valueText = "";
		// this.onIcon =
		// JEXStatics.iconRepository.getImageWithName(IconRepository.TOGGLE_ON2,
		// toggleWidth, toggleWidth);
		// this.offIcon =
		// JEXStatics.iconRepository.getImageWithName(IconRepository.TOGGLE_OFF,
		// toggleWidth, toggleWidth);
	}
	
	public int index()
	{
		return this.index;
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void paintComponent(Graphics2D g2)
	{
		if(this.panel == null)
			return;
		g2.setColor(backColor);
		g2.fill(this.panel.getBoundsLocal());
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Point handleP = this.handleLocation();
		
		// Draw Scroll
		g2.setStroke(strokeLine);
		g2.setColor(rightColor);
		this.lineShape.setLine(handleP.x, this.lineY(), this.right(), this.lineY());
		g2.draw(this.lineShape);
		g2.setColor(leftColor);
		this.lineShape.setLine(this.left(), this.lineY(), handleP.x, this.lineY());
		g2.draw(this.lineShape);
		
		// Draw Handle
		g2.setPaint(rightColor);// Color.DARK_GRAY);
		this.handleShape.setFrameFromCenter(handleP.x, handleP.y, handleP.x + this.handleRadius, handleP.y + this.handleRadius);
		g2.fill(this.handleShape);
		g2.setColor(leftColor);
		g2.setStroke(strokeHandle);
		g2.draw(this.handleShape);
		
		// Draw text
		g2.setFont(FontUtility.boldFont);
		fm = g2.getFontMetrics();
		int nameTextWidth = fm.stringWidth(this.nameText + ": ");
		if(!(this.nameText == null || this.nameText.equals("")))
		{
			g2.drawString(this.nameText + ": ", this.left(), this.lineY() - 10);
		}
		g2.setFont(FontUtility.defaultFont);
		fm = g2.getFontMetrics();
		int endTextWidth = fm.stringWidth(this.endText);
		g2.drawString(this.valueText, this.left() + nameTextWidth, this.lineY() - 10);
		g2.drawString(this.endText, (int) (this.right() - endTextWidth), this.lineY() - 10);
		
		// Draw toggle
		if(this.hasToggle)
		{
			Rectangle toggleRect = this.toggleRect();
			if(toggleState == HERE)
				g2.drawImage(hereIcon, toggleRect.x, toggleRect.y, toggleRect.x + toggleRect.width, toggleRect.y + toggleRect.height, 0, 0, toggleWidth, toggleWidth, this);
			else if(toggleState == LEFT)
				g2.drawImage(leftIcon, toggleRect.x, toggleRect.y, toggleRect.x + toggleRect.width, toggleRect.y + toggleRect.height, 0, 0, toggleWidth, toggleWidth, this);
			else if(toggleState == RIGHT)
				g2.drawImage(rightIcon, toggleRect.x, toggleRect.y, toggleRect.x + toggleRect.width, toggleRect.y + toggleRect.height, 0, 0, toggleWidth, toggleWidth, this);
			else
				g2.drawImage(allIcon, toggleRect.x, toggleRect.y, toggleRect.x + toggleRect.width, toggleRect.y + toggleRect.height, 0, 0, toggleWidth, toggleWidth, this);
		}
		g2.dispose();
	}
	
	public void repaint()
	{
		if(this.panel != null)
		{
			this.panel.repaint();
		}
	}
	
	public void setNumPositions(int n)
	{
		this.numPositions = n;
		this.setIndex(this.index);
	}
	
	public void setIndex(int index)
	{
		if(index < 0)
			this.index = 0;
		else if(index >= numPositions)
			this.index = numPositions - 1;
		else
			this.index = index;
		this.repaint();
	}
	
	public void indexChanged()
	{
		// Manager.log("Index changed", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_IndexChanged_NULL, (Object[]) null);
	}
	
	public void setEdge(int edge)
	{
		this.edge = edge;
		this.repaint();
	}
	
	public void setVerticalOffset(int downwardShift)
	{
		this.verticalOffset = downwardShift;
		this.repaint();
	}
	
	public void setHeight(int height)
	{
		this.panel.setMinimumSize(new Dimension(20, height));
		this.panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		this.panel.setPreferredSize(new Dimension(20, height));
	}
	
	public void setHandleRadius(int radius)
	{
		this.handleRadius = radius;
		this.repaint();
	}
	
	public void setNameText(String text)
	{
		this.nameText = text;
		this.repaint();
	}
	
	public void setEndText(String text)
	{
		this.endText = text;
		this.repaint();
	}
	
	public void setValueText(String text)
	{
		this.valueText = text;
		this.repaint();
	}
	
	public void setToggleEnabled(boolean enabled)
	{
		this.hasToggle = enabled;
		this.repaint();
	}
	
	public void setToggleHereImage(Image im)
	{
		this.hereIcon = im;
	}
	
	public void setToggleLeftImage(Image im)
	{
		this.leftIcon = im;
	}
	
	public void setToggleRightImage(Image im)
	{
		this.rightIcon = im;
	}
	
	public void setToggleAllImage(Image im)
	{
		this.allIcon = im;
	}
	
	public int toggleWidth()
	{
		return this.toggleWidth;
	}
	
	private void setIndex(double dragX)
	{
		double newHandleX = (this.firstHandle.x + dragX);
		int index = (int) Math.round((numPositions - 1) * ((double) (newHandleX - this.left()) / ((double) (this.right() - this.left()))));
		if(index != this.index)
		{
			this.setIndex(index);
			this.indexChanged();
		}
	}
	
	private void toggle()
	{
		toggleState = toggleState + 1;
		if(toggleState > RIGHT)
			toggleState = HERE;
		this.repaint();
		this.toggled();
	}
	
	public void toggled()
	{
		SSCenter.defaultCenter().emit(this, SIG_Toggled_NULL, (Object[]) null);
	}
	
	public void setState(int newState)
	{
		if(toggleState == newState)
			return;
		this.toggleState = newState;
		this.repaint();
	}
	
	public int state()
	{
		return this.toggleState;
	}
	
	private Point handleLocation()
	{
		Point ret = new Point(0, this.lineY());
		ret.x = ((int) Math.round((this.right() - this.left()) * ((double) this.index) / ((double) this.numPositions - 1))) + this.left();
		return ret;
	}
	
	private Rectangle toggleRect()
	{
		return new Rectangle(this.right() + this.minEdge() + this.edge(), (int) (this.lineY() - toggleWidth / 2), (int) toggleWidth, (int) toggleWidth);
	}
	
	private Rectangle handleRect()
	{
		Point handleP = this.handleLocation();
		return new Rectangle(handleP.x - this.handleRadius - 1, handleP.y - this.handleRadius - 1, 2 * handleRadius + 2, 2 * handleRadius + 2);
	}
	
	private int edge()
	{
		return this.edge;
	}
	
	private int minEdge()
	{
		int ret = (int) Math.max(this.lineWidth / 2, strokeHandle.getLineWidth() / 2 + this.handleRadius) + 1;
		return ret;
	}
	
	private int left()
	{
		return this.minEdge() + this.edge();
	}
	
	private int right()
	{
		if(hasToggle)
		{
			return this.panel.getBoundsLocal().width - this.minEdge() - 2 * this.edge() - toggleWidth;
		}
		else
		{
			return this.panel.getBoundsLocal().width - this.minEdge() - this.edge();
		}
	}
	
	private int lineY()
	{
		return (int) (this.center() + this.verticalOffset);
	}
	
	private int center()
	{
		return this.panel.getBoundsLocal().height / 2;
	}
	
	boolean movingHandle = false;
	Point firstPress = null;
	Point firstHandle = null;
	double dragX = 0;
	
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		// System.out.println(e.getWheelRotation());
		if(e.isShiftDown() || listenToAllWheelEvents) // then horizontal
		{
			this.setIndex(this.index + e.getWheelRotation());
			this.indexChanged();
		}
		else
		{
			Component parent = this.panel.getParent();
			parent.dispatchEvent(e);
		}
	}
	
	public void mouseDragged(MouseEvent e)
	{
		dragX = (double) (e.getPoint().x - firstPress.x);
		if(this.movingHandle)
		{
			this.setIndex(dragX);
		}
	}
	
	public void mouseMoved(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void mouseClicked(MouseEvent e)
	{   
		
	}
	
	public void mouseEntered(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void mouseExited(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void mousePressed(MouseEvent e)
	{
		firstPress = e.getPoint();
		firstHandle = this.handleLocation();
		// System.out.println("Pressed " + click);
		// System.out.println("Handle " + this.handleRect());
		if(this.handleRect().contains(firstPress))
			this.movingHandle = true;
		else
			this.movingHandle = false;
		if(this.movingHandle)
		{
			SSCenter.defaultCenter().emit(this, SIG_ScrollStarted_NULL, (Object[]) null);
		}
	}
	
	public void mouseReleased(MouseEvent e)
	{
		if(!this.movingHandle)
		{
			Point p = e.getPoint();
			Rectangle r = this.handleRect();
			if(this.toggleRect().contains(p))
			{
				this.toggle();
			}
			else if(!r.contains(p))
			{
				if(p.y >= (this.lineY() - this.lineWidth / 2) && p.y <= (this.lineY() + this.lineWidth / 2))
				{
					if(p.x >= (this.left() - this.lineWidth) && p.x <= r.x)
					{
						this.setIndex(this.index - 1);
						this.indexChanged();
					}
					else if(p.x >= (r.x + r.width) && p.x <= (this.right() + this.lineWidth))
					{
						this.setIndex(this.index + 1);
						this.indexChanged();
					}
				}
			}
		}
		else
		{
			this.movingHandle = false;
			this.firstPress = null;
			this.firstHandle = null;
			SSCenter.defaultCenter().emit(this, SIG_ScrollFinished_NULL, (Object[]) null);
		}
		
	}
	
	public boolean imageUpdate(Image arg0, int info_flags, int x, int y, int width, int height)
	{
		if(info_flags != ALLBITS)
		{
			// Indicates image has not finished loading
			// Returning true will tell the image loading
			// thread to keep drawing until image fully
			// drawn loaded.
			// Logs.log("Had to wait for image", 0, this);
			return true;
		}
		else
		{
			this.repaint();
			return false;
		}
	}
	
}

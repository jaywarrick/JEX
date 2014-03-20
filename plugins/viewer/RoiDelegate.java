package plugins.viewer;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Vector;

import signals.SSCenter;

public class RoiDelegate {
	
	public static final String SIG_RoiUpdated_Null = "SIG_RoiUpdated_Null";
	public static final String SIG_RoiFinished_Null = "SIG_RoiFinished_Null";
	protected static final Color[] defaultHandle = new Color[] { Color.BLACK, Color.WHITE };
	protected static final PointList emptyPointList = new PointList();
	protected static final List<Point> emptyLineList = new Vector<Point>();
	
	public static final int MODE_VIEW = 0, MODE_MOVE = 1, MODE_CREATE = 2, MODE_SMASHED = 3;
	
	protected PointList editablePoints;
	protected List<PointList> smashedPoints;
	protected List<Point> lines = new Vector<Point>();
	protected List<Point> growthLines = new Vector<Point>();
	protected int type = ROIPlus.ROI_RECT;
	
	public RoiDelegate(PointList roi)
	{
		this(roi, new Vector<PointList>());
	}
	
	/**
	 * accepts null argument to create an empty roi. an empty PointList and null both result in the same RoiDelegate
	 * 
	 * @param pl
	 */
	public RoiDelegate(PointList roi, List<PointList> smashedRois)
	{
		if(roi == null)
			editablePoints = new PointList();
		else
			editablePoints = roi;
		if(smashedRois == null)
			smashedRois = new Vector<PointList>();
		else
			this.smashedPoints = smashedRois;
		// if(this.points.size() == 0) this.paint(); // results in update and
		// create mode
		// else this.roiFinished(); // results in update and move mode
	}
	
	// No need to reimplement these
	public int type()
	{
		return this.type;
	}
	
	public void setEditablePoints(PointList roi)
	{
		if(roi == null)
			roi = new PointList();
		this.editablePoints = roi;
		this.roiUpdated();
	}
	
	public void setSmashedPoints(List<PointList> list)
	{
		if(list == null)
			list = new Vector<PointList>();
		this.smashedPoints = list;
		this.roiUpdated();
	}
	
	public List<PointList> getSmashedPoints()
	{
		if(this.smashedPoints == null)
			return new Vector<PointList>();
		else
			return this.smashedPoints;
	}
	
	public void paintSmashedRois(Graphics2D g2, List<PointList> transformedPointLists)
	{
		int n = countNonEmptyPointLists(this.smashedPoints);
		int count = 0;
		for (int i = 0; i < this.smashedPoints.size(); i++)
		{
			if(transformedPointLists.get(i).size() != 0)
			{
				g2.setColor(getColorForIntInRange(count, n));
				this.paintLinesForPointList(g2, transformedPointLists.get(i), MODE_SMASHED, null);
				count++;
			}
		}
	}
	
	public static Color getColorForIntInRange(int index, int ofTotal)
	{
		if(ofTotal < 2)
			return new Color(0, 0, 255);
		double ratio = (((double) (index)) / (ofTotal - 1));
		int value = (int) (255 * (ratio));
		Color ret = new Color(value, value, 255);
		return ret;
	}
	
	private int countNonEmptyPointLists(List<PointList> l)
	{
		int ret = 0;
		for (PointList p : l)
		{
			if(p.size() > 0)
				ret++;
		}
		return ret;
	}
	
	public PointList getEditablePoints()
	{
		return this.editablePoints;
	}
	
	public void clearAllPoints()
	{
		this.editablePoints.clear();
		this.smashedPoints.clear();
	}
	
	// public Point getReferenceOfPointAt(Point p){ return
	// this.points.getReferenceOfPointAt(p);}
	public void roiUpdated()
	{
		SSCenter.defaultCenter().emit(this, SIG_RoiUpdated_Null, (Object[]) null);
	}
	
	public void roiFinished()
	{
		SSCenter.defaultCenter().emit(this, SIG_RoiFinished_Null, (Object[]) null);
	}
	
	public Color[] getHandleBorderAndFillColor(int index)
	{
		return defaultHandle;
	}
	
	public int size()
	{
		return editablePoints.size();
	}
	
	public Rectangle getBounds()
	{
		Rectangle ret = this.editablePoints.getBounds();
		return ret;
	}
	
	public List<Point> getLines()
	{
		return this.lines;
	}
	
	// Reimplement these in all subclasses;
	public boolean isLineType()
	{
		return false;
	}
	
	public void paintLinesForPointList(Graphics2D g2, PointList transformedPoints, int mode, Point cursorLocation)
	{}
	
	public boolean pointIsInRoi(Point p)
	{
		return false;
	}
	
	public void leftClicked(Point p)
	{};
	
	public void rightClicked(Point p)
	{}
	
	public void dragging(Rectangle r)
	{}
	
	public void dragFinished(Rectangle r)
	{}
	
	public void finishRoi()
	{
		SSCenter.defaultCenter().emit(this, SIG_RoiFinished_Null, (Object[]) null);
	}
	
	public void handleLeftClicked(Point p)
	{}
	
	public void handleRightClicked(Point p)
	{}
	
	// This should be called to keep the lineList updated for future calls to
	// paint()
	// An up-to-date line list will facilitate faster drawing later
	// so it doesn't have to be recreated or updated for each call to paint()
	
}

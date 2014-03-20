package plugins.viewer;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import logs.Logs;

public class PGonDelegate extends RoiDelegate {
	
	private Rectangle rect;
	
	public PGonDelegate(PointList pl)
	{
		super(pl);
		this.type = ROIPlus.ROI_POLYGON;
	}
	
	// Reimplement these in all subclasses;
	@Override
	public boolean isLineType()
	{
		return false;
	}
	
	@Override
	public void paintLinesForPointList(Graphics2D g2, PointList transformedList, int mode, Point cursorLocation)
	{
		Point p1;
		Point p2 = null;
		// g2.draw(g2.getClip());
		if(transformedList.size() > 1)
		{
			p2 = transformedList.get(0);
			for (int i = 1; i < transformedList.size(); i++)
			{
				p1 = p2;
				p2 = transformedList.get(i);
				g2.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
		if(transformedList.size() > 0 && mode == MODE_CREATE)
		{
			p1 = transformedList.get(transformedList.size() - 1);
			p2 = cursorLocation;
			g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
		else if(transformedList.size() > 0)
		{
			p1 = p2;
			p2 = transformedList.get(0);
			g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
	}
	
	@Override
	public boolean pointIsInRoi(Point p)
	{
		rect = this.editablePoints.getBounds();
		return rect.contains(p);
	}
	
	@Override
	public void leftClicked(Point p)
	{
		this.editablePoints.add(p);
		this.roiUpdated();
	}
	
	@Override
	public void rightClicked(Point p)
	{
		this.roiFinished();
	}
	
	@Override
	public void dragging(Rectangle r)
	{}
	
	@Override
	public void dragFinished(Rectangle r)
	{
		this.editablePoints.add(r.x, r.y);
		this.roiUpdated();
	}
	
	@Override
	public void finishRoi()
	{
		this.roiFinished();
	}
	
	@Override
	public void handleLeftClicked(Point p)
	{
		Logs.log("handleLeftClicked", 0, this);
	}
	
	@Override
	public void handleRightClicked(Point p)
	{
		Logs.log("handleRightClicked", 0, this);
	}
	
}

package plugins.viewer;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import logs.Logs;

public class RectDelegate extends RoiDelegate {
	
	protected Rectangle rect;
	protected boolean dragging = false;
	
	public RectDelegate(PointList pl)
	{
		super(pl);
		this.type = ROIPlus.ROI_RECT;
	}
	
	// //////////////////////////
	
	@Override
	public boolean isLineType()
	{
		return false;
	}
	
	@Override
	public void paintLinesForPointList(Graphics2D g2, PointList transformedList, int mode, Point cursorLocation)
	{
		if(transformedList.size() == 2)
			g2.draw(transformedList.getBounds());
		else if(transformedList.size() == 1 && mode == MODE_CREATE)
		{
			this.rect = new Rectangle(transformedList.get(0).x, transformedList.get(0).y, cursorLocation.x - transformedList.get(0).x, cursorLocation.y - transformedList.get(0).y);
			makePositive(this.rect);
			g2.draw(this.rect);
		}
	}
	
	protected static void makePositive(Rectangle r)
	{
		if(r.width < 0)
		{
			r.x = r.x + r.width;
			r.width = -1 * r.width;
		}
		if(r.height < 0)
		{
			r.y = r.y + r.height;
			r.height = -1 * r.height;
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
		if(this.editablePoints.size() < 2)
		{
			this.editablePoints.add(p);
		}
		if(this.editablePoints.size() == 2)
			this.roiFinished();
		else
			this.roiUpdated();
	}
	
	@Override
	public void rightClicked(Point p)
	{
		Logs.log("rightClicked", 0, this);
	}
	
	@Override
	public void dragging(Rectangle r)
	{
		if(dragging == false && this.editablePoints.size() < 2)
		{
			this.editablePoints.add(r.x, r.y);
		}
		
		dragging = true;
		
		this.roiUpdated();
	}
	
	@Override
	public void dragFinished(Rectangle r)
	{
		dragging = false;
		if(this.editablePoints.size() < 2)
		{
			this.editablePoints.add(r.x + r.width, r.y + r.height);
			this.roiFinished();
		}
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

package plugins.viewer;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Graphics2D;
import java.awt.Point;

public class LineDelegate extends RectDelegate {
	
	public LineDelegate(PointList pl)
	{
		super(pl);
		this.type = ROIPlus.ROI_LINE;
	}
	
	@Override
	public void paintLinesForPointList(Graphics2D g2, PointList transformedList, int mode, Point cursorLocation)
	{
		if(transformedList.size() == 2)
			g2.drawLine(transformedList.get(0).x, transformedList.get(0).y, transformedList.get(1).x, transformedList.get(1).y);
		else if(transformedList.size() == 1 && mode == MODE_CREATE)
		{
			g2.drawLine(transformedList.get(0).x, transformedList.get(0).y, cursorLocation.x, cursorLocation.y);
		}
	}
	
	@Override
	public boolean isLineType()
	{
		return true;
	}
	
	@Override
	public boolean pointIsInRoi(Point p)
	{
		if(this.size() < 2)
			return false;
		rect = this.editablePoints.getBounds();
		if(rect.width < 5)
			rect.width = 5;
		if(rect.height < 5)
			rect.height = 5;
		return rect.contains(p);
	}
}

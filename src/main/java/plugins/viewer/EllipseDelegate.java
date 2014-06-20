package plugins.viewer;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

public class EllipseDelegate extends RectDelegate {
	
	public EllipseDelegate(PointList pl)
	{
		super(pl);
		this.type = ROIPlus.ROI_ELLIPSE;
	}
	
	@Override
	public void paintLinesForPointList(Graphics2D g2, PointList transformedList, int mode, Point cursorLocation)
	{
		if(transformedList.size() == 2)
		{
			this.rect = transformedList.getBounds();
			g2.drawOval(rect.x, rect.y, rect.width, rect.height);
		}
		else if(transformedList.size() == 1 && mode == MODE_CREATE)
		{
			this.rect = new Rectangle(transformedList.get(0).x, transformedList.get(0).y, cursorLocation.x - transformedList.get(0).x, cursorLocation.y - transformedList.get(0).y);
			makePositive(this.rect);
			g2.drawOval(rect.x, rect.y, rect.width, rect.height);
		}
	}
	
}

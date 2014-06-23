package plugins.viewer;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Graphics2D;
import java.awt.Point;

public class PLineDelegate extends PGonDelegate {
	
	public PLineDelegate(PointList pl)
	{
		super(pl);
		this.type = ROIPlus.ROI_POLYLINE;
	}
	
	@Override
	public boolean isLineType()
	{
		return true;
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
	}
	
}

package jex.arrayView;

import java.awt.Point;
import java.awt.Rectangle;

public interface ImageDisplayController {
	
	public void clickedPoint(Point p);
	
	public void rightClickedPoint(Point p);
	
	public void extendedRectangle(Rectangle r);
	
}

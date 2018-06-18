package image.roi;

import Database.DBObjects.JEXData;
import Database.Definition.Type;
import Database.SingleUserDatabase.xml.XDataSingle;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import miscellaneous.CSVList;
import miscellaneous.SSVList;

public class Trajectory extends XDataSingle {
	
	private static final long serialVersionUID = 1L;
	
	// class interpolated variables
	private Point iPoint = null;
	private double iRadius = 0;
	private double iCertainty = 1;
	private boolean isActive = true;
	
	private int cellRadius = 0;
	private int interpolWindow = 1;
	private int maxDispTime = 1;
	
	private TreeMap<Integer,Point> pmap;
	
	/**
	 * Create a new Trajectory
	 */
	public Trajectory()
	{
		super(new Type(JEXData.ROI, "Trajectory"));
		
		pmap = new TreeMap<Integer,Point>();
	}
	
	/**
	 * Create a useable trajectory
	 * 
	 * @param cellRadius
	 * @param maxDissapearanceTime
	 */
	public Trajectory(int cellRadius, int maxDissapearanceTime)
	{
		this();
		this.setMaxDissapearanceTime(maxDissapearanceTime);
		this.setCellRadius(cellRadius);
		this.setInterpolationWindow(5);
		pmap = new TreeMap<Integer,Point>();
	}
	
	/**
	 * Create a new Trajectory of length LENGTH, filled with empty points
	 * 
	 * @param length
	 * @param cellRadius
	 * @param maxDissapearanceTime
	 */
	public Trajectory(int length, int cellRadius, int maxDissapearanceTime)
	{
		this();
		this.setMaxDissapearanceTime(maxDissapearanceTime);
		this.setCellRadius(cellRadius);
		this.setInterpolationWindow(5);
		
		pmap = new TreeMap<Integer,Point>();
		for (int k = 0; k < length; k++)
		{
			Point p = new Point(-1, -1);
			pmap.put(new Integer(k), p);
		}
	}
	
	/**
	 * Create a new Trajectory from a point map
	 * 
	 * @param pmap
	 */
	public Trajectory(TreeMap<Integer,Point> pmap)
	{
		this();
		this.pmap = pmap;
	}
	
	/**
	 * Create a new Trajectory from a vector of points
	 * 
	 * @param pointList
	 */
	public Trajectory(Vector<Point> pointList)
	{
		this();
		for (int k = 0, len = pointList.size(); k < len; k++)
		{
			Point p = pointList.get(k);
			pmap.put(new Integer(k), p);
		}
	}
	
	/**
	 * Create a new Trajectory from a PointList object
	 */
	public Trajectory(PointList pl)
	{
		this();
		for(IdPoint p : pl)
		{
			pmap.put(new Integer(p.id), new Point(p.x, p.y));
		}
	}
	
	/**
	 * Create a new Trajectory from a vector of points
	 * 
	 * @param pointList
	 */
	public Trajectory(List<Point> pointList)
	{
		this();
		for (int k = 0, len = pointList.size(); k < len; k++)
		{
			Point p = pointList.get(k);
			pmap.put(new Integer(k), p);
		}
	}
	
	/**
	 * Add a point to the trajectory at frame I
	 * 
	 * @param p
	 * @param i
	 */
	public void add(Point p, int i)
	{
		pmap.put(new Integer(i), p);
	}
	
	/**
	 * Remove all points for frames further than LENGTH
	 * 
	 * @param length
	 */
	public void trim(int length)
	{
		
		if(pmap.size() == 0)
			return;
		if(pmap.lastKey() == null)
			return;
		while (pmap.size() > 0 && pmap.lastKey() >= length)
		{
			pmap.remove(pmap.lastKey());
		}
	}
	
	public void trim(int min, int max)
	{
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			if(key < min || key >= max)
				pmap.remove(key);
		}
	}
	
	/**
	 * Extract a sub-section of the track in a new track
	 * 
	 * @param min
	 * @param max
	 * @return extracted subset trajectory
	 */
	public Trajectory extract(int min, int max)
	{
		Trajectory result = new Trajectory(this.cellRadius, this.maxDispTime);
		
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			if(key >= min || key < max)
				result.add(pmap.get(key), key);
		}
		
		return result;
	}
	
	/**
	 * Get the first point in the trajectory
	 * 
	 * @return first point in the traj
	 */
	public Point getFirst()
	{
		return pmap.get(pmap.firstKey());
	}
	
	/**
	 * Get the next point in the trajectory
	 * 
	 * @param index
	 * @return next point in the traj
	 */
	public Point getNext(Integer index)
	{
		Integer max = pmap.lastKey();
		index = index + 1;
		while (index <= max)
		{
			Point p = pmap.get(index);
			if(p != null)
				return p;
			index = index + 1;
		}
		return null;
	}
	
	/**
	 * Get the next point in the trajectory
	 * 
	 * @param index
	 * @return next point in the traj
	 */
	public Point getPrevious(Integer index)
	{
		Integer min = pmap.firstKey();
		index = index - 1;
		while (index >= min)
		{
			Point p = pmap.get(index);
			if(p != null)
				return p;
			index = index - 1;
		}
		return null;
	}
	
	/**
	 * Return true if the trajectory holds a point later than P
	 * 
	 * @param p
	 * @return
	 */
	public boolean hasNext(Integer index)
	{
		Point result = this.getNext(index);
		if(result == null)
			return false;
		return true;
	}
	
	/**
	 * Return last point in the trajectory
	 * 
	 * @return last point in the traj
	 */
	public Point getLast()
	{
		if(pmap.size() == 0)
			return null;
		return pmap.get(pmap.lastKey());
	}
	
	/**
	 * Get the point at frame FRAME
	 * 
	 * @param frame
	 * @return point at framce FRAME
	 */
	public Point getPoint(int frame)
	{
		return pmap.get(new Integer(frame));
	}
	
	/**
	 * Get closest point with frame number higher or equal to FRAME
	 * 
	 * @param frame
	 * @return closest point to frame FRAME
	 */
	public Point getClosest(int frame)
	{
		return getNext(frame - 1);
	}
	
	/**
	 * Get closest point with frame number lower or equal to FRAME
	 * 
	 * @param frame
	 * @return closest point to frame FRAME
	 */
	public Point getClosestDecreasing(int frame)
	{
		return getPrevious(frame + 1);
	}
	
	/**
	 * Get closest point with frame number higher or equal to FRAME
	 * 
	 * @param frame
	 * @return closest point to frame FRAME
	 */
	public int getClosestFrame(int frame)
	{
		return next(frame - 1);
	}
	
	/**
	 * Get closest point with frame number lower or equal to FRAME
	 * 
	 * @param frame
	 * @return closest point to frame FRAME
	 */
	public int getClosestDecreasingFrame(int frame)
	{
		return previous(frame + 1);
	}
	
	/**
	 * Get the list of points of this trajectory in order
	 * 
	 * @return list of points
	 */
	public List<Point> getPoints()
	{
		Collection<Point> points = pmap.values();
		List<Point> result = new ArrayList<Point>(0);
		for (Point p : points)
		{
			result.add(p);
		}
		return result;
	}
	
	/**
	 * Get all the points of the trajectory until frame LENGTH
	 * 
	 * @param length
	 * @return sorted list of points
	 */
	public List<Point> getPoints(int length)
	{
		SortedMap<Integer,Point> subMap = pmap.subMap(0, new Integer(length));
		Collection<Point> points = subMap.values();
		List<Point> result = new ArrayList<Point>(0);
		for (Point p : points)
		{
			result.add(p);
		}
		return result;
	}
	
	/**
	 * Get all the points of the trajectory after frame LENGTH
	 * 
	 * @param length
	 * @return collection of points
	 */
	public List<Point> getPointsAfter(int length)
	{
		SortedMap<Integer,Point> subMap = pmap.tailMap(new Integer(length));
		Collection<Point> points = subMap.values();
		List<Point> result = new ArrayList<Point>(0);
		for (Point p : points)
		{
			result.add(p);
		}
		return result;
	}
	
	/**
	 * Get all the points in the trajectory between frame START, and frame END (included)
	 * 
	 * @param start
	 * @param end
	 * @return list of points contained between the set bounds
	 */
	public List<Point> getPoints(int start, int end)
	{
		SortedMap<Integer,Point> subMap = pmap.subMap(new Integer(start), new Integer(end));
		Collection<Point> points = subMap.values();
		List<Point> result = new ArrayList<Point>(0);
		for (Point p : points)
		{
			result.add(p);
		}
		return result;
	}
	
	/**
	 * Get all the points in the trajectory between frame START, and frame END (included)
	 * 
	 * @param start
	 * @param end
	 * @return list of points contained between the set bounds
	 */
	public List<Integer> getFrames(int start, int end)
	{
		Set<Integer> keys = pmap.keySet();
		List<Integer> result = new ArrayList<Integer>(0);
		for (Integer key : keys)
		{
			if(key >= start || key < end)
				result.add(key);
		}
		return result;
	}
	
	/**
	 * Get all the points in the trajectory
	 * 
	 * @return list of points contained between the set bounds
	 */
	public List<Integer> getFrames()
	{
		Set<Integer> keys = pmap.keySet();
		List<Integer> result = new ArrayList<Integer>(0);
		for (Integer key : keys)
		{
			result.add(key);
		}
		return result;
	}
	
	/**
	 * Returns the current status of the trajectory
	 * 
	 * @return is track active
	 */
	public boolean isActive()
	{
		boolean result = isActive;
		return result;
	}
	
	/**
	 * Returns the status of the trajectory at frame INDEX
	 * 
	 * @param index
	 * @return is track active after frame INDEX
	 */
	public boolean isActive(int index)
	{
		if(pmap.size() == 0)
			return false;
		if(pmap.lastKey() == null)
			return false;
		Integer last = pmap.lastKey();
		if(last >= index - 3)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Has the trajectory started yet?
	 * 
	 * @return true if the trajectoy has started already
	 */
	public boolean hasStarted()
	{
		Point p = this.getFirst();
		if(p != null)
			return true;
		return false;
	}
	
	/**
	 * Has the trajectory started at frame LENGTH
	 * 
	 * @return return true if the trajectory has started at frame INDEX
	 */
	public boolean hasStarted(int index)
	{
		Integer first = pmap.firstKey();
		if(first < length)
			return true;
		return false;
	}
	
	/**
	 * Does this trajectory have a characteristic named NAME of a value VALUE
	 * 
	 * @param name
	 * @param value
	 * @return return true if the trajectory bud has a field name with value value
	 */
	public boolean hasField(String name, String value)
	{
		String v = this.getAttributeValue(name);
		if(value.equals(v))
			return true;
		return false;
	}
	
	/**
	 * Length of this trajectory, absolute
	 * 
	 * @return last frame of this trajectory
	 */
	public int length()
	{
		if(pmap == null || pmap.size() == 0)
			return 0;
		Integer last = pmap.lastKey();
		return last;
	}
	
	/**
	 * Return the number of points saved in this trajectory
	 * 
	 * @return nb of points in the trajectory
	 */
	public int nbPoints()
	{
		if(pmap == null || pmap.size() == 0)
			return 0;
		return pmap.size();
	}
	
	/**
	 * Get frame value of first frame
	 * 
	 * @return frame index of first frame
	 */
	public int initial()
	{
		if(pmap.size() == 0)
			return -1;
		return pmap.firstKey();
	}
	
	/**
	 * Get frame value of next frame
	 * 
	 * @return frame index of next frame
	 */
	public int next(int index)
	{
		if(pmap.size() == 0)
			return -1;
		Integer max = pmap.lastKey();
		index = index + 1;
		while (index <= max)
		{
			Point p = pmap.get(index);
			if(p != null)
				return index;
			index = index + 1;
		}
		return -1;
	}
	
	/**
	 * Get frame value of next frame
	 * 
	 * @return frame index of next frame
	 */
	public int previous(int index)
	{
		if(pmap.size() == 0)
			return -1;
		Integer min = pmap.firstKey();
		index = index - 1;
		while (index >= min)
		{
			Point p = pmap.get(index);
			if(p != null)
				return index;
			index = index - 1;
		}
		return -1;
	}
	
	/**
	 * set the cell radius
	 * 
	 * @param radius
	 */
	public void setCellRadius(int radius)
	{
		this.cellRadius = radius;
		this.setAttribute("CellRadius", "" + radius);
	}
	
	/**
	 * Get the cell radius
	 * 
	 * @return the cell radius
	 */
	public int getCellRadius()
	{
		return cellRadius;
	}
	
	/**
	 * Set the interpolation sliding window
	 * 
	 * @param i
	 */
	public void setInterpolationWindow(int i)
	{
		this.setAttribute("InterpolationWindow", "" + i);
		this.interpolWindow = i;
	}
	
	/**
	 * Get the width of the sliding window
	 * 
	 * @return
	 */
	public int getInterpolationWindow()
	{
		return interpolWindow;
	}
	
	/**
	 * Set the maximum number of frames to keep a track active after last recorded point
	 * 
	 * @param maxDissapearanceTime
	 */
	public void setMaxDissapearanceTime(int maxDissapearanceTime)
	{
		this.setAttribute("MaxDissapearanceTime", "" + maxDissapearanceTime);
		this.maxDispTime = maxDissapearanceTime;
	}
	
	/**
	 * Get the maximum number of frames to keep a track active after last recorded point
	 * 
	 * @return
	 */
	public int getMaxDissapearanceTime()
	{
		return maxDispTime;
	}
	
	/**
	 * Calculate characteristic parameters of the track upto frame FRAME
	 * 
	 * @param frame
	 */
	public void interpolate(int frame)
	{
		// get some variables
		// int maxDissapearanceTime = this.getMaxDissapearanceTime();
		// int cellRadius = this.getCellRadius();
		int slidingWindow = this.getInterpolationWindow();
		
		// Initiate variables that will be created by this method
		iPoint = new Point(-1, -1);
		iRadius = this.getCellRadius();
		iCertainty = 1;
		int frames = 0;
		
		// Collect the points to calculate on
		if(slidingWindow == -1)
			slidingWindow = frame;
		List<Integer> frameList = getFrames(frame - slidingWindow, frame);
		if(frameList == null)
			return;
		if(frameList.size() == 0)
		{
			return;
		}
		
		double displacementX = 0;
		double displacementY = 0;
		
		Point first = pmap.get(frameList.get(0));
		Point last = pmap.get(frameList.get(frameList.size() - 1));
		displacementX = last.getX() - first.getX();
		displacementY = last.getY() - first.getY();
		frames = frameList.get(frameList.size() - 1) - frameList.get(0);
		
		int framesUnknown = frame - frameList.get(frameList.size() - 1);
		// System.out.println("   Trajectory ---> frame = "+frame+" lastframe = "+last.getIntField(FRAME)+" framesUnknown = "+framesUnknown);
		int averageXdisp = (frames == 0) ? 0 : (int) (0.5 * framesUnknown * displacementX / frames);
		int averageYdisp = (frames == 0) ? 0 : (int) (0.5 * framesUnknown * displacementY / frames);
		
		iPoint = last;
		// System.out.println("   Trajectory ---> last point x="+last.X()+" y="+last.Y()+"... average velocity dx="+averageXdisp+" dy="+averageYdisp);
		// last.translate(averageXdisp, averageYdisp);
		iPoint = new Point(iPoint.x + averageXdisp, iPoint.y + averageYdisp);
		// System.out.println("   Trajectory ---> interpolated point x="+iPoint.x+" y="+iPoint.y);
		// iRadius = iRadius ;
	}
	
	public Point interpolatedPoint()
	{
		Point result = (Point) iPoint.clone();
		return result;
	}
	
	public double interpolatedRadius()
	{
		double result = iRadius;
		return result;
	}
	
	public double interpolatedCertainty()
	{
		double result = iCertainty;
		return result;
	}
	
	/**
	 * Print the current trajectory with the interpolated point appended
	 */
	public void print()
	{
		String result = "";
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			Point p = pmap.get(key);
			result = result + " " + key + "(" + p.getX() + "," + p.getY() + ")";
		}
		if(iPoint != null)
			result = result + " --ext-- " + "(" + iPoint.x + "," + iPoint.y + ")";
		System.out.println("   Trajectory ---> Track is " + result);
	}
	
	@Override
	public String toString()
	{
		String result = "";
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			Point p = pmap.get(key);
			if(p.getX() == -1)
			{
				result = result + "(-,-)";
			}
			else
			{
				result = result + "(" + p.getX() + "," + p.getY() + ")";
			}
		}
		
		return result;
	}
	
	/**
	 * Translate the trajectory by minus the coordinates of point p
	 * 
	 * @param p
	 * @return
	 */
	public Trajectory translate(Point p)
	{
		double dX = p.getX();
		double dY = p.getY();
		
		TreeMap<Integer,Point> newMap = new TreeMap<Integer,Point>();
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			Point thisP = pmap.get(key);
			
			Point transP = new Point();
			transP.setLocation(thisP.getX() - dX, thisP.getY() - dY);
			
			newMap.put(key, transP);
		}
		
		Trajectory ret = new Trajectory(newMap);
		return ret;
	}
	
	/**
	 * Return the bounding rectangle of this trajectory
	 * 
	 * @return
	 */
	public Rectangle getBounds()
	{
		int minX = 0;
		int minY = 0;
		int maxX = 0;
		int maxY = 0;
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			Point p0 = pmap.get(key);
			minX = (minX > p0.getX()) ? (int) p0.getX() : minX;
			maxX = (maxX < p0.getX()) ? (int) p0.getX() : maxX;
			minY = (minY > p0.getY()) ? (int) p0.getY() : minY;
			maxY = (maxY < p0.getY()) ? (int) p0.getY() : maxY;
		}
		double thisDX = (maxX - minX);
		double thisDY = (maxY - minY);
		
		Rectangle result = new Rectangle(minX, minY, (int) thisDX, (int) thisDY);
		return result;
	}
	
	// ----------------------------------------------------------
	// ------ CALCULATION FUNCTIONS -----------------------------
	// ----------------------------------------------------------
	public double meanDisp;
	public double totalDisp;
	public double meanAngle;
	public double totalAngle;
	public double ratioLength;
	public double totalLength;
	public double effectiveLenth;
	public int length;
	public Vector<Double> Displacements;
	public Vector<Double> Angles;
	public Vector<Double> Wangles;
	public double micronPerPixel = 1;
	public double secondPerFrame = 1;
	
	// private int nbFrames = 1;
	
	/**
	 * Calculate characteristics of the trajectory, traditional style
	 */
	public void calculate()
	{
		meanDisp = 0;
		totalDisp = 0;
		meanAngle = 0;
		totalAngle = 0;
		ratioLength = 1;
		effectiveLenth = 0;
		
		Displacements = new Vector<Double>(0);
		Angles = new Vector<Double>(0);
		Wangles = new Vector<Double>(0);
		Point firstPoint = null;
		Point lastPoint = null;
		// int lengthAngle = 0;
		length = 0;
		int frames = 0;
		
		int thisFrame = 0;
		int lastFrame = 0;
		Point p1 = null;
		Point p2 = null;
		
		if(pmap.size() == 1)
		{
			meanDisp = 0;
			meanAngle = 0;
			return;
		}
		
		int index = pmap.firstKey();
		while (index < pmap.size())
		{
			// get the next point
			p1 = pmap.get(index);
			thisFrame = index;
			
			// If this is the first time step
			if(firstPoint == null)
			{
				firstPoint = new Point(p1.x, p1.y);
				p2 = p1;
				lastFrame = thisFrame;
				index = this.next(index);
				continue;
			}
			
			int deltaFrames = thisFrame - lastFrame;
			length = length + deltaFrames;
			double distance = micronPerPixel * distance(p1, p2);
			double velocity = distance / (deltaFrames * secondPerFrame);
			double angleDeg = angle(p2, p1);
			
			Displacements.add(velocity);
			if(!((p1.x == p2.x) && (p1.y == p2.y)))
			{
				Angles.add(angleDeg);
				Wangles.add(velocity);
			}
			
			// System.out.println("Particule traveled "+distance+" pixels over "+deltaFrames+" frames at an angle of "+angleDeg);
			totalDisp = totalDisp + distance;
			
			// save point as potential last point
			lastPoint = new Point(p1.x, p1.y);
			frames = frames + deltaFrames;
			p2 = p1;
			lastFrame = thisFrame;
			index = this.next(index);
		}
		
		meanDisp = micronPerPixel * totalDisp / (length * secondPerFrame);
		meanAngle = totalAngle / length;
		if(firstPoint != null && lastPoint != null)
			totalAngle = angle(lastPoint, firstPoint);
		
		if((firstPoint == null) || (lastPoint == null))
			effectiveLenth = 0;
		else
		{
			effectiveLenth = micronPerPixel * distance(firstPoint, lastPoint);
			meanAngle = angle(p2, p1);
		}
		ratioLength = (totalDisp == 0) ? 0 : effectiveLenth / totalDisp;
		
		// System.out.println("Particule traveled on average "+meanDisp+" pixels per frame at an angle of "+meanAngle);
		// System.out.println("Particule traveled "+effectiveLenth+" pixels for a total travel of "+totalLength);
	}
	
	// /**
	// * Calculate characteristics of the trajectory, high-throughput style
	// * @param deltaFrame
	// */
	// public void calculate(int deltaFrame){
	// // calculate simple variables
	// calculateValues();
	//
	// meanDisp = 0;
	// meanAngle = 0;
	// Displacements = new Vector<Double>(0);
	// Angles = new Vector<Double>(0);
	// Wangles = new Vector<Double>(0);
	//
	// Set<Integer> keys = pmap.keySet();
	// for (Integer key: keys){
	// // first point
	// Point p1 = pmap.get(key);
	// int thisFrame = key;
	//
	// // next point
	// int otherFrame = this.getClosestFrame(key+deltaFrame);
	// Point p2 = pmap.get(new Integer(otherFrame));
	//
	// int frames = otherFrame - thisFrame;
	// if (frames == 0)
	// continue;
	// double distance = (double)micronPerPixel*distance(p1,p2);
	// double velocity = distance/((double)frames*(double)secondPerFrame);
	// double angleDeg = angle(p1,p2);
	//
	// Displacements.add(velocity);
	// if (!((p1.x==p2.x)&&(p1.y==p2.y))){
	// Angles.add(angleDeg);
	// Wangles.add(velocity);
	// //System.out.println("Particule traveled "+distance+" pixels over "+frames+" frames at an angle of "+angleDeg);
	// }
	// }
	//
	// Vector<Double> dispStat =
	// Calculator.calculateDoubleStatistics(Displacements);
	// Vector<Double> angleStat = Calculator.calculateDoubleStatistics(Angles);
	// // Vector wangleStat = Calculator.calculateDoubleStatistics(Wangles);
	//
	// meanDisp = (Double)dispStat.get(2);
	// meanAngle = (Double)angleStat.get(2);
	//
	// //System.out.println("Particule traveled on average "+meanDisp+" pixels per frame at an angle of "+meanAngle);
	// //System.out.println("Particule traveled "+effectiveLenth+" pixels for a total travel of "+totalLength);
	// }
	
	// public void calculateValues(){
	// totalDisp = 0;
	// ratioLength = 1;
	// effectiveLenth = 0;
	// double totalAngle = 0;
	// int lengthAngle = 0;
	//
	// Point firstPoint = null;
	// Point lastPoint = null;
	//
	// int frames = 0;
	// int index = 0;
	// int thisFrame = 0;
	// int lastFrame = 0;
	// Point p1 = null;
	// Point p2 = null;
	//
	// if (pmap.size() == 1){return;}
	// while (index != -1){
	// // get the next point
	// // Point p = pmap.get(new Integer(index));
	// thisFrame = index;
	//
	// if (firstPoint == null){
	// firstPoint = new Point(p1.x,p1.y);
	// p2 = p1;
	// lastFrame = thisFrame;
	// index = this.next(index);
	// continue;
	// }
	//
	// int deltaFrames = thisFrame - lastFrame;
	// double distance = (double)micronPerPixel*distance(p1,p2);
	// // double velocity =
	// distance/((double)deltaFrames*(double)secondPerFrame);
	// double angleDeg = angle(p2,p1);
	//
	// // save point as potential last point
	// lastPoint = new Point(p1.x, p1.y);
	// frames = frames + deltaFrames;
	//
	// if (!((p1.x==p2.x)&&(p1.y==p2.y))){
	// totalAngle = totalAngle + angleDeg;
	// lengthAngle ++;
	// }
	//
	// //System.out.println("Particule traveled "+distance+" pixels over "+skip+" frames at an angle of "+angleDeg);
	// totalDisp = totalDisp + distance;
	//
	// p2=p1;
	// lastPoint = p2;
	// lastFrame = thisFrame;
	// index = this.next(index);
	// }
	//
	// meanDisp = totalDisp/(double)length;
	// meanAngle = totalAngle/(double)lengthAngle;
	//
	// if ((firstPoint == null)||(lastPoint == null)) effectiveLenth = 0;
	// else effectiveLenth =
	// (double)micronPerPixel*distance(firstPoint,lastPoint);
	//
	// ratioLength = (totalLength == 0) ? 0 : effectiveLenth/totalLength;
	// }
	
	public double meanVelocity()
	{
		// double meanDisp = 0;
		double totalDisp = 0;
		int length = 0;
		Point p1 = null;
		Point p2 = null;
		
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			p1 = pmap.get(key);
			
			int next = this.next(key);
			if(next == -1)
				continue;
			p2 = pmap.get(next);
			
			double distance = distance(p1, p2);
			totalDisp = totalDisp + distance;
			length = length + (next - key);
		}
		double effectiveDisp = distance(pmap.get(pmap.firstKey()), p2);
		double avergaeVelocity = effectiveDisp / length;
		return avergaeVelocity;
	}
	
	// Find the distance between two points
	public double distance(Point p1, Point p2)
	{
		double currentDistance = Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
		return currentDistance;
	}
	
	// Find the angle of a segment
	public double angle(Point p1, Point p2)
	{
		double y = p1.y - p2.y;
		double x = p1.x - p2.x;
		double resultRad = Math.atan2(y, x);
		double result = Math.toDegrees(resultRad);
		// System.out.println("Particule traveled dx="+x+" and dy="+y+" and resultRad="+resultRad+" and result="+result);
		return result;
	}
	
	/**
	 * Set scaling in microns per pixel
	 * 
	 * @param mpp
	 */
	public void setMPP(double mpp)
	{
		this.micronPerPixel = mpp;
	}
	
	/**
	 * Set the frame rate in seconds per frame
	 * 
	 * @param spf
	 */
	public void setSPF(double spf)
	{
		this.secondPerFrame = spf;
	}
	
	// Get and Set functions
	public double getMeanDisp()
	{
		double result = meanDisp;
		return result;
	}
	
	public double getTotalDisp()
	{
		double result = totalDisp;
		return result;
	}
	
	public double getRatioLength()
	{
		double result = ratioLength;
		return result;
	}
	
	public double getAngleDisp()
	{
		double result = meanAngle;
		return result;
	}
	
	public double getTotalAngle()
	{
		return totalAngle;
	}
	
	@SuppressWarnings("unchecked")
	public Vector<Double> getDispVect()
	{
		Vector<Double> result = (Vector<Double>) Displacements.clone();
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public Vector<Double> getAngleVect()
	{
		Vector<Double> result = (Vector<Double>) Angles.clone();
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public Vector<Double> getWeighedAngleVector()
	{
		Vector<Double> result = (Vector<Double>) Wangles.clone();
		return result;
	}
	
	/**
	 * Get a list of vectors extracted form this trajectory
	 * 
	 * @param deltaFrame
	 */
	public VectSet getVectors(int deltaFrame, boolean average)
	{
		VectSet result = new VectSet();
		
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			Point p1 = pmap.get(key);
			
			int next = this.next(key);
			if(next == -1)
				continue;
			Point p2 = pmap.get(next);
			
			double dx = (p2.getX() - p1.getX());
			double dy = (p2.getY() - p1.getY());
			Vect v = new Vect(dx, dy);
			
			if(average)
				v.multiply((next - key));
			result.add(v);
		}
		
		return result;
	}
	
	// ----------------------------------------------------------
	// ------ I/O FUNCTIONS -----------------------------
	// ----------------------------------------------------------
	
	// Temporary XML stuff
	public void update()
	{
		this.removeContent();
		this.addMeta("Points", this.pmapList());
		this.addMeta("Number of Points", "" + this.pmapList().length());
		this.addMeta("CellRadius", "" + this.cellRadius);
		this.addMeta("InterpolationWindow", "" + this.interpolWindow);
		this.addMeta("MaxDispTime", "" + this.maxDispTime);
	}
	
	public void load()
	{
		String pointStr = this.getAtt("Points");
		SSVList polygon = new SSVList(pointStr);
		pmap = new TreeMap<Integer,Point>();
		Iterator<String> itr = polygon.iterator();
		while (itr.hasNext())
		{
			CSVList pt = new CSVList(itr.next());
			if(pt.get(0).equals("") || pt.get(1).equals("") || pt.get(2).equals(""))
				continue;
			int index = (new Integer(pt.get(0))).intValue();
			int x = (new Integer(pt.get(1))).intValue();
			int y = (new Integer(pt.get(2))).intValue();
			Point p = new Point(x, y);
			pmap.put(new Integer(index), p);
		}
		
		this.cellRadius = Integer.parseInt(this.getAtt("CellRadius"));
		this.interpolWindow = Integer.parseInt(this.getAtt("InterpolationWindow"));
		this.maxDispTime = Integer.parseInt(this.getAtt("MaxDispTime"));
	}
	
	public String pmapList()
	{
		SSVList polygonPts = new SSVList();
		
		Set<Integer> keys = pmap.keySet();
		for (Integer key : keys)
		{
			Point p = pmap.get(key);
			String pt = "" + key + "," + p.x + "," + p.y;
			polygonPts.add(pt);
		}
		
		String result = polygonPts.toString();
		return result;
	}
	
	public static Trajectory fromXDataSingle(XDataSingle ds)
	{
		Trajectory result = new Trajectory();
		result.cellRadius = Integer.parseInt(ds.getAtt("CellRadius"));
		result.interpolWindow = Integer.parseInt(ds.getAtt("InterpolationWindow"));
		result.maxDispTime = Integer.parseInt(ds.getAtt("MaxDispTime"));
		
		String pointStr = ds.getAtt("Points");
		SSVList polygon = new SSVList(pointStr);
		Iterator<String> itr = polygon.iterator();
		while (itr.hasNext())
		{
			CSVList pt = new CSVList(itr.next());
			if(pt.get(0).equals("") || pt.get(1).equals("") || pt.get(2).equals(""))
			{
				continue;
			}
			int index = (new Integer(pt.get(0))).intValue();
			int x = (new Integer(pt.get(1))).intValue();
			int y = (new Integer(pt.get(2))).intValue();
			Point p = new Point(x, y);
			result.add(p, new Integer(index));
		}
		return result;
	}
	
}

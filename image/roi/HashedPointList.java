package image.roi;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import logs.Logs;
import miscellaneous.Pair;

public class HashedPointList implements Comparator<Pair<IdPoint,IdPoint>> {
	
	public TreeMap<Integer,PointList> xHash = new TreeMap<Integer,PointList>();
	public TreeMap<Integer,PointList> yHash = new TreeMap<Integer,PointList>();
	public TreeMap<Integer,IdPoint> idHash = new TreeMap<Integer,IdPoint>();
	public PointList points;
	
	public HashedPointList()
	{
		this.points = new PointList();
	}
	
	public HashedPointList(PointList points)
	{
		this.points = points;
		PointList hashedPoints;
		for (IdPoint p : points)
		{
			// Add to xHash
			hashedPoints = this.xHash.get(p.x);
			if(hashedPoints == null)
			{
				hashedPoints = new PointList();
			}
			hashedPoints.add(p);
			this.xHash.put(p.x, hashedPoints);
			
			// Add to yHash
			hashedPoints = this.yHash.get(p.y);
			if(hashedPoints == null)
			{
				hashedPoints = new PointList();
			}
			hashedPoints.add(p);
			this.yHash.put(p.y, hashedPoints);
			
			// Add to idHash
			this.idHash.put(p.id, p);
		}
	}
	
	/**
	 * radius is actually a square region extending radius pixels around the specified point. Returns the nearest point within that radius.
	 * 
	 * @param p
	 *            (point around which to search)
	 * @param radius
	 *            (radius of region to search)
	 * @param squareRegion
	 *            (search in a square-shaped region or a circular region)
	 * @return nearest point in that range (ties are broken by which is found first)
	 */
	public IdPoint getNearestInRange(Point p, double radius, boolean squareRegion)
	{
		PointList xMatches = getMatches(p, this.xHash, (int) radius, true);
		if(xMatches == null)
		{
			return null;
		}
		PointList yMatches = getMatches(p, this.yHash, (int) radius, false);
		if(yMatches == null)
		{
			return null;
		}
		
		// Perform the intersection of the match lists
		xMatches.retainAll(yMatches);
		if(xMatches.size() == 0)
		{
			return null;
		}
		if(xMatches.size() == 1)
		{
			return xMatches.get(0);
		}
		
		// Calculate the 'distance measure' from p for each remaining point
		double[] distance = new double[xMatches.size()];
		for (int i = 0; i < distance.length; i++)
		{
			distance[i] = Math.pow((xMatches.get(i).x - p.x), 2) + Math.pow((xMatches.get(i).y - p.y), 2);
		}
		
		// Find the min distance and return a new point with those coordinates
		// Set the minimum radius to be that of the corners of the searched
		// square region
		// or the circle that fits within that square
		double min = radius * radius;
		if(squareRegion)
		{
			min = min * Math.sqrt(2);
		}
		for (int i = 0; i < distance.length; i++)
		{
			min = Math.min(min, distance[i]);
		}
		for (int i = 0; i < distance.length; i++)
		{
			if(distance[i] == min)
			{
				return xMatches.get(i);
			}
		}
		// Logs.log("No nearest point found", 0, this);
		return null;
	}
	
	public static List<Pair<IdPoint,IdPoint>> getNearestNeighbors(PointList l1, PointList l2, double radius, boolean squareRegion)
	{
		List<Pair<IdPoint,IdPoint>> ret = new Vector<Pair<IdPoint,IdPoint>>();
		
		HashedPointList sl2 = new HashedPointList(l2);
		for (IdPoint p : l1)
		{
			IdPoint nearest = sl2.getNearestInRange(p, radius, squareRegion);
			ret.add(new Pair<IdPoint,IdPoint>(p, nearest));
		}
		Logs.log("getNearestNeighbors returned " + ret.size() + " pairs for a list of " + l1.size() + " points, which is a difference of " + (ret.size() - l1.size()), 0, HashedPointList.class);
		return ret;
	}
	
	/**
	 * Returns the neighboring points within a specified radius (or square with specified edge length) along with the distance from each of those points.
	 * 
	 * @param p
	 * @param radius
	 * @param squareRegion
	 * @return
	 */
	public Vector<Pair<IdPoint,Double>> getNeighbors(IdPoint p, double radius, boolean squareRegion)
	{
		Vector<Pair<IdPoint,Double>> ret = new Vector<Pair<IdPoint,Double>>();
		
		PointList xMatches = getMatches(p, this.xHash, (int) radius, true);
		if(xMatches == null)
		{
			return null;
		}
		PointList yMatches = getMatches(p, this.yHash, (int) radius, false);
		if(yMatches == null)
		{
			return null;
		}
		
		// Perform the intersection of the match lists
		xMatches.retainAll(yMatches);
		if(xMatches.size() == 0)
		{
			return null;
		}
		
		// Calculate the 'distance measure' from p for each remaining point
		IdPoint temp;
		for (int i = 0; i < xMatches.size(); i++)
		{
			temp = xMatches.get(i);
			Double dist = Math.sqrt(Math.pow((temp.x - p.x), 2) + Math.pow((temp.y - p.y), 2));
			if(squareRegion || dist <= radius)
			{
				ret.add(new Pair<IdPoint,Double>(temp, Math.sqrt(Math.pow((temp.x - p.x), 2) + Math.pow((temp.y - p.y), 2))));
			}
		}
		if(ret.size() == 0)
		{
			return null;
		}
		return ret;
	}
	
	/**
	 * Returns the neighboring points within a specified radius (or square with specified edge length) along with the distance from each of those points.
	 * 
	 * @param p
	 * @param radius
	 * @param squareRegion
	 * @return
	 */
	public PointList getPointsInRect(Rectangle r)
	{
		PointList xMatches = getPointsInRange(this.xHash, r.x, r.x + r.width);
		if(xMatches == null)
		{
			return null;
		}
		PointList yMatches = getPointsInRange(this.yHash, r.y, r.y + r.height);
		if(yMatches == null)
		{
			return null;
		}
		
		// Perform the intersection of the match lists
		xMatches.retainAll(yMatches);
		if(xMatches.size() == 0)
		{
			return null;
		}
		
		return xMatches;
	}
	
	private static PointList getPointsInRange(TreeMap<Integer,PointList> hash, int min, int max)
	{
		PointList matches = new PointList();
		PointList temp;
		
		for (int i = min; i <= max; i++)
		{
			temp = hash.get(i);
			if(temp != null)
			{
				matches.addAll(temp);
			}
		}
		
		if(matches.size() == 0)
		{
			return null;
		}
		return matches;
	}
	
	private static PointList getMatches(Point p, TreeMap<Integer,PointList> hash, int radius, boolean isX)
	{
		PointList matches = new PointList();
		PointList temp;
		
		// Get the points with the x point in the correct range
		if(isX)
		{
			temp = getPointsInRange(hash, p.x - radius, p.x + radius);
			if(temp != null)
			{
				matches.addAll(temp);
			}
		}
		else
		{
			temp = getPointsInRange(hash, p.y - radius, p.y + radius);
			if(temp != null)
			{
				matches.addAll(temp);
			}
		}
		
		if(matches.size() == 0)
		{
			return null;
		}
		return matches;
	}
	
	public static List<Pair<IdPoint,IdPoint>> filterConflicts(List<Pair<IdPoint,IdPoint>> pairs)
	{
		// Sort the pairs list into ascending order of distance between the
		// points within the pair
		Collections.sort(pairs, new HashedPointList());
		
		// Take the first occurrence of each
		List<Pair<IdPoint,IdPoint>> resolved = new Vector<Pair<IdPoint,IdPoint>>();
		for (Pair<IdPoint,IdPoint> pair : pairs)
		{
			// If this pair connects a point p1 with a point p2 that is already
			// connected to a different point p1
			// (i.e., already exists in the resolved list), eliminate the
			// connection from p1 to p2
			if(hasConflict(resolved, pair))
			{
				pair.p2 = null; // Eliminate the conflicting connection with p2
				// that already exists in a different pair
				resolved.add(pair);
			}
			else
			{
				resolved.add(pair);
			}
		}
		
		// Resort resolved list before returning
		Collections.sort(resolved, new HashedPointList());
		
		return resolved;
	}
	
	private static boolean hasConflict(List<Pair<IdPoint,IdPoint>> pairs, Pair<IdPoint,IdPoint> p)
	{
		if(p.p2 == null)
		{
			return false;
		}
		for (Pair<IdPoint,IdPoint> pair : pairs)
		{
			if(pair.p2 == p.p2)
			{
				return true;
			}
		}
		return false;
	}
	
	public static double distance(Pair<IdPoint,IdPoint> p)
	{
		if(p.size() < 2 || p.p1 == null || p.p2 == null)
		{
			return Double.MAX_VALUE;
		}
		double ret = ((Point) p.p1).distance(p.p2);
		return ret;
	}
	
	public boolean remove(Point p)
	{
		PointList xMatches = this.xHash.get(p.x);
		PointList yMatches = this.yHash.get(p.y);
		return (this.points.remove(p) && xMatches != null && xMatches.remove(p) && yMatches != null && yMatches.remove(p));
	}
	
	@Override
	public int compare(Pair<IdPoint,IdPoint> pair1, Pair<IdPoint,IdPoint> pair2)
	{
		return (int) Math.signum(distance(pair1) - distance(pair2));
	}
	
}

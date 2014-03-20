package function.tracker;

import image.roi.PointList;
import image.roi.Trajectory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class TrackExtend {
	
	public static int EXTEND_TO_CLOSEST = 2;
	public static int EXTEND_TO_MINIMAL = 1;
	
	// class final variables
	private double ADDITION_PENALITY = 70;
	private double ENDING_PENALITY = 70;
	// private final int MAX_SCENARIOS = 500000;
	// private final double RELEASE_CONSTRAINTS = 0.8;
	
	// class options
	private int mode = EXTEND_TO_MINIMAL;
	private double current_Radius_Search = 1;
	// private int radiusSearchMultiplier = 3;
	// private double autorizedAppearanceRatio = 0.15;
	
	// class variables
	public int maxDisplacement = 30;
	public int maxDissapear = 3;
	public double spf = 30;
	public double mpp = 0.3;
	private int currentImage = 0;
	// private int nbOfPointsToAdd = 0;
	
	private PointList pList;
	
	private List<Trajectory> allTrajectories = new ArrayList<Trajectory>(0);
	private List<Trajectory> activeTrajectories = new ArrayList<Trajectory>(0);
	
	public TrackExtend()
	{}
	
	public void setExtensionMode(int mode)
	{
		this.mode = mode;
	}
	
	public void extendWithPoints(PointList pList, int frame)
	{
		this.pList = pList;
		this.currentImage = frame;
		calculatePosition();
	}
	
	public void calculatePosition()
	{
		System.out.println("   Tracker.TrackExtend ---> Phase 3: ****** extending track frame " + currentImage);
		
		System.out.println("   Tracker.TrackExtend ---> Maximum cell displacement is set to " + maxDisplacement);
		ADDITION_PENALITY = Math.max(70, 2 * maxDisplacement);
		ENDING_PENALITY = Math.max(70, 2 * maxDisplacement);
		
		// if this is the first image each point the trajectories are trivial
		if(currentImage == 0)
		{
			System.out.println("   Tracker.TrackExtend ---> Creating initial trajectories");
			allTrajectories = new ArrayList<Trajectory>(0);
			activeTrajectories = new ArrayList<Trajectory>(0);
			
			for (int k = 0, len = pList.size(); k < len; k++)
			{
				Point p = pList.get(k);
				Trajectory trajectory = new Trajectory(maxDisplacement, maxDissapear);
				trajectory.add(p, 0);
				allTrajectories.add(trajectory);
			}
			return;
		}
		
		// Fetch all active trajectories
		activeTrajectories = new ArrayList<Trajectory>(0);
		for (int i = 0, len = allTrajectories.size(); i < len; i++)
		{
			Trajectory trajectory = allTrajectories.get(i);
			if(trajectory.isActive(currentImage))
			{
				trajectory.trim(currentImage);
				trajectory.interpolate(currentImage);
				activeTrajectories.add(trajectory);
			}
		}
		System.out.println("   Tracker.TrackExtend ---> Number of trajectories: " + allTrajectories.size());
		System.out.println("   Tracker.TrackExtend ---> Number of active trajectories: " + activeTrajectories.size());
		
		// get the new point list
		// this.nbOfPointsToAdd = this.pList.size();
		
		// determine the best extension of the tracks
		current_Radius_Search = 1;
		List<Point> minimalExtention = calculateExtention();
		if(minimalExtention == null)
			return;
		System.out.println("   Tracker.TrackExtend ---> Phase 3: minimalExtention found");
		
		// create the new trajectory list
		for (int k = 0, len = minimalExtention.size(); k < len; k++)
		{
			Trajectory trajectory;
			if(k < activeTrajectories.size())
				trajectory = activeTrajectories.get(k);
			else
			{
				trajectory = new Trajectory(maxDisplacement, maxDissapear);
				allTrajectories.add(trajectory);
			}
			Point additionPoint = minimalExtention.get(k);
			if(additionPoint.x == -1)
				continue;
			trajectory.add(additionPoint, currentImage);
		}
	}
	
	/**
	 * Return the trajectories defined
	 * 
	 * @return
	 */
	public List<Trajectory> getTrajectories()
	{
		return this.allTrajectories;
	}
	
	/**
	 * Duplicate a list of points... low level copy
	 * 
	 * @param oldPoints
	 * @return
	 */
	public List<Point> duplicate(List<Point> oldPoints)
	{
		List<Point> newPoints = new ArrayList<Point>(0);
		for (int i = 0, len = oldPoints.size(); i < len; i++)
		{
			Point oldPoint = oldPoints.get(i);
			newPoints.add(new Point(oldPoint.x, oldPoint.y));
		}
		return newPoints;
	}
	
	// ----------------------------------------------------
	// --------- CALCULATION FUNCTIONS --------------------
	// ----------------------------------------------------
	/**
	 * Calculate the extension of the extisting trajectories given a new List of points
	 */
	private List<Point> calculateExtention()
	{
		List<Point> result = null;
		
		if(mode == EXTEND_TO_CLOSEST)
		{
			result = closestExtension(activeTrajectories, pList);
		}
		else if(mode == EXTEND_TO_MINIMAL)
		{
			boolean OVERFLOW = true;
			List<Integer>[] possibilities = null;
			
			while (OVERFLOW)
			{
				// Determine the possible combinations
				System.out.println("   Tracker.TrackExtend ---> determining possible extensions of tracks");
				possibilities = null;
				possibilities = determinePossibleExtensions(activeTrajectories);
				
				// Determine number of possibilities and ask user if he wants to
				// proceed
				long nbPermutations = 1;
				for (List<Integer> l : possibilities)
				{
					if(nbPermutations < 10000000)
					{
						nbPermutations = (l.size() == 0) ? nbPermutations : l.size() * nbPermutations;
					}
				}
				
				System.out.println("   Tracker.TrackExtend ---> found " + nbPermutations + " possible extensions of tracks");
				if(nbPermutations > 200000)
				{
					this.current_Radius_Search = 0.8 * this.current_Radius_Search;
					double radius = current_Radius_Search * maxDisplacement;
					System.out.println("   Tracker.TrackExtend ---> Too many combinations, reducing constraints to " + radius);
				}
				else
				{
					OVERFLOW = false;
				}
			}
			
			// Find the best extension among these
			result = bestExtension(activeTrajectories, possibilities);
		}
		
		return result;
	}
	
	/**
	 * Determine all the possible senarios
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Integer>[] determinePossibleExtensions(List<Trajectory> trajectories)
	{
		List<Integer>[] possibilities = new ArrayList[trajectories.size()];
		for (int i = 0; i < trajectories.size(); i++)
		{
			possibilities[i] = new ArrayList<Integer>(0);
		}
		
		// for each point determine which point it is close to
		for (int indPoint = 0, len = pList.size(); indPoint < len; indPoint++)
		{
			Point p = pList.get(indPoint);
			List<Integer> potentialTrajectories = findTrajectoriesClose(p, trajectories);
			
			// add the point to each trajectory possible extension
			for (Integer i : potentialTrajectories)
			{
				if(possibilities[i] == null)
					possibilities[i] = new ArrayList<Integer>(0);
				// List<Integer> ext = possibilities.get(i);
				possibilities[i].add(indPoint);
			}
		}
		
		// Return the possibility list
		return possibilities;
	}
	
	/**
	 * Find trajectories in the List of current trajectories that are close to point p
	 * 
	 * @param p
	 * @return
	 */
	private List<Integer> findTrajectoriesClose(Point p, List<Trajectory> trajectories)
	{
		List<Integer> result = new ArrayList<Integer>(0);
		List<Double> distances = new ArrayList<Double>(0);
		
		// get the interpolated next point of a track and the radius of
		// potential location
		double minDist = 100000;
		for (int k = 0, len = trajectories.size(); k < len; k++)
		{
			Trajectory trajectory = trajectories.get(k);
			
			// find the distance to expected next point of the trajectory
			Point iPoint = trajectory.interpolatedPoint();
			// double iRadius = current_Radius_Search *
			// trajectory.interpolatedRadius();
			// System.out.println("   Tracker.TrackExtend ---> Distance with track "+k+": "+distance(p,
			// iPoint));
			
			// if the distance is short enough and if the extension is still
			// free
			// if (distance(p, iPoint) < iRadius) result.add(k);
			Double d = distance(p, iPoint);
			double radius = current_Radius_Search * maxDisplacement;
			if(d < minDist)
			{
				minDist = d;
			}
			if(d < radius)
			{
				addPointInSortedList(k, d, result, distances);
				// result.add(k);
				// distances.add(d);
			}
		}
		// Logs.log("Minimum distance = " + minDist,1,this);
		
		// Keep only the 2 closest for computation ease
		while (result.size() > 2)
		{
			result.remove(result.size() - 1);
		}
		
		return result;
	}
	
	/**
	 * Return the most likely senario
	 * 
	 * @param possibleExtensions
	 * @return
	 */
	private List<Point> bestExtension(List<Trajectory> trajectories, List<Integer>[] possibleExtensions)
	{
		int[] compteur = getFirstExtension(possibleExtensions);
		List<Point> bestExtension = null;
		double bestScore = 10 * Math.max(activeTrajectories.size(), 1) * this.ENDING_PENALITY;
		
		// get number of permutations
		long nbPermutations = 1;
		double pourcent = 0;
		for (List<Integer> l : possibleExtensions)
		{
			nbPermutations = (l.size() == 0) ? nbPermutations : l.size() * nbPermutations;
			if(nbPermutations > 2000000000)
			{
				nbPermutations = -1;
				break;
			}
		}
		
		while (compteur != null)
		{
			List<Point> points = getPointList(compteur, possibleExtensions, pList);
			
			if(!isExtensionValid(points))
				continue;
			
			double currentscore = this.scoreExtension(points);
			if(currentscore < bestScore)
			{
				bestExtension = points;
				bestScore = currentscore;
			}
			
			compteur = getNextExtension(compteur, possibleExtensions);
			pourcent = pourcent + 1 / (double) nbPermutations;
		}
		
		return bestExtension;
	}
	
	/**
	 * Get the first extension in this list
	 * 
	 * @param possibleExtensions
	 * @return
	 */
	private int[] getFirstExtension(List<Integer>[] possibleExtensions)
	{
		int[] result = new int[possibleExtensions.length];
		for (int i = 0, len = possibleExtensions.length; i < len; i++)
		{
			List<Integer> l = possibleExtensions[i];
			// result[i]= (l==null) ? 0 : l.size() -1;
			result[i] = (l == null || l.size() == 0) ? -1 : 0;
		}
		return result;
	}
	
	/**
	 * Get the indexes for the next extension
	 * 
	 * @param currenIndeces
	 * @param possibleExtensions
	 * @return
	 */
	private int[] getNextExtension(int[] currenIndeces, List<Integer>[] possibleExtensions)
	{
		int len = possibleExtensions.length;
		int k = len - 1;
		if(k < 0)
			return null;
		
		currenIndeces[k] = currenIndeces[k] + 1;
		
		while (k > 0)
		{
			int lenK = possibleExtensions[k].size();
			if(lenK == 0)
			{
				currenIndeces[k] = -1;
				currenIndeces[k - 1] = currenIndeces[k - 1] + 1;
				k--;
			}
			else if(currenIndeces[k] >= lenK)
			{
				currenIndeces[k] = 0;
				currenIndeces[k - 1] = currenIndeces[k - 1] + 1;
				k--;
			}
			else
			{
				return currenIndeces;
			}
		}
		
		int lenK = possibleExtensions[0].size();
		if(currenIndeces[0] >= lenK)
		{
			return null;
		}
		
		return currenIndeces;
	}
	
	/**
	 * From a list of indexes and a list of list of possbilities get hte list of points
	 * 
	 * @param currentIndexes
	 * @param possibleExtensions
	 * @param points
	 * @return
	 */
	private List<Point> getPointList(int[] currentIndexes, List<Integer>[] possibleExtensions, PointList points)
	{
		List<Point> result = new ArrayList<Point>(0);
		
		Point[] pts = points.toArray();
		for (int i = 0, len = currentIndexes.length; i < len; i++)
		{
			List<Integer> possI = possibleExtensions[i];
			
			if(possI == null)
			{
				result.add(new Point(-1, -1));
				continue;
			}
			
			// int index = currentIndexes[possI.get(i)];
			int possIndex = currentIndexes[i];
			if(possIndex == -1)
			{
				result.add(new Point(-1, -1));
				continue;
			}
			
			int index = possI.get(currentIndexes[i]);
			
			if(index == -1)
			{
				result.add(new Point(-1, -1));
				continue;
			}
			
			Point p = pts[index];
			pts[index] = new Point(-1, -1);
			result.add(p);
		}
		
		for (Point p : pts)
		{
			if(p.x == -1)
				continue;
			result.add(p);
		}
		
		return result;
	}
	
	/**
	 * Determine if a list of points is valid
	 * 
	 * @param points
	 * @return
	 */
	private boolean isExtensionValid(List<Point> points)
	{
		for (Point p : points)
		{
			if(p.x == -1 && p.y == -1)
				continue;
			if(numberOfOccurances(p, points) > 1)
				return false;
		}
		return true;
	}
	
	/**
	 * Return the number of occurances of Point p in point list points
	 * 
	 * @param p
	 * @param points
	 * @return
	 */
	private int numberOfOccurances(Point p, List<Point> points)
	{
		int result = 0;
		for (Point pp : points)
		{
			if(p.equals(pp))
				result++;
		}
		return result;
	}
	
	/**
	 * Add int INDEX in int list INDEXES at a location based on a list of double SCORES such that the list is sorted by increasing score
	 * 
	 * @param index
	 * @param score
	 * @param indexes
	 * @param scores
	 */
	private void addPointInSortedList(Integer index, Double score, List<Integer> indexes, List<Double> scores)
	{
		boolean added = false;
		for (int i = 0, len = indexes.size(); i < len; i++)
		{
			if(score < scores.get(i))
			{
				indexes.add(i, index);
				scores.add(i, score);
				added = true;
				break;
			}
		}
		if(!added)
		{
			indexes.add(index);
			scores.add(score);
		}
	}
	
	/**
	 * Return the most likely senario
	 * 
	 * @param possibleExtensions
	 * @return
	 */
	private List<Point> closestExtension(List<Trajectory> trajectories, PointList pList)
	{
		List<Point> pointsToDo = new ArrayList<Point>(0);
		for (Point p : pList)
			pointsToDo.add(p);
		
		List<Point> pointsNew = new ArrayList<Point>(0);
		Point[] pointsExtended = new Point[trajectories.size()];
		
		// for each point determine which trajectory is closest to it
		while (pointsToDo.size() > 0)
		{
			Point p = pointsToDo.remove(0);
			// Logs.log("points to do left = " +
			// pointsToDo.size(),1,this);
			
			// Find the closest track to it
			List<Integer> possibilities = findTrajectoriesClose(p, trajectories);
			
			int index = 0;
			boolean placed = false;
			while (!placed)
			{
				
				// If there are no trajectories close by make it a new
				// trajectory
				if(possibilities.size() <= index)
				{
					pointsNew.add(p);
					placed = true;
					continue;
				}
				
				// Is there already a point extending this track
				Point existingPoint = pointsExtended[possibilities.get(index)];
				if(existingPoint == null)
				{
					// If there is no point extending this track add the found
					// one
					pointsExtended[possibilities.get(index)] = p;
					placed = true;
				}
				else
				{
					// If there is a point extending the track find out which is
					// closest
					Trajectory traj = trajectories.get(possibilities.get(index));
					double dOld = distanceOfPointToTrajectory(existingPoint, traj);
					double dNew = distanceOfPointToTrajectory(p, traj);
					
					if(dOld <= dNew)
					{
						// If the existing one is closer, try to place the new
						// point in the second
						// closest trajectory
						index = index + 1;
						continue;
					}
					else
					{
						// Place the new point as the extension of the
						// trajectory
						// and place the old point back into the list
						pointsToDo.add(existingPoint);
						pointsExtended[possibilities.get(index)] = p;
						placed = true;
					}
				}
				
			}
		}
		
		// Return the possibility list
		List<Point> result = new ArrayList<Point>(0);
		for (int i = 0, len = pointsExtended.length; i < len; i++)
		{
			Point p = pointsExtended[i];
			if(p == null)
				result.add(new Point(-1, -1));
			else
				result.add(p);
		}
		for (Point p : pointsNew)
		{
			result.add(p);
		}
		return result;
	}
	
	/**
	 * Return the distance between the trajectory and a point
	 * 
	 * @param p
	 * @param trajectory
	 * @return
	 */
	private double distanceOfPointToTrajectory(Point p, Trajectory trajectory)
	{
		Point iPoint = trajectory.interpolatedPoint();
		Double d = distance(p, iPoint);
		return d;
	}
	
	/**
	 * Get the score of a potential extension
	 * 
	 * @param extension
	 * @return
	 */
	private double scoreExtension(List<Point> extension)
	{
		double result = 0;
		
		// calculate the score of each extension
		for (int k = 0, len = activeTrajectories.size(); k < len; k++)
		{
			Trajectory trajectory = activeTrajectories.get(k);
			Point p = extension.get(k);
			Point p2 = trajectory.getLast();
			if(p.x == -1)
			{
				result = result + this.ENDING_PENALITY;
			}
			else
			{
				result = result + score(p, p2);
			}
		}
		// add a penalty for the creation of new trajectories
		for (int k = activeTrajectories.size(), len = extension.size(); k < len; k++)
		{
			result = result + this.ADDITION_PENALITY;
		}
		
		return result;
	}
	
	/**
	 * Return the score of a single extension
	 * 
	 * @param newPoint
	 * @param oldPoint
	 * @return
	 */
	private double score(Point pNEW, Point pOLD)
	{
		// if the track is ended add a penality
		if((pOLD.x != -1) && (pNEW.x == -1))
		{
			return this.ENDING_PENALITY;
		}
		
		// if the track is continued add the distance with the last point
		if((pOLD.x != -1) && (pNEW.x != -1))
		{
			return distance(pOLD, pNEW);
		}
		
		return 0;
	}
	
	// find the distance between two points
	private double distance(Point p1, Point p2)
	{
		double result = Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
		return result;
	}
	
	// print the trajectories
	public void printTrajectories()
	{
		for (int k = 0, len = allTrajectories.size(); k < len; k++)
		{
			Trajectory trajK = allTrajectories.get(k);
			String toPrint = trajK.toString();
			System.out.println("  ** Tracker: --------- Track " + k + " ---------" + " (" + trajK.isActive() + ")");
			System.out.println("  ** Tracker: " + toPrint);
		}
	}
	
	public void printSenarios(List<List<Point>> l)
	{
		for (int k = 0, len = l.size(); k < len; k++)
		{
			List<Point> v = l.get(k);
			String ext = "Track " + k + ": ";
			for (int i = 0, lenI = v.size(); i < lenI; i++)
			{
				Point p = v.get(i);
				ext = ext + "(" + p.x + "," + p.y + ")";
			}
			System.out.println("   Tracker.TrackExtend ---> " + ext);
		}
	}
	
}
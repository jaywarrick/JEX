//
//  TrackAnalyser.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 8/12/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package function.tracker;

import image.roi.Trajectory;
import image.roi.Vect;
import image.roi.VectSet;
import image.roi.XTrajectorySet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import logs.Logs;
import miscellaneous.VectorUtility;

public class TrackStatistics {
	
	// Class variables
	private XTrajectorySet tracksXML = null;
	private int numberTracks = 0;
	private List<Trajectory> trajectories;
	
	public TrackStatistics()
	{}
	
	public TrackStatistics(XTrajectorySet tracks)
	{
		tracksXML = tracks;
		trajectories = tracksXML.getTrajectories();
		numberTracks = (trajectories == null) ? 0 : trajectories.size();
		Logs.log("Number of tracks found " + numberTracks, 1, this);
	}
	
	public TrackStatistics(List<Trajectory> trajectories)
	{
		this.trajectories = trajectories;
		numberTracks = (trajectories == null) ? 0 : trajectories.size();
		Logs.log("Number of tracks found " + numberTracks, 1, this);
	}
	
	public TrackStatistics(Trajectory[] trajectories)
	{
		this.trajectories = new ArrayList<Trajectory>(0);
		for (Trajectory t : trajectories)
			this.trajectories.add(t);
		numberTracks = (trajectories == null) ? 0 : this.trajectories.size();
		Logs.log("TrackAnalyzer ---> Number of tracks found " + numberTracks, 1, this);
	}
	
	// --------------------------
	// -- Calculation functions
	// --------------------------
	public List<Double> CIs;
	public List<Double> velocities;
	public List<Double> angles;
	public List<Double> wangles;
	public List<Double> distTrav;
	public List<Double> totalDist;
	public List<Double> startinPositionX;
	public List<Double> startinPositionY;
	public List<Double> persistenceTimes;
	public VectSet vectors = new VectSet();
	public VectSet allvectors = new VectSet();
	public Vect meanVector;
	public double meanVelocity; // The mean velocity
	public double meanDisplacement; // The mean displacement
	public double meanAngle; // The mean angle
	public double meanTotalDisplacement; // The mean displacement
	public double CI; // The chemotactic index
	public double SCI; // The segmented chemotactic
	// index
	public double xDispRate; // The x displacement rate
	public double yDispRate; // The y displacement rate
	public double persistenceTime; // Persistence time
	
	public void startAnalysis(Trajectory singleTraj)
	{
		// List of values of importance
		CIs = new ArrayList<Double>(0);
		velocities = new ArrayList<Double>(0);
		angles = new ArrayList<Double>(0);
		wangles = new ArrayList<Double>(0);
		vectors = new VectSet();
		meanVector = new Vect();
		meanVelocity = 0; // The mean velocity
		meanDisplacement = 0; // The mean velocity
		meanAngle = 0; // The mean angle
		CI = 0; // The chemotactic index
		SCI = 0; // The segmented chemotactic index
		xDispRate = 0; // The chemotactic index
		yDispRate = 0; // The chemotactic index
		double scale = this.micronPerPixel / (this.secondPerFrame * this.deltaFrame);
		
		// Make the lists of vector displacements
		VectSet vs = singleTraj.getVectors(this.deltaFrame, true);
		
		for (int i = 0, len = vs.size(); i < len; i++)
		{
			// get the trajectory vector number INDEX
			Vect v = vs.get(i);
			
			// Scale vector
			v.multiply(scale);
			
			// Add vectors
			vectors.add(v);
			
			// calculate velocities
			velocities.add(v.norm());
			
			// calculate angles
			angles.add(v.angle());
			
			// calculate Chemotaxis indexes
			CIs.add(vs.chemtotaxisIndex());
		}
		
		meanVector = vectors.mean(); // The mean Vector
		meanVelocity = vectors.meanNorm(); // The mean velocity
		meanAngle = vectors.meanAngle(); // The mean angle
		CI = VectorUtility.mean(CIs); // The chemotactic index
		SCI = 0; // The segmented chemotactic index
		
	}
	
	// calculate values for inputed trajectories
	public void startAnalysis()
	{
		// List of values of importance
		CIs = new ArrayList<Double>(0);
		velocities = new ArrayList<Double>(0);
		angles = new ArrayList<Double>(0);
		wangles = new ArrayList<Double>(0);
		distTrav = new Vector<Double>(0);
		totalDist = new Vector<Double>(0);
		startinPositionX = new Vector<Double>(0);
		startinPositionY = new Vector<Double>(0);
		persistenceTimes = new Vector<Double>(0);
		vectors = new VectSet();
		allvectors = new VectSet();
		meanVector = new Vect();
		meanVelocity = 0; // The mean velocity
		meanDisplacement = 0; // The mean velocity
		meanAngle = 0; // The mean angle
		CI = 0; // The chemotactic index
		SCI = 0; // The segmented chemotactic index
		xDispRate = 0; // The chemotactic index
		yDispRate = 0; // The chemotactic index
		persistenceTime = 0; // The persistence time
		double scale = this.micronPerPixel / (this.secondPerFrame * this.deltaFrame);
		
		if(mode == SINGLEDISPLACEMENT)
		{
			// Make the lists of vector displacements
			List<VectSet> displacementLists = new ArrayList<VectSet>(0);
			for (int i = 0; i < numberTracks; i++)
			{
				// Get trajectory i
				Trajectory traj = loadTrajectory(i);
				
				// get all the displacements of the trajectory
				VectSet vs = traj.getVectors(this.deltaFrame, true);
				
				// add to list of vector lists
				displacementLists.add(vs);
				allvectors.add(vs);
				persistenceTimes.add(persistence(vs));
			}
			
			int index = 0;
			boolean listNotEmpty = true;
			
			// go through every track and add one vector from it as long as
			// 1. there are still vectors available
			// 2. the final list is smaller than the number requested
			while (listNotEmpty)
			{
				// if the requested number of vectors has been reached, stop
				// there
				if(nbCells > 0 && vectors.size() >= nbCells)
				{
					listNotEmpty = false;
					continue;
				}
				
				// else add one vector of each track
				boolean added = false;
				for (int i = 0; i < numberTracks; i++)
				{
					// if the number of vectors is reached, then break
					if(nbCells > 0 && vectors.size() >= nbCells)
					{
						listNotEmpty = false;
						break;
					}
					
					// Open the list of displacements
					VectSet vs = displacementLists.get(i);
					
					// if there is not enough displacements on this list, skip
					// it
					if(vs.size() <= index)
						continue;
					
					// get the trajectory vector number INDEX
					Vect v = vs.get(index);
					
					// Scale vector
					v.multiply(scale);
					
					// Add vectors
					vectors.add(v);
					
					// calculate velocities
					velocities.add(v.norm());
					
					// calculate angles
					angles.add(v.angle());
					
					// calculate Chemotaxis indexes
					CIs.add(vs.chemtotaxisIndex());
					
					// go to next index
					added = true;
				}
				index++;
				
				// If all lists are empty before then end prematurely
				if(!added)
					listNotEmpty = false;
			}
		}
		else if(mode == FULLTRACKS)
		{
			for (int i = 0; i < numberTracks; i++)
			{
				// Get trajectory i
				Trajectory traj = loadTrajectory(i);
				
				// get all the displacements of the trajectory
				VectSet vs = traj.getVectors(this.deltaFrame, true);
				
				// Add vectors
				Vect vsum = vs.mean();
				vsum.multiply(scale);
				vectors.add(vsum);
				allvectors.add(vs);
				
				// calculate velocities
				velocities.add(vs.meanNorm());
				totalDist.add(vs.meanNorm() * vs.size());
				
				// calculate angles
				angles.add(vs.angleMean());
				
				// calculate Chemotaxis indexes
				CIs.add(vs.chemtotaxisIndex());
				
				// get all the displacements of the trajectory
				Point first = traj.getFirst();
				Point last = traj.getLast();
				double dist = this.distance(first, last);
				
				// add to list of vector lists
				distTrav.add(dist);
				startinPositionX.add(first.getX());
				startinPositionY.add(first.getY());
				persistenceTimes.add(persistence(vs));
			}
		}
		
		if(mode == SINGLEDISPLACEMENT)
		{
			meanVector = vectors.mean(); // The mean Vector
			meanVelocity = vectors.meanNorm(); // The mean velocity
			meanAngle = vectors.meanAngle(); // The mean angle
			meanDisplacement = vectors.meanNorm() * vectors.size();
			xDispRate = vectors.meanNormOnAxis(VectSet.XAXIS);
			yDispRate = vectors.meanNormOnAxis(VectSet.YAXIS);
			CI = VectorUtility.mean(CIs); // The chemotactic index
			SCI = 0; // The segmented chemotactic index
			
			for (int i = 0; i < numberTracks; i++)
			{
				// Get trajectory i
				Trajectory traj = loadTrajectory(i);
				
				// get all the displacements of the trajectory
				Point first = traj.getFirst();
				Point last = traj.getLast();
				double dist = this.distance(first, last);
				
				// add to list of vector lists
				distTrav.add(dist);
			}
			
			persistenceTime = 0;
			for (double p : persistenceTimes)
			{
				persistenceTime = persistenceTime + p / persistenceTimes.size();
			}
		}
		else if(mode == FULLTRACKS)
		{
			meanVector = vectors.mean(); // The mean Vector
			meanVelocity = VectorUtility.mean(velocities); // The mean velocity
			meanAngle = vectors.meanAngle(); // The mean angle
			xDispRate = vectors.meanNormOnAxis(VectSet.XAXIS);
			yDispRate = vectors.meanNormOnAxis(VectSet.YAXIS);
			CI = VectorUtility.mean(CIs); // The chemotactic index
			SCI = 0; // The segmented chemotactic index
			
			meanTotalDisplacement = 0;
			for (Double d : totalDist)
			{
				meanTotalDisplacement = meanTotalDisplacement + d / totalDist.size();
			}
			
			meanDisplacement = 0;
			for (Double d : distTrav)
			{
				meanDisplacement = meanDisplacement + d / distTrav.size();
			}
			
			persistenceTime = 0;
			int plength = 0;
			for (double p : persistenceTimes)
			{
				if(p == Double.POSITIVE_INFINITY)
					continue;
				persistenceTime = persistenceTime + p;
				plength = plength + 1;
			}
			persistenceTime = persistenceTime / plength;
		}
		
		System.out.println("   TrackStatistics ---> ############################################################# ");
		System.out.println("   TrackStatistics ---> Calculated track statistics: ");
		System.out.println("   TrackStatistics ---> Found " + vectors.size() + " displacement vectors");
		System.out.println("   TrackStatistics ---> Mean vector = " + meanVector + " of norm = " + meanVector.norm() + " and angle = " + meanAngle);
		System.out.println("   TrackStatistics ---> Mean velocity = " + meanVelocity + " and CI = " + CI + " and SCI = " + SCI);
		System.out.println("   TrackStatistics ---> List of norms : " + velocities.toString());
		System.out.println("   TrackStatistics ---> List of angles: " + angles.toString());
		System.out.println("   TrackStatistics ---> List of CIs   : " + CIs.toString());
		System.out.println("   TrackStatistics ---> ############################################################# ");
	}
	
	public Trajectory loadTrajectory(int index, int min, int max)
	{
		if((index < 0) || (index >= trajectories.size()))
		{
			return null;
		}
		Trajectory traj = trajectories.get(index);
		Trajectory result = (Trajectory) traj.clone();
		result.trim(min, max);
		return result;
	}
	
	/**
	 * Return trajectory number INDEX
	 * 
	 * @param index
	 * @return
	 */
	public Trajectory loadTrajectory(int index)
	{
		if((index < 0) || (index >= trajectories.size()))
		{
			return null;
		}
		Trajectory result = trajectories.get(index);
		result.setMPP(micronPerPixel);
		result.setSPF(secondPerFrame);
		return result;
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
	 * Normalize a degrees angle between 0 (included) and 360 (excluded)
	 * 
	 * @param a
	 * @return
	 */
	public static double normalizeAngle(double a)
	{
		double result = a;
		while (result < 0 || result >= 360)
		{
			if(result < 0)
				result = result + 360;
			if(result >= 360)
				result = result - 360;
		}
		return result;
	}
	
	/**
	 * Return the single displacement vectors of all tracks
	 * 
	 * @return
	 */
	public VectSet getVectors(boolean rescale)
	{
		double scale = this.micronPerPixel / (this.secondPerFrame * this.deltaFrame);
		
		VectSet result = new VectSet();
		for (int i = 0; i < numberTracks; i++)
		{
			// Get trajectory i
			Trajectory traj = loadTrajectory(i);
			
			// get all the displacements of the trajectory
			VectSet vs = traj.getVectors(this.deltaFrame, true);
			
			for (Vect v : vs)
			{
				// add to list of vector lists
				if(rescale)
					v.multiply(scale);
				result.add(v);
			}
		}
		
		return result;
	}
	
	public double persistence(VectSet vset)
	{
		double result = 0;
		List<Double> angles = new ArrayList<Double>(0);
		
		// Calculate the angle variations
		Vect oldV = null;
		for (Vect v : vset)
		{
			if(oldV == null)
			{
				oldV = v;
				continue;
			}
			
			double angle1 = oldV.angle();
			double angle2 = v.angle();
			angles.add(angle2 - angle1);
			
			oldV = v;
		}
		
		// Get the persistence time
		for (double angle : angles)
		{
			result = angle * angle / (angles.size());
		}
		
		double persistance = 2 * deltaFrame / result;
		return persistance;
	}
	
	// --------------------------
	// -- Remote Operation
	// --------------------------
	public static int SINGLEDISPLACEMENT = 1;
	public static int FULLTRACKS = 2;
	
	// Track variables
	public double micronPerPixel = 1;
	public double secondPerFrame = 1;
	public int angleOffset = 0;
	public int mode = 2;
	public int deltaFrame = 1;
	public int nbCells = -1;
	
	/**
	 * Set calculation to be done. Either sliding window experiments on a track, or individual displacement experiments
	 * 
	 * @param ranges
	 * @param nbCells
	 * @param nbFrames
	 */
	public void setParameters(int nbCells, int deltaFrame)
	{
		this.nbCells = nbCells;
		this.deltaFrame = deltaFrame;
	}
	
	/**
	 * Display the statistics of the tracks
	 */
	public void print()
	{
		System.out.println("   -----------------------------------------------");
		System.out.println("   TrackStatistics ---> Statistics of the tracks :");
		System.out.println("   -----------------------------------------------");
		System.out.println("   --- Calculation mode is " + this.mode);
		System.out.println("   --- Mean velocity = " + this.meanVelocity);
		System.out.println("   --- Mean Angle = " + this.meanAngle);
		System.out.println("   --- Mean Displacement = " + this.meanVector.norm());
		System.out.println("   --- CI = " + this.CI);
		System.out.println("   --- SCI = " + this.SCI);
		System.out.println("   --- Velocities = " + this.velocities);
		System.out.println("   --- Angles = " + this.angles);
		System.out.println("   -----------------------------------------------");
		
	}
}

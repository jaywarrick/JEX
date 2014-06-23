//
//  TrackAnalyser.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 8/12/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package function.roitracker;

import image.roi.Vect;
import image.roi.VectSet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import jex.utilities.ROIUtility;
import logs.Logs;
import miscellaneous.VectorUtility;

public class RoiTrackStatistics {
	
	// Data
	private RoiTrack track;
	
	// Statics
	public static int MIGRATION_LINEAR_GRADIENT = 1;
	public static int MIGRATION_POINT_SOURCE = 2;
	public static Axis XAXIS = new Axis(new Point(0, 0), new Point(1, 0));
	public static Axis YAXIS = new Axis(new Point(0, 0), new Point(0, 1));
	
	// Properties and options
	private double micronPerPixel = 1;
	private double secondPerFrame = 1;
	private int deltaFrame = 1;
	private Axis mainAxis = XAXIS;
	private int migrationType = MIGRATION_LINEAR_GRADIENT;
	
	// Calculated variables and outputs
	public List<Double> velocities;
	public List<Double> chemovelocities;
	public List<Double> angles;
	public List<Double> wangles;
	public List<Double> distTrav;
	public VectSet vectors;
	public Vect meanVector;
	public double meanVelocity; // The mean velocity
	public double meanChemoVelocity; // The mean chemotactic velocity
	public double meanDisplacement; // The mean displacement
	public double meanAngle; // The mean angle
	public double totalDisplacement; // The total displacement
	public double CI; // The chemotactic index
	public double MCI; // The meandering index
	public double xDispRate; // The x displacement rate
	public double yDispRate; // The y displacement rate
	public double persistenceTime; // Persistence time
	public double startinPositionX;
	public double startinPositionY;
	
	// ---------------------------
	// ------ Initiators ---------
	// ---------------------------
	public RoiTrackStatistics()
	{   
		
	}
	
	public RoiTrackStatistics(RoiTrack track)
	{
		this.setTrack(track);
	}
	
	// ---------------------------
	// -- Setters and Getters ----
	// ---------------------------
	/**
	 * Set the track used
	 */
	public void setTrack(RoiTrack track)
	{
		// Set the track
		this.track = track;
		
		// Start the analysis
		this.startAnalysis();
	}
	
	/**
	 * Get the track used
	 */
	public RoiTrack getTrack()
	{
		return this.track;
	}
	
	/**
	 * Set the migration type, ie in a linear gradient of a point source Use static values MIGRATION_LINEAR_GRADIENT or MIGRATION_POINT_SOURCE
	 * 
	 * @param migrationType
	 */
	public void setMigrationType(int migrationType)
	{
		this.migrationType = migrationType;
	}
	
	/**
	 * Get the migration type, ie in a linear gradient of a point source
	 * 
	 * @param migrationType
	 */
	public int getMigrationType()
	{
		return this.migrationType;
	}
	
	/**
	 * Set the binning rate, to average each displacement over multiple frames e.g. recommended for slow moving cells to minize the digital error due to single pixel movements
	 * 
	 * @param deltaFrame
	 */
	public void setBinning(int deltaFrame)
	{
		this.deltaFrame = deltaFrame;
	}
	
	/**
	 * Set the number of seconds per frame, for quantitative migration values
	 * 
	 * @param secondPerFrame
	 */
	public void setSecondsPerFrame(double secondPerFrame)
	{
		this.secondPerFrame = secondPerFrame;
	}
	
	/**
	 * Set the number of microns per pixel, for quantitative migration values
	 * 
	 * @param micronPerPixel
	 */
	public void setMicronsPerPixel(double micronPerPixel)
	{
		this.micronPerPixel = micronPerPixel;
	}
	
	/**
	 * Set the expected axis of migration, the default axis is set to migration along the X vector the second axis is automatically set to be othogonal to this one
	 * 
	 * @param mainAxis
	 */
	public void setAxis(Axis mainAxis)
	{
		this.mainAxis = mainAxis;
	}
	
	// ---------------------------
	// -- Calculation functions --
	// ---------------------------
	/**
	 * Analyze the trajectory
	 */
	public void startAnalysis()
	{
		// List of displacement vectors
		this.vectors = new VectSet();
		VectSet dispVectors = new VectSet();
		
		// List of values pertaining to each displacement vector
		this.velocities = new ArrayList<Double>(0);
		this.chemovelocities = new ArrayList<Double>(0);
		this.angles = new ArrayList<Double>(0);
		this.wangles = new ArrayList<Double>(0);
		this.distTrav = new ArrayList<Double>(0);
		
		// List of values averaged over the whole trajectory
		this.meanVector = new Vect();
		this.meanVelocity = 0; // The mean velocity
		this.meanChemoVelocity = 0; // The mean chemotactic velocity
		this.meanDisplacement = 0; // The mean displacement
		this.meanAngle = 0; // The mean angle
		this.totalDisplacement = 0; // The total displacement
		this.CI = 0; // The chemotactic index
		this.MCI = 0; // The meandering index
		this.xDispRate = 0; // The x displacement rate
		this.yDispRate = 0; // The y displacement rate
		this.persistenceTime = 0; // Persistence time
		this.startinPositionX = 0;
		this.startinPositionY = 0;
		
		// List of values of importance
		double scale = this.micronPerPixel / (this.secondPerFrame * this.deltaFrame);
		
		// Make the lists of vector displacements
		VectSet vs = this.track.getVectors(true);
		
		// Set the starting location of the track
		Point p = ROIUtility.getRectangleCenter(this.track.first());
		this.startinPositionX = (p == null) ? -1 : p.getX();
		this.startinPositionY = (p == null) ? -1 : p.getY();
		
		// Loop through the vectors
		for (int i = 0, len = vs.size(); i < len; i++)
		{
			// get the trajectory vector number INDEX
			Vect v = vs.get(i);
			dispVectors.add(v.duplicate());
			
			// Scale vector
			v.multiply(scale);
			
			// Add vectors
			this.vectors.add(v);
			
			// calculate velocities
			double norm = v.norm();
			this.velocities.add(norm);
			this.chemovelocities.add(this.mainAxis.projectVectorOnAxis(v));
			
			// calculate angles
			double angle = RoiTrackStatistics.normalizeAngle(v.angle());
			this.angles.add(angle);
			
			// calculate wangles
			this.wangles.add(norm * angle);
			
			// calculate the displacement
			double displacement = norm * this.secondPerFrame * this.deltaFrame;
			this.distTrav.add(displacement);
			
			// add to the total displacement
			this.totalDisplacement = this.totalDisplacement + displacement;
		}
		
		this.meanVector = this.vectors.mean(); // The mean Vector
		this.meanVelocity = this.vectors.meanNorm(); // The mean velocity
		this.meanAngle = this.vectors.meanAngle(); // The mean angle
		this.meanDisplacement = dispVectors.mean().norm(); // The mean displacement
		this.xDispRate = this.meanVector.getDX(); // The x displacement rate
		this.yDispRate = this.meanVector.getDY(); // The y displacement rate
		
		this.meanChemoVelocity = VectorUtility.mean(this.chemovelocities);
		
		// Calculate the meandering index
		this.MCI = dispVectors.sum().norm() / this.totalDisplacement;
		
		// calculate chemotaxis index
		double displacementTowardsMainAxis = this.mainAxis.projectVectorOnAxis(dispVectors.sum());
		this.CI = displacementTowardsMainAxis / this.totalDisplacement;
		
		// calculate the persistence time
		this.persistenceTime = this.persistence(vs);
		
		// Display stuff
		Logs.log("Finished analyzing trajectory", 2, this);
	}
	
	/**
	 * Find the distance between two points
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	public double distance(Point p1, Point p2)
	{
		double currentDistance = Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
		return currentDistance;
	}
	
	/**
	 * Find the angle of a segment
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
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
			{
				result = result + 360;
			}
			if(result >= 360)
			{
				result = result - 360;
			}
		}
		return result;
	}
	
	/**
	 * Calculate the persistence time of the set of vectors
	 * 
	 * @param vset
	 * @return
	 */
	private double persistence(VectSet vset)
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
		
		double persistance = 2 * this.deltaFrame / result;
		return persistance;
	}
	
	// --------------------------
	// -- Remote Operation
	// --------------------------
	
	/**
	 * Display the statistics of the tracks
	 */
	public void print()
	{
		System.out.println("   -----------------------------------------------");
		System.out.println("   TrackStatistics ---> Statistics of the tracks :");
		System.out.println("   -----------------------------------------------");
		System.out.println("   --- Calculation mode is " + this.migrationType);
		System.out.println("   --- Mean velocity = " + this.meanVelocity);
		System.out.println("   --- Mean Angle = " + this.meanAngle);
		System.out.println("   --- Mean Displacement = " + this.meanVector.norm());
		System.out.println("   --- CI = " + this.CI);
		System.out.println("   --- Velocities = " + this.velocities);
		System.out.println("   --- Angles = " + this.angles);
		System.out.println("   -----------------------------------------------");
		
	}
}

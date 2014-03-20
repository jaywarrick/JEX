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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import miscellaneous.VectorUtility;

public class TrajectoryStatistics {
	
	// Track variables
	public double micronPerPixel = 1;
	public double secondPerFrame = 1;
	public int angleOffset = 0;
	public int deltaFrame = 1;
	
	// Class variables
	private Trajectory trajectory;
	public List<Double> velocities;
	public List<Double> angles;
	public List<Double> wangles;
	public VectSet vectors = new VectSet();
	public Vect meanVector;
	public double meanVelocity; // The mean velocity
	public double meanDisplacement; // The mean displacement
	public double meanAngle; // The mean angle
	public double CI; // The chemotactic index
	public double SCI; // The segmented
	
	// chemotactic index
	
	public TrajectoryStatistics(Trajectory trajectory)
	{
		this.trajectory = trajectory;
		// Logs.log("Loading trajectory statistics", 2, this);
	}
	
	// calculate values for inputed trajectories
	public void startAnalysis()
	{
		startAnalysis(trajectory.initial(), trajectory.length());
	}
	
	public void startAnalysis(int min, int max)
	{
		// List of values of importance
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
		double distance = 0;
		double distCI = 0;
		double scale = this.micronPerPixel / (this.secondPerFrame * this.deltaFrame);
		
		int thisFrame = min;
		int nextFrame = thisFrame + deltaFrame;
		Point iniP = null;
		Point lastP = null;
		while (nextFrame <= max)
		{
			Point first = trajectory.getPoint(thisFrame);
			Point second = trajectory.getPoint(nextFrame);
			if(first == null)
			{
				thisFrame = thisFrame + deltaFrame;
				nextFrame = thisFrame + deltaFrame;
				continue;
			}
			if(iniP == null)
				iniP = first;
			
			if(second == null)
			{
				nextFrame = nextFrame + deltaFrame;
				continue;
			}
			lastP = second;
			
			// get the trajectory vector number INDEX
			Vect v = new Vect(second, first);
			distCI = distCI + distance(second, first);
			
			// Scale vector
			scale = this.micronPerPixel / (this.secondPerFrame * (nextFrame - thisFrame));
			v.multiply(scale);
			
			// Add vectors
			vectors.add(v);
			
			// calculate velocities
			Double w = (double) v.norm();
			distance = distance + w;
			velocities.add(w);
			
			// calculate angles if the norm of the vector is 0 then the angle is
			// irrelevant
			if(w > 0)
			{
				Double a = v.angle() + angleOffset;
				a = normalizeAngle(a);
				angles.add(a);
				wangles.add(w);
			}
			
			thisFrame = nextFrame;
			nextFrame = nextFrame + deltaFrame;
		}
		
		double effectiveDistance = (iniP == null || lastP == null) ? 0 : distance(iniP, lastP);
		meanVector = (vectors.size() == 0) ? new Vect(0, 0) : vectors.mean(); // The
		// mean
		// Vector
		meanVelocity = (velocities.size() == 0) ? 0 : VectorUtility.mean(velocities); // The
		// mean
		// velocity
		meanAngle = (vectors.size() == 0) ? 0 : vectors.meanAngle(); // The mean
		// angle
		CI = (distCI == 0) ? 0 : effectiveDistance / distCI; // the chemotactic
		// angle
		SCI = 0; // The segmented chemotactic index
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
	
}

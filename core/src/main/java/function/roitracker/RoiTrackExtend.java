package function.roitracker;

import ij.ImagePlus;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jex.utilities.ROIUtility;

import org.jfree.util.Log;

public class RoiTrackExtend {
	
	// statics
	private static double STARTING_PENALITY = 39;
	private static double ENDING_PENALITY = 39;
	public static int CENTER_OF_MASS_DISTANCE_CALCULATION = 0;
	public static int CENTROID_DISTANCE_CALCULATION = 1;
	public static int ORIGIN_DISTANCE_CALCULATION = 2;
	
	// class options
	public int maxDisplacement = 30;
	public int maxDissapear = 3;
	public boolean useInterpolation = false;
	public int distanceCalculationMode = CENTROID_DISTANCE_CALCULATION;
	
	// class variables
	private int currentFrame = 0;
	
	// Data and output variables
	private List<ROIPlus> rois;
	private List<RoiTrack> tracks;
	
	public RoiTrackExtend(List<RoiTrack> tracks)
	{
		this.tracks = tracks;
	}
	
	public RoiTrackExtend()
	{
		this.tracks = new ArrayList<RoiTrack>(0);
	}
	
	public void extendRoiTracks(List<ROIPlus> rois, ImagePlus optionalImageForCenterOfMassCalculation, int frame)
	{
		// Pass variables
		this.rois = rois;
		this.currentFrame = frame;
		
		// Start the extension process
		Log.log(1, "Extending RoiTracks to frame " + this.currentFrame);
		
		// if this is the first set of rois, the tracks are trivial
		if(this.tracks.size() == 0)
		{
			Log.log(1, "Creating initial trajectories");
			for (ROIPlus roi : rois)
			{
				RoiTrack track = new RoiTrack(roi, this.currentFrame);
				this.tracks.add(track);
			}
			return;
		}
		
		// Else the ROIPluses from ROIS must be associated with exitsing RoiTracks
		else
		{
			Log.log(1, "Finding closest neighbour extensions");
			HashMap<ROIPlus,RoiTrack> extensions = this.closestExtension(optionalImageForCenterOfMassCalculation);
			
			Log.log(1, "Extensions found, adding to the tracks");
			for (ROIPlus roi : extensions.keySet())
			{
				RoiTrack track = extensions.get(roi);
				
				// If there is no track associated, then it's a new track
				if(track == null)
				{
					track = new RoiTrack(roi, this.currentFrame);
					this.tracks.add(track);
				}
				// Else add it to the track
				else
				{
					track.addRoi(roi, this.currentFrame);
				}
			}
		}
	}
	
	// ----------------------------------------------------
	// ------------- SETTINGS and I/O ---------------------
	// ----------------------------------------------------
	
	public void setMaxDisplacement(int maxDisplacement)
	{
		this.maxDisplacement = maxDisplacement;
	}
	
	public void setMaxDissapearance(int maxDissapearance)
	{
		this.maxDissapear = maxDissapearance;
	}
	
	public void setUseInterpolation(boolean useInterpolation)
	{
		this.useInterpolation = useInterpolation;
	}
	
	public void setStartingPenality(double penality)
	{
		STARTING_PENALITY = penality;
	}
	
	public void setEndingPenality(double penality)
	{
		ENDING_PENALITY = penality;
	}
	
	public void setDistanceCalculationMode(int distanceCalculationMode)
	{
		this.distanceCalculationMode = distanceCalculationMode;
	}
	
	public List<RoiTrack> getTracks()
	{
		return this.tracks;
	}
	
	// ----------------------------------------------------
	// ----------------- CALCULATION ----------------------
	// ----------------------------------------------------
	
	/**
	 * Return the most likely senario
	 * 
	 * @return
	 */
	private HashMap<ROIPlus,RoiTrack> closestExtension(ImagePlus optionalImage)
	{
		// Make the output variable
		HashMap<ROIPlus,RoiTrack> result = new HashMap<ROIPlus,RoiTrack>();
		
		// Loop through the tracks and find the closest extending ROIPlus
		for (RoiTrack track : this.tracks)
		{
			// If the track has dissapeared for too long skip it
			int lastFrameofTrack = track.lastKey();
			if((this.currentFrame - lastFrameofTrack) > this.maxDissapear)
			{
				continue;
			}
			
			// Find the closest roi to track TRACK
			DistRoiPair thisPair = this.findROIClose(track, optionalImage);
			ROIPlus thisRoi = thisPair.thisRoi;
			double thisDist = thisPair.thisDist;
			
			// If the roi is too far skip
			if(thisDist > this.maxDisplacement)
			{
				Log.log(1, "Extension too far, skipping");
				continue;
			}
			
			// Has the ROI been selected by another track?
			RoiTrack thatTrack = result.get(thisRoi);
			
			// If the score of the other track is better keep it
			// or use the new found track
			if(thatTrack != null)
			{
				// Get the distance between the older track and the roi
				double thatDist = this.distanceRoiTrack(thisRoi, thatTrack, optionalImage);
				
				// Score the two extensions
				double score1 = this.scoreExtension(track, thisRoi, thisDist, optionalImage);
				double score2 = this.scoreExtension(thatTrack, thisRoi, thatDist, optionalImage);
				
				// If it scores less then the previous one then update the map, linking the new track with the ROI
				if(score1 < score2)
				{
					result.put(thisRoi, track);
					Log.log(1, "Better extension found: replacing");
					
				}
			}
			// If there is no track that previously selected this ROI, add it to the list
			else
			{
				result.put(thisRoi, track);
				Log.log(1, "Unique extension found: adding");
			}
		}
		
		// Add the non-linked ROIPluses
		for (ROIPlus roi : this.rois)
		{
			if(result.get(roi) == null)
			{
				result.put(roi, null);
			}
		}
		
		return result;
	}
	
	/**
	 * Find trajectories in the List of current trajectories that are close to point p
	 * 
	 * @return
	 */
	private DistRoiPair findROIClose(RoiTrack track, ImagePlus optionalImage)
	{
		// Variables used
		double dist = -1;
		ROIPlus closest = null;
		
		// Loop through the ROIPluses from ROIS
		// and determine the distance
		for (ROIPlus roi : this.rois)
		{
			// Measure the distance
			double d = this.distanceRoiTrack(roi, track, optionalImage);
			
			// If it is smaller that the current minimum, update the variables
			// or if this is the first one update the variables
			// Else skip it
			if(dist == -1 || d < dist)
			{
				dist = d;
				closest = roi;
			}
		}
		
		DistRoiPair result = new DistRoiPair(closest, dist);
		return result;
	}
	
	/**
	 * Return the distance between the trajectory and a point
	 * 
	 * @return
	 */
	private double distanceRoiTrack(ROIPlus roi, RoiTrack track, ImagePlus optionalImage)
	{
		// Set up variables
		ROIPlus lastRoi = null;
		double dist = -1;
		
		// If the use interpolation flag is off then just take the last roi in the track
		if(!this.useInterpolation)
		{
			lastRoi = track.last();
		}
		// Else get the interpolated point and calculate the distance from that
		else
		{
			lastRoi = track.last(); // THIS IS NOT YET IMPLEMENTED
		}
		this.distanceCalculationMode = ORIGIN_DISTANCE_CALCULATION;
		
		// Calculate the distance between two ROIs
		// If the distance measurement mode is a center of mass of the ROI
		if(this.distanceCalculationMode == CENTER_OF_MASS_DISTANCE_CALCULATION)
		{
			// Get the center of masses of the two rois
			Point p1 = ROIUtility.getCenterOfMass(optionalImage.getProcessor(), roi);
			Point p2 = ROIUtility.getCenterOfMass(optionalImage.getProcessor(), lastRoi);
			
			// Measure the distance
			dist = this.distance(p1, p2);
		}
		// If the distance measurement mode is something else
		else if(this.distanceCalculationMode == ORIGIN_DISTANCE_CALCULATION)
		{
			// Get the center of masses of the two rois
			Point p1 = ROIUtility.getRectangleCenter(roi);
			Point p2 = ROIUtility.getRectangleCenter(lastRoi);
			
			// Measure the distance
			dist = this.distance(p1, p2);
		}
		// If the distance measurement mode is something else
		else if(this.distanceCalculationMode == CENTROID_DISTANCE_CALCULATION)
		{
			// Get the center of masses of the two rois
			Point p1 = ROIUtility.getCentroid(roi);
			Point p2 = ROIUtility.getCentroid(lastRoi);
			
			// Measure the distance
			dist = this.distance(p1, p2);
		}
		
		return dist;
	}
	
	/**
	 * Returns the distance between two points
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	private double distance(Point p1, Point p2)
	{
		double result = Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
		return result;
	}
	
	/**
	 * Scores the cost of extending a track with a roi Takes into account if the track is starting of ending with a null track or a size zero track it is considered starting with a null roi it is considered ending
	 * 
	 * @param roi
	 * @param track
	 * @return
	 */
	private double scoreExtension(RoiTrack track, ROIPlus roi, double distance, ImagePlus optionalImage)
	{
		// The score of an extension takes into account:
		// (1) the distance of the extension
		// (2) how many frames have been skipped
		// (3) if the track is being started or ended
		
		// if it's a starting frame return the start penalty
		if(track == null || track.size() == 0)
		{
			return STARTING_PENALITY;
		}
		
		// If it's an ending of track return the end penalty
		if(roi == null)
		{
			return ENDING_PENALITY;
		}
		
		// Get the last frame in the track and calculate the number of frames "skipped"
		int lastFrame = track.lastKey();
		int deltaFrame = Math.abs(lastFrame - this.currentFrame);
		
		// Calculate the score:
		// The score is the distance to the last track divided by the number of frames
		double score = distance / (deltaFrame);
		
		return score;
	}
	
}

class DistRoiPair {
	
	public ROIPlus thisRoi = null;
	public double thisDist = -1;
	
	DistRoiPair(ROIPlus roi, Double distance)
	{
		this.thisRoi = roi;
		this.thisDist = distance;
	}
}
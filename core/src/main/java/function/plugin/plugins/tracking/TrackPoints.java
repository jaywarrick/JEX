package function.plugin.plugins.tracking;

import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.oldlap.FastLAPTracker;
import fiji.plugin.trackmate.tracking.oldlap.LAPTracker;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.JEXCSVWriter;
import miscellaneous.Pair;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableReader;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Track Points (LAP)",
		menuPath="Image Tools > Tracking",
		visible=true,
		description="LAP (Linear Assignment Problem) point tracking algorithm."
		)
public class TrackPoints extends JEXPlugin {

	public TrackPoints()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Maxima", type=MarkerConstants.TYPE_ROI, description="PointRois over time to be linked/tracked.", optional=false)
	JEXData roiData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Max Linking Distance", description="Maximum pixel radius within which to consider a linking two points from time t to t+1.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="25.0")
	double maxLinkDistance;
	
	@ParameterMarker(uiOrder=2, name="Max Gap Distance", description="Maximum pixel distance to entertain when a gap exists.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double maxGapDistance;
	
	@ParameterMarker(uiOrder=3, name="Max Gap Frames", description="Maximum gap in frames to entertain.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int maxGapFrames;
	
	@ParameterMarker(uiOrder=4, name="Time Dim Name", description="Name of the time dimension in the ROI object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="T")
	String timeDimName;
	
	@ParameterMarker(uiOrder=4, name="New Track Dim Name", description="Name of the track dimension to create in the output ROI object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Track")
	String trackDimName;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Maxima (tracked)", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant maxima roi with id's linked from image frame to image frame.", enabled=true)
	JEXData trackedMaximaData;
	
	@OutputMarker(uiOrder=2, name="Tracks", type=MarkerConstants.TYPE_ROI, flavor="", description="Inidividual point rois for each track with ids matching the maxima (tracked) output.", enabled=true)
	JEXData trackRoiData;
	
	@OutputMarker(uiOrder=3, name="Position Table", type=MarkerConstants.TYPE_FILE, flavor="", description="A csv table of positions of all cells at all times.", enabled=true)
	JEXData positionData;
	
	@OutputMarker(uiOrder=4, name="Median Deltas ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="Point ROI that represents the median overall motion of the cells, stored as a point roi for use with other functions such as image registration.", enabled=true)
	JEXData deltasRoiData;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		roiData.getDataMap();
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Create the SpotCollections
		TreeMap<DimensionMap,SpotCollection> spotCollections = getSpotCollections();
		
		// Link the spots to create the segments
		// For each SpotCollection, track the Spots to create segments
		// Convert the segments to track rois
		// Store them in the finalTrackRois object
		TreeMap<DimensionMap,ROIPlus> finalTrackRois = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> finalMaximaRois = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap, Double> positionTable = new TreeMap<>();
		TreeMap<DimensionMap, ROIPlus> diffRois = new TreeMap<>();
		
		int count = 0, percentage = 0;
		for(Entry<DimensionMap,SpotCollection> e : spotCollections.entrySet())
		{
			DimensionMap segmentMap = e.getKey(); // This map doesn't have a time dim. Time info is in the SpotCollections
			
			if(this.isCanceled())
			{
				return false;
			}
			
			// Track the spots to create segments
			Logs.log("Tracking points for " + e.getKey(), this);
			FastLAPTracker tracker = new FastLAPTracker(e.getValue(), this.getSimpleTrackerSettings(maxLinkDistance, maxGapDistance, maxGapFrames));
			tracker.process();
			
			// Convert to trackRois and maximaRois (these maps have timeDims in them)
			Pair<TreeMap<DimensionMap, ROIPlus>,TreeMap<DimensionMap, ROIPlus>> rois = this.getMaximaRois(tracker, segmentMap, roiData.getDimTable().getDimWithName(timeDimName));
			
			// Get the positions csv table
			for(Entry<DimensionMap, ROIPlus> e3 : rois.p2.entrySet())
			{
				for(IdPoint p : e3.getValue().pointList)
				{
					positionTable.put(e3.getKey().copyAndSet("Id=" + p.id + ",Measurement=x"), (double) p.x);
					positionTable.put(e3.getKey().copyAndSet("Id=" + p.id + ",Measurement=y"), (double) p.y);
				}
			}
			
			// Get diffs... the dimension maps returned have a time dim.
			TreeMap<DimensionMap,Vector<Pair<Double,Double>>> diffs = StatisticsUtility.getDiffVectors(rois.p2, timeDimName);
			
			for(Entry<DimensionMap,Vector<Pair<Double,Double>>> e2 : diffs.entrySet())
			{
				Pair<Double,Double> diff = StatisticsUtility.getMedianDiff(e2.getValue());
				PointList p = new PointList();
				p.add(new IdPoint((int) Math.round(diff.p1), (int) Math.round(diff.p2), 0));
				diffRois.put(e2.getKey().copy(), new ROIPlus(p, ROIPlus.ROI_POINT));
			}
			
			// Store them in the finalTrackRois map
			finalTrackRois.putAll(rois.p1);
			finalMaximaRois.putAll(rois.p2);
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) spotCollections.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		// Create the JEXData and assign the outputs
		this.trackRoiData = RoiWriter.makeRoiObject("temp", finalTrackRois);
		this.trackedMaximaData = RoiWriter.makeRoiObject("temp", finalMaximaRois);
		String tablePath = JEXCSVWriter.writeDoubleTable(positionTable);
		this.positionData = FileWriter.makeFileObject("position data", null, tablePath);
		this.deltasRoiData = RoiWriter.makeRoiObject("diffs", diffRois);
		
		// Return status
		return true;
	}
	
	public String getSpot(Spot s)
	{
		return s.getName() + "," + s.getFeature(Spot.FRAME);
	}
	
	public Pair<TreeMap<DimensionMap,ROIPlus>,TreeMap<DimensionMap,ROIPlus>> getMaximaRois(LAPTracker tracker, DimensionMap segmentMap, Dim timeDim)
	{
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = tracker.getResult();
		
		TreeMap<DimensionMap,PointList> tracks = new TreeMap<DimensionMap,PointList>();
		for(Spot s : graph.vertexSet())
		{
			// Get the incoming and outgoing edges			
			Spot incoming = null;
			Spot outgoing = null;
			for(DefaultWeightedEdge edge : graph.edgesOf(s))
			{
				Spot temp = graph.getEdgeTarget(edge);
				if(temp.getFeature(Spot.FRAME) == s.getFeature(Spot.FRAME))
				{
					incoming = temp;
				}
				else
				{
					outgoing = temp;
				}
			}
			
			// If no incoming edge, then create new track.
			if(incoming == null)
			{
				DimensionMap toSave = new DimensionMap("Id=" + s.getName() + ",LastFrame=" + s.getFeature(Spot.FRAME));
				PointList pToSave = new PointList();
				pToSave.add(this.getIdPoint(s));
				if(outgoing != null)
				{
					toSave = new DimensionMap("Id=" + outgoing.getName() + ",LastFrame=" + outgoing.getFeature(Spot.FRAME));
					pToSave.add(this.getIdPoint(outgoing));
				}
				tracks.put(toSave, pToSave);
			}
			else if(outgoing != null)
			{
				// remove track by last point, add point, put track back with new last point
				PointList toExtend = tracks.remove(new DimensionMap("Id=" + incoming.getName() + ",LastFrame=" + incoming.getFeature(Spot.FRAME)));
				toExtend.add(this.getIdPoint(outgoing));
				tracks.put(new DimensionMap("Id=" + outgoing.getName() + ",LastFrame=" + outgoing.getFeature(Spot.FRAME)), toExtend);
			}
		}
		
		TreeMap<DimensionMap,ROIPlus> trackRois = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,IdPoint> allPoints = new TreeMap<DimensionMap,IdPoint>();
		int idCounter = 0;
		for(PointList pl : tracks.values())
		{
			DimensionMap newMap = segmentMap.copy();
			newMap.put(trackDimName, ""+idCounter);
			ROIPlus track = this.getTrackRoi(pl);
			trackRois.put(newMap.copy(), track);
			for(IdPoint p : pl)
			{
				newMap.put(timeDimName, ""+p.id);
				allPoints.put(newMap.copy(), p);
			}
			idCounter++;
		}
		
		// Make new maxima rois with id's that actually match for different times
		TreeMap<DimensionMap,ROIPlus> maximaRois = new TreeMap<DimensionMap,ROIPlus>();
		for (String time : timeDim.values())
		{
			// for each time			
			DimensionMap newMap = segmentMap.copy();
			newMap.put(timeDim.dimName, "" + time);
			
			TreeMap<DimensionMap,IdPoint> filteredPoints = JEXTableReader.filter(allPoints, newMap);
			PointList temp = new PointList();
			for (Entry<DimensionMap,IdPoint> e : filteredPoints.entrySet())
			{
				IdPoint p = e.getValue().copy();
				p.id = Integer.parseInt(e.getKey().get(trackDimName));
				temp.add(p);
			}
			ROIPlus newMaximaRoi = new ROIPlus(temp, ROIPlus.ROI_POINT);
			maximaRois.put(newMap, newMaximaRoi);
		}
		return new Pair<>(trackRois, maximaRois);
	}
	
	//	public TreeMap<DimensionMap,ROIPlus> getTrackRois(List<SortedSet<Spot>> segments, DimensionMap segmentMap)
	//	{
	//		Vector<PointList> tracks = new Vector<PointList>();
	//		for(SortedSet<Spot> segment : segments)
	//		{
	//			PointList pl = new PointList();
	//			for(Spot s : segment)
	//			{
	//				pl.add(this.getIdPoint(s));
	//			}
	//			tracks.add(pl);
	//		}
	//		
	//		TreeMap<DimensionMap,ROIPlus> trackRois = new TreeMap<DimensionMap,ROIPlus>();
	//		int idCounter = 0;
	//		for(PointList pl : tracks)
	//		{
	//			DimensionMap newMap = segmentMap.copy();
	//			newMap.put(trackDimName, ""+idCounter);
	//			ROIPlus track = this.getTrackRoi(pl);
	//			trackRois.put(newMap, track);
	//			idCounter = idCounter + 1;
	//		}
	//		return trackRois;
	//	}
	
	public TreeMap<DimensionMap,SpotCollection> getSpotCollections()
	{
		// Prepare to store a spotCollection for each time-series defined by the dimTable
		TreeMap<DimensionMap,SpotCollection> ret = new TreeMap<DimensionMap,SpotCollection>();
		
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		DimTable table = roiData.getDimTable();
		Dim timeDim = table.getDimWithName(timeDimName);
		DimTable tableFixedTime = table.getSubTable(new DimensionMap(timeDimName + "=" + timeDim.valueAt(0)));
		for(DimensionMap map : tableFixedTime.getMapIterator())
		{
			SpotCollection spots = new SpotCollection();
			for(String time : timeDim.values())
			{
				DimensionMap fullMap = map.copy();
				fullMap.put(timeDimName, time);
				
				ROIPlus roi = rois.get(fullMap);
				if(roi != null)
				{
					for(IdPoint p : roi.getPointList())
					{
						Spot theSpot = this.getSpot(p, time);
						//Logs.log(""+theSpot.getFeatures(), this);
						spots.add(theSpot, Integer.parseInt(time));
					}
				}
			}
			DimensionMap newMap = map.copy();
			newMap.remove(timeDimName);
			ret.put(newMap, spots);
		}
		return ret;
	}
	
	public ROIPlus getTrackRoi(PointList pl)
	{
		IdPoint firstPoint = pl.get(0);
		PointList toAdd = new PointList();
		toAdd.add(firstPoint);
		ROIPlus ret = new ROIPlus(toAdd, ROIPlus.ROI_POINT);
		ret.setPattern(pl.copy());
		return ret;
	}
	
	public Spot getSpot(IdPoint pt, String frame)
	{
		// For reference
		// public Spot( final double x, final double y, final double z, final double radius, final double quality, final String name )
		Spot ret = new Spot( pt.x, pt.y, 0.0, 1.0, 1.0, ""+pt.id );
		ret.putFeature(Spot.FRAME, Double.parseDouble(frame));
		return ret;
	}
	
	public IdPoint getIdPoint(Spot s)
	{
		return new IdPoint(s.getFeature(Spot.POSITION_X).intValue(), s.getFeature(Spot.POSITION_Y).intValue(), s.getFeature(Spot.FRAME).intValue());
	}
	
	public Map<String,Object> getSimpleTrackerSettings(Double maxLinkingDistance, Double maxGapDistance, Integer maxGapFrames)
	{
		Map<String, Object> settings = new HashMap<String, Object>();
		// Linking
		settings.put(KEY_LINKING_FEATURE_PENALTIES, DEFAULT_LINKING_FEATURE_PENALTIES);
		// Gap closing
		settings.put(KEY_ALLOW_GAP_CLOSING, true);
		settings.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, DEFAULT_GAP_CLOSING_FEATURE_PENALTIES);
		// Track splitting
		settings.put(KEY_ALLOW_TRACK_SPLITTING, false);
		settings.put(KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE);
		settings.put(KEY_SPLITTING_FEATURE_PENALTIES, DEFAULT_SPLITTING_FEATURE_PENALTIES);
		// Track merging
		settings.put(KEY_ALLOW_TRACK_MERGING, false);
		settings.put(KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE);
		settings.put(KEY_MERGING_FEATURE_PENALTIES, DEFAULT_MERGING_FEATURE_PENALTIES);
		// Others
		settings.put(KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE);
		settings.put(KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR);
		settings.put(KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE);
		// Panel ones
		settings.put(KEY_LINKING_MAX_DISTANCE, maxLinkingDistance);
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, maxGapDistance);
		settings.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, maxGapFrames);
		// Hop!
		return settings;
	}
}

package function.singleCellAnalysis;

import image.roi.IdPoint;
import image.roi.PointList;

import java.awt.Point;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import tables.DimensionMap;

public class TrackHash implements Comparator<PointList> {
	
	HashMap<DimensionMap,HashMap<IdPoint,PointList>> trackHash = new HashMap<DimensionMap,HashMap<IdPoint,PointList>>();
	
	public void updateTracks(DimensionMap lDim)
	{
		HashMap<IdPoint,PointList> newTracks = new HashMap<IdPoint,PointList>();
		HashMap<IdPoint,PointList> tracks = this.getTracks(lDim);
		if(tracks == null)
		{
			return; // nothing to update
		}
		for (Point p : tracks.keySet())
		{
			PointList track = tracks.get(p);
			newTracks.put(track.lastElement(), track);
		}
		this.trackHash.put(lDim, newTracks);
	}
	
	public void putPoint(DimensionMap lDim, IdPoint lastPoint, IdPoint thisPoint)
	{
		HashMap<IdPoint,PointList> tracks = this.getTracks(lDim);
		if(tracks == null)
		{
			tracks = new HashMap<IdPoint,PointList>();
		}
		PointList track = tracks.get(lastPoint);
		if(track == null)
		{
			track = new PointList();
			track.add(lastPoint);
		}
		track.add(thisPoint);
		tracks.put(lastPoint, track);
		this.trackHash.put(lDim, tracks);
	}
	
	public PointList getTrack(DimensionMap lDim, IdPoint p)
	{
		HashMap<IdPoint,PointList> tracks = this.getTracks(lDim);
		if(tracks == null)
		{
			return null;
		}
		return tracks.get(p);
	}
	
	public HashMap<IdPoint,PointList> getTracks(DimensionMap lDim)
	{
		HashMap<IdPoint,PointList> tracks = this.trackHash.get(lDim);
		return tracks;
	}
	
	public List<PointList> getTracksList(DimensionMap lDim)
	{
		HashMap<IdPoint,PointList> tracks = this.getTracks(lDim);
		List<PointList> ret = new Vector<PointList>();
		if(tracks == null)
		{
			return ret;
		}
		for (IdPoint p : tracks.keySet())
		{
			ret.add(tracks.get(p));
		}
		Collections.sort(ret, new TrackHash());
		return ret;
		
	}
	
	public int compare(PointList pl1, PointList pl2)
	{
		Point origin = new Point(0, 0);
		return (int) Math.signum(origin.distance(pl1.get(0)) - origin.distance(pl2.get(0)));
	}
	
	public void clear()
	{
		this.trackHash.clear();
	}
	
}
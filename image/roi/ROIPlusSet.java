package image.roi;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import miscellaneous.Copiable;
import tables.DimensionMap;

public class ROIPlusSet implements ROIDisplayable, Copiable<ROIPlusSet> {
	
	private TreeMap<DimensionMap,ROIPlus> rois;
	private int type;
	private String name;
	
	public ROIPlusSet()
	{
		this.rois = new TreeMap<DimensionMap,ROIPlus>();
		this.type = ROIPlus.ROI_UNDEFINED;
	}
	
	public ROIPlusSet(TreeMap<DimensionMap,ROIPlus> rois)
	{
		this();
		this.rois = rois;
		if(this.rois.firstEntry() != null)
		{
			this.type = this.rois.firstEntry().getValue().type;
		}
	}
	
	public Map<DimensionMap,ROIPlus> getRoiMap()
	{
		return this.rois;
	}
	
	public ROIPlus getRoiForDimension(DimensionMap map)
	{
		return rois.get(map);
	}
	
	public PointList getEditablePointsForDimension(DimensionMap map)
	{
		ROIPlus roip = rois.get(map);
		if(roip == null)
			return null;
		return roip.getPointList();
	}
	
	public List<PointList> getPatternedRoisForDimension(DimensionMap map)
	{
		Vector<PointList> ret = new Vector<PointList>();
		if(this.type == ROIPlus.ROI_POINT)
		{
			PointList temp = new PointList();
			ROIPlus roi = rois.get(map);
			if(roi != null)
			{
				for (ROIPlus r : roi)
				{
					temp.addAll(r.getPointList());
				}
				ret.add(temp);
			}
		}
		else
		{
			ROIPlus roip = rois.get(map);
			if(roip == null)
				return ret;
			Iterator<PointList> itr = roip.patternPointListIterator();
			while (itr.hasNext())
			{
				ret.add(itr.next());
			}
		}
		return ret;
	}
	
	public void setEditablePointsForDimension(PointList points, DimensionMap map)
	{
		ROIPlus roi = rois.get(map);
		if(roi != null)
			roi.pointList = points;
		else
			this.rois.put(map, new ROIPlus(points, this.type()));
	}
	
	public List<PointList> getEditablePointsForDimensions(List<DimensionMap> maps)
	{
		List<PointList> result = new Vector<PointList>(0);
		if(rois == null || rois.size() == 0)
			return result;
		
		for (DimensionMap map : maps)
		{
			ROIPlus roip = rois.get(map);
			if(roip == null)
				continue;
			result.add(roip.getPointList());
		}
		return result;
	}
	
	public void setRoiForDimension(ROIPlus roi, DimensionMap map)
	{
		this.rois.put(map, roi);
	}
	
	public List<ROIPlus> getRoisForDimensions(List<DimensionMap> maps)
	{
		List<ROIPlus> result = new Vector<ROIPlus>(0);
		if(rois == null || rois.size() == 0)
			return result;
		
		for (DimensionMap map : maps)
		{
			ROIPlus roip = rois.get(map);
			if(roip == null)
				continue;
			result.add(roip);
		}
		return result;
	}
	
	public void setType(int type)
	{
		this.type = type;
	}
	
	public int type()
	{
		return this.type;
	}
	
	public String name()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public ROIPlusSet copy()
	{
		ROIPlusSet ret = new ROIPlusSet();
		ret.setName(this.name());
		ret.setType(this.type());
		for (DimensionMap map : this.rois.keySet())
		{
			ret.setRoiForDimension(this.rois.get(map).copy(), map);
		}
		return ret;
	}
	
}

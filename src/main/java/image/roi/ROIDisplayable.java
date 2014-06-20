package image.roi;

import java.util.List;
import java.util.Map;

import tables.DimensionMap;

public interface ROIDisplayable {
	
	public Map<DimensionMap,ROIPlus> getRoiMap();
	
	public ROIPlus getRoiForDimension(DimensionMap map);
	
	public void setRoiForDimension(ROIPlus roi, DimensionMap map);
	
	public void setEditablePointsForDimension(PointList points, DimensionMap map);
	
	public PointList getEditablePointsForDimension(DimensionMap map);
	
	public List<PointList> getEditablePointsForDimensions(List<DimensionMap> maps);
	
	public List<ROIPlus> getRoisForDimensions(List<DimensionMap> map);
	
	public int type();
	
	public String name();
	
	public void setName(String name);
	
}

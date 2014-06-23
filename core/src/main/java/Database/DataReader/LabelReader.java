package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

public class LabelReader {
	
	public static String readLabelName(JEXData label)
	{
		if(!label.getDataObjectType().equals(JEXData.LABEL))
			return null;
		return label.getDataObjectName();
	}
	
	public static String readLabelValue(JEXData label)
	{
		if(!label.getDataObjectType().equals(JEXData.LABEL))
			return null;
		JEXDataSingle ds = label.getFirstSingle();
		String value = ds.get(JEXDataSingle.VALUE);
		return value;
	}
	
	public static String readLabelUnit(JEXData label)
	{
		if(!label.getDataObjectType().equals(JEXData.LABEL))
			return null;
		JEXDataSingle ds = label.getFirstSingle();
		String value = ds.get(JEXDataSingle.UNIT);
		return value;
	}
	
}

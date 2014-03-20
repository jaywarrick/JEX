package miscellaneous;

import java.util.List;

public class CSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	private static String sepReg = "\\,";
	private static String sep = ",";
	
	public CSVList()
	{
		super(sepReg, sep);
	}
	
	public CSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	public CSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	public CSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	public CSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}

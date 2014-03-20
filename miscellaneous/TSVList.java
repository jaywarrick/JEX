package miscellaneous;

import java.util.List;

/**
 * Tab Separated Variable List
 * 
 * @author warrick
 * 
 */
public class TSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	private static String sepReg = "\\t";
	private static String sep = "\t";
	
	public TSVList()
	{
		super(sepReg, sep);
	}
	
	public TSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	public TSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	public TSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	public TSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}
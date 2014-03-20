package miscellaneous;

import java.util.List;

public class DSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	private static String sepReg = "\\-";
	private static String sep = "-";
	
	public DSVList()
	{
		super(sepReg, sep);
	}
	
	public DSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	public DSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	public DSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	public DSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}

package miscellaneous;

import java.util.List;

public class LSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	private static String sepReg = "\\n";
	private static String sep = "\n";
	
	public LSVList()
	{
		super(sepReg, sep);
	}
	
	public LSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	public LSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	public LSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	public LSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}

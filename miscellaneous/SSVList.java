package miscellaneous;

import java.util.List;

public class SSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	private static String sepReg = "\\;";
	private static String sep = ";";
	
	public SSVList()
	{
		super(sepReg, sep);
	}
	
	public SSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	public SSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	public SSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	public SSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}

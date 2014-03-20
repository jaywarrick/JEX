package miscellaneous;

import java.util.List;

public class PSVList extends SVList {
	
	private static final long serialVersionUID = 1L;
	private static String sepReg = "\\.";
	private static String sep = ".";
	
	public PSVList()
	{
		super(sepReg, sep);
	}
	
	public PSVList(String svl)
	{
		super(svl, sepReg, sep);
	}
	
	public PSVList(List<String> values)
	{
		super(values, sepReg, sep);
	}
	
	public PSVList(List<String[]> values, int arrayIndex)
	{
		super(values, arrayIndex, sepReg, sep);
	}
	
	public PSVList(Object[] s)
	{
		super(s, sepReg, sep);
	}
}

package miscellaneous;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class SVList extends Vector<String> {
	
	private static final long serialVersionUID = 1L;
	protected String separatorRegex;
	protected String separator;
	
	public SVList(String sepReg, String sep)
	{
		super();
		this.separatorRegex = sepReg;
		this.separator = sep;
	}
	
	public SVList(String svl, String sepReg, String sep)
	{
		this(sepReg, sep);
		this.add(svl.split(this.separatorRegex));
	}
	
	public SVList(List<String> values, String sepReg, String sep)
	{
		this(sepReg, sep);
		this.addAll(values);
	}
	
	public SVList(List<String[]> values, int arrayIndex, String sepReg, String sep)
	{
		this(sepReg, sep);
		if(values == null)
		{
			return;
		}
		if(arrayIndex < 0 || arrayIndex >= values.size())
		{
			System.out.println("Warning in SVList(List<String[], int arrayIndex) - invalid arrayIndex");
			return;
		}
		Iterator<String[]> itr = values.iterator();
		while (itr.hasNext())
		{
			this.add(itr.next()[arrayIndex]);
		}
	}
	
	public SVList(Object[] s, String sepReg, String sep)
	{
		this(sepReg, sep);
		this.add(s);
	}
	
	public void prepend(String svl)
	{
		SVList toAdd = new SVList(svl, this.separatorRegex, this.separator);
		for (int i = toAdd.size() - 1; i >= 0; i--)
		{
			this.add(0, toAdd.get(i));
		}
	}
	
	public void append(String svl)
	{
		SVList toAdd = new SVList(svl, this.separatorRegex, this.separator);
		Iterator<String> itr = toAdd.iterator();
		while (itr.hasNext())
		{
			this.add(itr.next());
		}
	}
	
	@SuppressWarnings({ "unused", "rawtypes" })
	private void add(List values)
	{
		// if(values == null) return;
		// if the list hold Object[]'s big enough, add the second item
		if(values.size() > 0 && values.get(0).getClass().isArray() && Array.getLength(values.get(0)) > 1)
		{
			Iterator itr = values.iterator();
			while (itr.hasNext())
			{
				this.add(((Object[]) itr.next())[1].toString());
			}
		}
		// if the list hold some other type of non-array object
		else if(values.size() > 0 && !(values.get(0).getClass().isArray()))
		{
			Iterator itr = values.iterator();
			while (itr.hasNext())
			{
				this.add(itr.next().toString());
			}
		}
	}
	
	private void add(Object[] s)
	{
		// if(s == null) return;
		for (int i = 0; i < s.length; i++)
		{
			this.add(s[i].toString());
		}
	}
	
	public String toSVString()
	{
		// Using a string builder is REDICULOUSLY faster. Writing the points for a big roi went from 5 mins to <1 sec !!!!
		if(this.size() > 0)
		{
			StringBuilder ret = new StringBuilder();
			ret.append(this.get(0));
			for (int i = 1; i < this.size(); i++)
			{
				ret.append(this.separator);
				ret.append(this.get(i));
			}
			return ret.toString();
		}
		return "";
	}
	
	public String[] toStringArray()
	{
		String[] ret = this.toSVString().split(this.separatorRegex);
		return ret;
	}
	
	public List<String[]> toFields(String prefix)
	{
		List<String[]> ret = new Vector<String[]>();
		for (int i = 0; i < this.size(); i++)
		{
			ret.add(new String[] { prefix + i, this.get(i) });
		}
		if(ret.size() == 0)
		{
			return null;
		}
		return ret;
	}
	
	public List<String[]> mergePairs(SVList l)
	{
		if(this.size() != l.size())
		{
			System.out.println("Warning in SVList:mergePairs - arguments should be same size");
			return null;
		}
		List<String[]> ret = new Vector<String[]>();
		for (int i = 0; i < this.size(); i++)
		{
			ret.add(new String[] { this.get(i), l.get(i) });
		}
		return ret;
	}
	
	@Override
	public String toString()
	{
		return this.toSVString();
	}
	
	public boolean equals(SVList l)
	{
		return this.toSVString().equals(l.toSVString());
	}
}

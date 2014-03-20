package Database.Definition;
import miscellaneous.StringUtility;


public class Type implements Comparable<Type> {
	
	private String type = null;
	private String flavor = null;
	
	public Type()
	{
		super();
	}
	
	public Type(Type baseType, String flavor)
	{
		this(baseType.getType(), flavor);
	}
	
	public Type(String type, String flavor)
	{
		super();
		if(type != null)
		{
			this.type = type;
		}
		if(flavor != null)
		{
			this.flavor = flavor;
		}
	}
	
	public Type(String type)
	{
		super();
		if(type != null)
		{
			String[] typeInfo = type.split("\\:");
			if(typeInfo != null)
			{
				if(typeInfo.length == 1)
				{
					this.type = type;
				}
				else if(typeInfo.length > 1)
				{
					this.type = typeInfo[0];
					this.flavor = typeInfo[1];
				}
			}
		}
	}
	
	public int hashCode()
	{
		return this.toString().hashCode();
	}
	
	public String toString()
	{
		if (this.getFlavor() == null)
		{
			return this.getType();
		}
		else
		{
			return this.getType() + ":" + this.getFlavor();
		}
	}
	
	public String getType()
	{
		return this.type;
	}
	
	public void setType(String type)
	{
		this.type = type;
	}
	
	/**
	 * Checks to see if the type variable of the two Type objects are equal.
	 * The flavor field can be different.
	 * 
	 * To check if type and flavor are equal, use equals(Object o)
	 * @param t
	 * @return
	 */
	public boolean matches(Type t)
	{
		if(t == null)
		{
			return false;
		}
		return this.getType().equals(t.getType());
	}
	
	public String getFlavor()
	{
		return this.flavor;
	}
	
	public void setFlavor(String flavor)
	{
		this.flavor = flavor;
	}

	@Override
	public int compareTo(Type o)
	{
		if(o == null)
		{
			return -1;
		}
		String thisString = this.toString();
		String thatString = o.toString();
		return StringUtility.compareString(thisString, thatString);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Type))
		{
			return false;
		}
		return this.compareTo((Type) o) == 0;
	}
}

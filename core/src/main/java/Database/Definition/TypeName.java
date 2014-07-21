package Database.Definition;

import miscellaneous.CSVList;

public class TypeName implements Comparable<TypeName> {
	
	private Type   type;
	private String name;
	private int dimension;
	
	// ---------------------------------------------
	// Creators
	// ---------------------------------------------
	
	public TypeName(Type type, String name)
	{
		this.type      = type;
		this.name      = name;
		this.dimension = 0;
	}
	
	public TypeName(Type type, String name, int dimension)
	{
		this.type = type;
		this.name = name;
		this.dimension = dimension;
	}
	
	public TypeName(CSVList csvTN)
	{
		if(csvTN.size() >= 1)
			this.type = new Type(csvTN.get(0));
		if(csvTN.size() >= 2)
			this.name = csvTN.get(1);
		if(csvTN.size() >= 3)
			this.dimension = Integer.parseInt(csvTN.get(2));
	}
	
	public TypeName(String tnStr)
	{
		CSVList csv = new CSVList(tnStr);
		if(csv.size() >= 1)
			this.type = new Type(csv.get(0)); 
		if(csv.size() >= 2)
			this.name = csv.get(1);
		if(csv.size() >= 3)
		{
			String theName = "";
			for (int i = 1; i < csv.size() - 1; i++)
			{
				if(i == 1)
					theName = csv.get(i);
				else
					theName = theName + "," + csv.get(i);
			}
			this.name = theName;
			this.dimension = Integer.parseInt(csv.get(csv.size() - 1));
		}
	}
	
	// ---------------------------------------------
	// Getter / Setters
	// ---------------------------------------------
	/**
	 * Set the type field
	 * 
	 * @param type
	 */
	public void setType(Type type)
	{
		this.type = type;
	}
	
	/**
	 * Set the name field
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Return the type field
	 * 
	 * @return type
	 */
	public Type getType()
	{
		return new Type(type.getType(), type.getFlavor());
	}
	
	/**
	 * Return the name field
	 * 
	 * @return name
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Return the dimension field
	 * 
	 * @return type
	 */
	public int getDimension()
	{
		return dimension;
	}
	
	/**
	 * Return the dimension field
	 * 
	 * @return name
	 */
	public void setDimension(int dimension)
	{
		this.dimension = dimension;
	}
	
	/**
	 * Output typename as a csv list
	 * 
	 * @return csv format of typename
	 */
	public CSVList toCSV()
	{
		CSVList result = new CSVList();
		result.add(this.getType().toString());
		result.add(this.getName());
		result.add("" + this.getDimension());
		return result;
	}
	
	/**
	 * Return string value of csv list representing this typename
	 * 
	 * @return string value of csv list
	 */
	public String toCSVString()
	{
		CSVList csv = this.toCSV();
		return csv.toString();
	}
	
	@Override
	public String toString()
	{
		return this.getType() + "-" + this.getName();
	}
	
	public TypeName duplicate()
	{
		return new TypeName(type, name);
	}
	
	@Override
	public int hashCode()
	{
		String decript = this.getType() + "" + this.getName();
		return decript.hashCode();
	}
	
	public int compareTo(TypeName o)
	{
		TypeName f = o;
		String str1 = this.getType() + "" + this.getName();
		String str2 = f.getType() + "" + f.getName();
		return str1.compareTo(str2);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof TypeName))
			return false;
		
		TypeName f = (TypeName) o;
		String str1 = this.getType() + "" + this.getName();
		String str2 = f.getType() + "" + f.getName();
		return str1.equals(str2);
	}
}

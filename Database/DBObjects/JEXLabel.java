package Database.DBObjects;

import Database.Definition.Type;
import tables.DimensionMap;

public class JEXLabel extends JEXData {
	
	String value;
	String unit;
	
	public JEXLabel(String name, String value, String unit)
	{
		super(JEXData.LABEL, name);
		setLabelValue(value);
		setLabelUnit(unit);
	}
	
	/**
	 * Set the value of the label
	 * 
	 * @param value
	 */
	public void setLabelValue(String value)
	{
		JEXDataSingle ds = this.getFirstSingle();
		if(ds == null)
		{
			ds = new JEXDataSingle();
			this.addData(new DimensionMap(), ds);
		}
		ds.put(JEXDataSingle.VALUE, value);
		this.value = value;
	}
	
	/**
	 * Return the value of the label
	 * 
	 * @return string value of label
	 */
	public String getLabelValue()
	{
		return this.value;
	}
	
	/**
	 * Set the unit of the label
	 * 
	 * @param unit
	 */
	public void setLabelUnit(String unit)
	{
		JEXDataSingle ds = this.getFirstSingle();
		if(ds == null)
		{
			ds = new JEXDataSingle();
			this.addData(new DimensionMap(), ds);
		}
		ds.put(JEXDataSingle.UNIT, unit);
		this.unit = unit;
	}
	
	/**
	 * Return the unit of the label
	 * 
	 * @return unit of the label
	 */
	public String getLabelUnit()
	{
		return this.unit;
	}
	
	/**
	 * Override the get type method of JEXData
	 * 
	 * @return label type
	 */
	@Override
	public Type getDataObjectType()
	{
		return JEXData.LABEL;
	}
	
	/**
	 * Return a representative string of the label
	 * 
	 * @return string representation
	 */
	@Override
	public String toString()
	{
		// return ""+this.getLabelValue();
		return "" + this.getLabelValue();// +" "+this.getLabelUnit();
	}
	
	/**
	 * Override the equals method
	 */
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof JEXLabel)
		{
			if(this.toString().equals(((JEXLabel) o).toString()))
				return true;
		}
		return false;
	}
	
	/**
	 * Hash code material
	 */
	@Override
	public int hashCode()
	{
		if(this.getDataID() == null)
			return this.getTypeName().hashCode();
		else
		{
			String code = this.getDataID() + "-" + this.getTypeName().toString() + "-" + this.getLabelValue() + this.getLabelUnit();
			return code.hashCode();
		}
	}
	
}

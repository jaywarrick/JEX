package preferences;

/**
 * Converter.<br>
 * Equivalent to the Apache beanutils converter
 */
public interface Converter {
	
	/**
	 * Converts <code>value</code> to an object of <code>type</code>.
	 * 
	 * @param type
	 * @param value
	 * @return <code>value</code> converted to an object of <code>type</code>.
	 */
	@SuppressWarnings("rawtypes")
	public Object convert(Class type, Object value);
	
}

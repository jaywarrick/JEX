package function.plugin.plugins.featureExtraction;

import java.util.Iterator;

public class LabelGenerator implements Iterator<Integer>
{
	private int current = 0;

	@Override
	public boolean hasNext()
	{
		if(current < Integer.MAX_VALUE-1)
		{
			return true;
		}
		return false;
	}
	
	@Override
	public Integer next()
	{
		current = current + 1;
		return current;
	}
	
	@Override
	public void remove()
	{
		// Do nothing
	}
}
package miscellaneous;

public class Pair<E,T> {
	
	public E p1;
	public T p2;
	
	public Pair()
	{
		this.p1 = null;
		this.p2 = null;
	}
	
	public Pair(E p1, T p2)
	{
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public E object1()
	{
		return this.p1;
	}
	
	public T object2()
	{
		return this.p2;
	}
	
	public boolean equals(Object o)
	{
		if(o instanceof Pair)
		{
			@SuppressWarnings("rawtypes")
			Pair p = (Pair) o;
			return ((p.object1() == this.p2 && p.object2() == this.p2) || (p.object1() == this.p2 && p.object2() == this.p1));
		}
		return false;
	}
	
	public Pair<E,T> match(Pair<E,T> p)
	{
		if((p.p1 == this.p1 && p.p2 == this.p2) || (p.p1 == this.p2 && p.p2 == this.p1))
		{
			return new Pair<E,T>(this.p1, this.p2);
		}
		else if(p.p1 == this.p1 || p.p2 == this.p1)
		{
			return new Pair<E,T>(this.p1, null);
		}
		else if(p.p1 == this.p2 || p.p2 == this.p2)
		{
			return new Pair<E,T>(null, this.p2);
		}
		else
		{
			return new Pair<E,T>();
		}
	}
	
	public int size()
	{
		if(this.p1 != null && this.p1 != null)
		{
			return 2;
		}
		if(this.p1 == null && this.p2 == null)
		{
			return 0;
		}
		else
		{
			return 1;
		}
	}
	
	public String toString()
	{
		String ret = "[";
		if(p1 == null)
		{
			ret = ret + "null";
		}
		else
		{
			ret = ret + p1.toString();
		}
		ret = ret + ":";
		if(p2 == null)
		{
			ret = ret + "null";
		}
		else
		{
			ret = ret + p2.toString();
		}
		ret = ret + "]";
		return ret;
	}
	
}

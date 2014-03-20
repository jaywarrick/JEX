package signals;

public interface Observer {
	
	public void changing(Observable obj, String accessorName);
	
	public void changed(Observable obj, String accessorName);
	
	public void changeAborted(Observable obj, String accessorName);
	
}

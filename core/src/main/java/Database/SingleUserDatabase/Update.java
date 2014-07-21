package Database.SingleUserDatabase;

public interface Update {
	
	public void startUpdate() throws Exception;
	
	public void finishUpdate() throws Exception;
	
	public void finalizeUpdate();
	
}

package function;

public interface GraphicalCrunchingEnabling {
	
	public void runStep(int index);
	
	public void runNext();
	
	public void runPrevious();
	
	public int getStep();
	
	public void loopNext();
	
	public void loopPrevious();
	
	public void recalculate();
	
	public void startIT();
	
	public void finishIT();
	
}

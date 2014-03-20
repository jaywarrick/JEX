package function.tracker;

import image.roi.Trajectory;

import java.util.Comparator;

public class CompareTrajectories implements Comparator<Trajectory> {
	
	public static int LENGTH = 0;
	public static int VELOCITY = 1;
	public static int MOSTDIR = 2;
	public static int LESSDIR = 3;
	
	private int mode = 0;
	
	public CompareTrajectories(int mode)
	{
		this.mode = mode;
	}
	
	public int compare(Trajectory trajOne, Trajectory trajTwo)
	{
		if(mode == LENGTH)
		{
			if(trajOne.nbPoints() > trajTwo.nbPoints())
				return -1;
			if(trajOne.nbPoints() == trajTwo.nbPoints())
				return 0;
			return 1;
		}
		
		TrajectoryStatistics statOne = new TrajectoryStatistics(trajOne);
		TrajectoryStatistics statTwo = new TrajectoryStatistics(trajTwo);
		statOne.startAnalysis();
		statTwo.startAnalysis();
		
		if(mode == VELOCITY)
		{
			if(statOne.meanVelocity > statTwo.meanVelocity)
				return -1;
			if(statOne.meanVelocity == statTwo.meanVelocity)
				return 0;
			return 1;
		}
		else if(mode == MOSTDIR)
		{
			if(statOne.CI > statTwo.CI)
				return -1;
			if(statOne.CI == statTwo.CI)
				return 0;
			return 1;
		}
		else if(mode == LESSDIR)
		{
			if(statOne.CI < statTwo.CI)
				return -1;
			if(statOne.CI == statTwo.CI)
				return 0;
			return 1;
		}
		
		return 0;
	}
	
}
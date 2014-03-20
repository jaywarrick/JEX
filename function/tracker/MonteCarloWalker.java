package function.tracker;

import ij.ImagePlus;
import image.roi.Trajectory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import miscellaneous.StatisticsUtility;

import org.jfree.chart.ChartPanel;

public class MonteCarloWalker {
	
	// Walk mode
	public int mode = 1;
	public static int PERSISTENT_RANDOM_WALK = 3;
	
	// Walk parameters for a biased 2D Gaussian walk
	public static int BIASED_2D_GAUSSIAN = 1;
	public float velocityBias = 0;
	public float angleBias = 0;
	public float sigmaX = 1;
	public float sigmaY = 1;
	public float deltaT = 1;
	
	// Walk parameters for a biased random walk walk with only velocity and
	// angle picked randomly
	public static int BIASED_RANDOM_ANGLE_AND_VELOCITY = 2;
	public float meanVelocity = 0;
	public float meanAngle = 0;
	public float sigmaVelocity = 1;
	public float sigmaAngle = 1;
	
	// Visual parameters
	public float spf = 1;
	public float ump = 1;
	
	// Memory variables
	public Trajectory traj;
	public List<Point> walk;
	public Point start = new Point(0, 0);
	
	public MonteCarloWalker(int mode)
	{
		this.mode = mode;
	}
	
	/**
	 * Make a new biased 2D gaussian random walk
	 * 
	 * @param velocityBias
	 * @param angleBias
	 * @param sigmaX
	 * @param sigmaY
	 * @param deltaT
	 */
	public MonteCarloWalker(float velocityBias, float angleBias, float sigmaX, float sigmaY, float deltaT)
	{
		this.velocityBias = velocityBias;
		this.angleBias = angleBias;
		this.sigmaX = sigmaX;
		this.sigmaY = sigmaY;
		this.deltaT = deltaT;
		this.mode = BIASED_2D_GAUSSIAN;
	}
	
	/**
	 * Make a new biased random walk with a gaussian distribution on velocity and angle
	 * 
	 * @param meanVelocity
	 * @param sigmaVelocity
	 * @param meanAngle
	 * @param sigmaAngle
	 */
	public MonteCarloWalker(int mode, float meanVelocity, float sigmaVelocity, float meanAngle, float sigmaAngle, float deltaT)
	{
		this.meanVelocity = meanVelocity;
		this.meanAngle = meanAngle;
		this.sigmaVelocity = sigmaVelocity;
		this.sigmaAngle = sigmaAngle;
		this.deltaT = deltaT;
		this.mode = BIASED_RANDOM_ANGLE_AND_VELOCITY;
	}
	
	public static void reset()
	{
		// traj = null;
		// walk = null;
	}
	
	/**
	 * Walk the walker for n steps
	 * 
	 * @param n
	 * @return
	 */
	public void walk(int n)
	{
		List<Point> result = new ArrayList<Point>(0);
		result.add(start);
		Point nextP = start;
		for (int i = 0; i < n; i++)
		{
			nextP = nextStep(nextP);
			result.add(nextP);
		}
		walk = result;
		traj = this.makeTrajectory(result);
		traj.print();
	}
	
	/**
	 * Find the next step of the walker
	 * 
	 * @param p
	 * @return
	 */
	public Point nextStep(Point p)
	{
		int newX = 0;
		int newY = 0;
		if(mode == BIASED_2D_GAUSSIAN)
		{
			int dx = (int) (velocityBias * deltaT * Math.cos(Math.toRadians(angleBias)));
			int dy = (int) (velocityBias * deltaT * Math.sin(Math.toRadians(angleBias)));
			
			double dRandR = StatisticsUtility.gaussian(0, sigmaX);
			double dRandTh = StatisticsUtility.gaussian(0, sigmaY);
			
			int dRandX = (int) (dRandR * Math.cos(Math.toRadians(angleBias)) + dRandTh * Math.sin(Math.toRadians(angleBias)));
			int dRandY = (int) (dRandR * Math.sin(Math.toRadians(angleBias)) - dRandTh * Math.cos(Math.toRadians(angleBias)));
			
			newX = p.x + dx + dRandX;
			newY = p.y + dy + dRandY;
		}
		else if(mode == BIASED_RANDOM_ANGLE_AND_VELOCITY)
		{
			double dRandR = StatisticsUtility.gaussian(meanVelocity, sigmaVelocity);
			double dRandTh = StatisticsUtility.gaussian(meanAngle, sigmaAngle);
			
			newX = p.x + (int) (dRandR * deltaT * Math.cos(Math.toRadians(dRandTh)));
			newY = p.y + (int) (dRandR * deltaT * Math.sin(Math.toRadians(dRandTh)));
			
			// System.out.println("   MonteCarloWalker ---> velocity picked: "+dRandR+" angle picked: "+dRandTh);
		}
		
		Point result = new Point(newX, newY);
		return result;
	}
	
	/**
	 * Display the path of the walked on an image
	 */
	public void showWalk()
	{
		Vector<Double> xvalues = new Vector<Double>(0);
		Vector<Double> yvalues = new Vector<Double>(0);
		List<Point> points = traj.getPoints();
		for (Point gpt : points)
		{
			double x = gpt.getX();
			double y = gpt.getY();
			xvalues.add(x);
			yvalues.add(y);
		}
		ChartPanel chartpanel = FigureFactory.make_CART_LINE("Walker", "x", " units", xvalues, "y", " units", yvalues);
		BufferedImage bim = FigureFactory.createGraphic(chartpanel);
		ImagePlus im = new ImagePlus("Walker", bim);
		im.show();
	}
	
	/**
	 * Display the path of the walked on an image
	 */
	public BufferedImage printWalk()
	{
		Vector<Double> xvalues = new Vector<Double>(0);
		Vector<Double> yvalues = new Vector<Double>(0);
		List<Point> points = traj.getPoints();
		for (Point gpt : points)
		{
			double x = gpt.getX();
			double y = gpt.getY();
			xvalues.add(x);
			yvalues.add(y);
		}
		ChartPanel chartpanel = FigureFactory.make_XYSTEP_Chart("Walker", "x", " units", xvalues, "y", " units", yvalues);
		BufferedImage bim = FigureFactory.createGraphic(chartpanel);
		ImagePlus im = new ImagePlus("Walker", bim);
		im.show();
		return bim;
	}
	
	/**
	 * Return the bounding rectangle of this walk
	 * 
	 * @return
	 */
	public Rectangle getBounds()
	{
		List<Point> track = traj.getPoints();
		int minX = 0;
		int minY = 0;
		int maxX = 0;
		int maxY = 0;
		for (int k = 0, len = track.size(); k < len; k++)
		{
			Point p0 = track.get(k);
			minX = (minX > (int) p0.getX()) ? (int) p0.getX() : minX;
			maxX = (maxX < (int) p0.getX()) ? (int) p0.getX() : maxX;
			minY = (minY > (int) p0.getY()) ? (int) p0.getY() : minY;
			maxY = (maxY < (int) p0.getY()) ? (int) p0.getY() : maxY;
		}
		double thisDX = (maxX - minX);
		double thisDY = (maxY - minY);
		
		Rectangle result = new Rectangle(minX, minY, (int) thisDX, (int) thisDY);
		return result;
	}
	
	/**
	 * Add the walk to graphics g... scale to fit this walk
	 * 
	 * @param g
	 * @param dim
	 */
	public void displayShadeTrack(Graphics g, Dimension dim)
	{
		List<Point> track = traj.getPoints();
		int numberFrames = traj.length();
		
		int minX = 0;
		int minY = 0;
		int maxX = 0;
		int maxY = 0;
		for (int k = 0, len = track.size(); k < len; k++)
		{
			Point p0 = track.get(k);
			minX = (minX > (int) p0.getX()) ? (int) p0.getX() : minX;
			maxX = (maxX < (int) p0.getX()) ? (int) p0.getX() : maxX;
			minY = (minY > (int) p0.getY()) ? (int) p0.getY() : minY;
			maxY = (maxY < (int) p0.getY()) ? (int) p0.getY() : maxY;
		}
		double setDX = dim.getWidth();
		double setDY = dim.getHeight();
		double thisDX = (maxX - minX);
		double thisDY = (maxY - minY);
		double scaleX = setDX / thisDX;
		double scaleY = setDY / thisDY;
		double scale = Math.min(scaleX, scaleY);
		
		for (int k = 0, len = track.size(); k < len; k++)
		{
			Point p0 = track.get(k);
			
			int shade = 255 * k / (numberFrames);
			Color c = new Color(shade, shade, 255);
			g.setColor(c);
			
			int x0 = (int) ((p0.getX() - minX) * scale);
			int y0 = (int) ((p0.getY() - minY) * scale);
			g.fillOval(x0, y0, 3, 3);
			
			if(k < len - 1)
			{
				Point p1 = track.get(k + 1);
				int x1 = (int) ((p1.getX() - minX) * scale);
				int y1 = (int) ((p1.getY() - minY) * scale);
				
				g.drawLine(x0, y0, x1, y1);
				g.fillOval(x1, y1, 3, 3);
			}
		}
	}
	
	// display all the selected maxima
	public void displayShadeTrack(Graphics g, double scale, int height, int minX, int minY, Color color)
	{
		List<Point> track = traj.getPoints();
		int numberFrames = traj.length();
		
		for (int k = 0, len = track.size(); k < len; k++)
		{
			Point p0 = track.get(k);
			
			float shade = (float) k / ((float) numberFrames);
			int red = (int) (shade * color.getRed());
			int green = (int) (shade * color.getGreen());
			int blue = (int) (shade * color.getBlue());
			Color c = new Color(red, green, blue);
			g.setColor(c);
			
			int x0 = (int) ((p0.getX() - minX) * scale);
			int y0 = (int) (height - (p0.getY() - minY) * scale);
			g.fillOval(x0, y0, 3, 3);
			
			if(k < len - 1)
			{
				Point p1 = track.get(k + 1);
				int x1 = (int) ((p1.getX() - minX) * scale);
				int y1 = (int) (height - (p1.getY() - minY) * scale);
				
				g.drawLine(x0, y0, x1, y1);
				g.fillOval(x1, y1, 3, 3);
			}
		}
	}
	
	// display all the selected maxima
	public void displayShadeTrack(Graphics g, int index, double scale, int height, int minX, int minY, Color color)
	{
		List<Point> track = traj.getPoints();
		int numberFrames = traj.length();
		
		int len = Math.min(track.size(), index);
		for (int k = 0; k < len; k++)
		{
			// for (int k=0,len=track.size(); k<len; k++){
			Point p0 = track.get(k);
			
			float shade = (float) k / ((float) numberFrames);
			int red = (int) (shade * color.getRed());
			int green = (int) (shade * color.getGreen());
			int blue = (int) (shade * color.getBlue());
			Color c = new Color(red, green, blue);
			g.setColor(c);
			
			int x0 = (int) ((p0.getX() - minX) * scale);
			int y0 = (int) (height - (p0.getY() - minY) * scale);
			g.fillOval(x0, y0, 3, 3);
			
			if(k < len - 1)
			{
				Point p1 = track.get(k + 1);
				int x1 = (int) ((p1.getX() - minX) * scale);
				int y1 = (int) (height - (p1.getY() - minY) * scale);
				
				g.drawLine(x0, y0, x1, y1);
				g.fillOval(x1, y1, 3, 3);
			}
		}
	}
	
	/**
	 * Make a trajectory from a walk
	 * 
	 * @param walk
	 * @return
	 */
	public Trajectory makeTrajectory(List<Point> walk)
	{
		Trajectory traj = new Trajectory(walk);
		return traj;
	}
	
	public static void makeNWalker(int n)
	{
		MonteCarloWalker[] walkers = new MonteCarloWalker[n];
		for (int i = 0; i < n; i++)
		{
			MonteCarloWalker mcw = new MonteCarloWalker(BIASED_2D_GAUSSIAN);
			mcw.velocityBias = 3;
			mcw.angleBias = 0;
			mcw.sigmaX = 5;
			mcw.sigmaY = 5;
			mcw.deltaT = 1;
			
			mcw.walk(100);
			walkers[i] = mcw;
		}
		
		int width = 800;
		int height = 600;
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(width, height, type);
		Graphics g = bimage.createGraphics();
		g.setColor(Color.white);
		g.drawRect(0, 0, width, height);
		
		int minX = 0;
		int minY = 0;
		int maxX = 0;
		int maxY = 0;
		for (int i = 0; i < n; i++)
		{
			Rectangle r = walkers[i].getBounds();
			int X1 = (int) r.getX();
			int Y1 = (int) r.getY();
			int X2 = (int) r.getWidth() + X1;
			int Y2 = (int) r.getHeight() + Y1;
			minX = (minX > X1) ? X1 : minX;
			maxX = (maxX < X2) ? X2 : maxX;
			minY = (minY > Y1) ? Y1 : minY;
			maxY = (maxY < Y2) ? Y2 : maxY;
		}
		double setDX = width;
		double setDY = height;
		double thisDX = (maxX - minX);
		double thisDY = (maxY - minY);
		double scaleX = setDX / thisDX;
		double scaleY = setDY / thisDY;
		double scale = Math.min(scaleX, scaleY);
		if(scaleX > scaleY)
		{
			scale = scaleY;
			minX = minX - (int) (((width - thisDX * scaleY) / 2) / scaleY);
		}
		else
		{
			scale = scaleX;
			minY = minY - (int) (((height - thisDY * scaleX) / 2) / scaleX);
		}
		
		Random random = new Random();
		for (int i = 0; i < n; i++)
		{
			Color nextColor = Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F);
			walkers[i].displayShadeTrack(g, scale, height, minX, minY, nextColor);
		}
		
		g.dispose();
		ImagePlus im = new ImagePlus("", bimage);
		im.show();
		
	}
	
}
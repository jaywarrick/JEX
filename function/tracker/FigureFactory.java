package function.tracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PolarPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.DefaultPolarItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class FigureFactory {
	
	// Variables
	public double min = 0.0;
	public double maxX = -1;
	public double maxY = -1;
	public Color color = Color.red;
	public String type = HistogramFactory.CART_HISTOGRAM;
	
	public String title = "";
	public String xLabel = "";
	public String yLabel = "";
	public String xUnit = "";
	public String yUnit = "";
	
	public List<Double> xseries;
	public List<Double> yseries;
	
	// GUI components
	private ChartPanel chartPanel;
	
	public FigureFactory()
	{}
	
	public void createChart()
	{
		// Create the chartpanel
		chartPanel = null;
		if(type.equals(HistogramFactory.CART_HISTOGRAM))
		{
			chartPanel = make_CART_HISTOGRAM(title, xLabel, xUnit, xseries, yLabel, yUnit, yseries);
		}
		else if(type.equals(HistogramFactory.CART_LINE))
		{
			chartPanel = make_CART_LINE(title, xLabel, xUnit, xseries, yLabel, yUnit, yseries);
		}
		else if(type.equals(HistogramFactory.CART_SCATTER))
		{
			chartPanel = make_CART_SCATTER(title, xLabel, xUnit, xseries, yLabel, yUnit, yseries);
		}
		else if(type.equals(HistogramFactory.POLAR_HISTOGRAM))
		{
			chartPanel = make_POLAR_LINE(title, xLabel, xUnit, xseries, yLabel, yUnit, yseries, maxY);
		}
		else if(type.equals(HistogramFactory.POLAR_LINE))
		{
			chartPanel = make_POLAR_LINE(title, xLabel, xUnit, xseries, yLabel, yUnit, yseries, maxY);
		}
		else if(type.equals(HistogramFactory.POLAR_SCATTER))
		{
			chartPanel = make_POLAR_LINE(title, xLabel, xUnit, xseries, yLabel, yUnit, yseries, maxY);
		}
		else
			return;
		
		JFreeChart chart = chartPanel.getChart();
		if(chart == null)
			return;
		chart.setBackgroundPaint(Color.white);
	}
	
	public BufferedImage createBufferedImage()
	{
		createChart();
		return createGraphic(chartPanel);
	}
	
	// -----------------------------------------------
	// --------- JFREECHART CREATION -----------------
	// -----------------------------------------------
	
	/**
	 * Return an image form a graph
	 * 
	 * @param g
	 * @return
	 */
	public static BufferedImage createGraphic(ChartPanel chatpanel)
	{
		JFreeChart chart = chatpanel.getChart();
		if(chart == null)
			return null;
		BufferedImage bImage = chart.createBufferedImage(800, 600);
		return bImage;
	}
	
	/**
	 * Make the chart on a XY plane of points joined by a line
	 * 
	 * @param name
	 * @return
	 */
	public static ChartPanel make_CART_LINE(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createXYLineChart(title, xlabel, ylabel, data, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.white);
		
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		return plotChartPane;
	}
	
	/**
	 * Make the chart on a XY plane of points joined by a line
	 * 
	 * @param name
	 * @return
	 */
	public static ChartPanel make_XYSTEP_Chart(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createXYStepChart(title, xlabel, ylabel, data, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.white);
		
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		return plotChartPane;
	}
	
	/**
	 * Make a histogram on a XY plane
	 * 
	 * @param name
	 * @return
	 */
	public ChartPanel make_CART_HISTOGRAM(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		// Make the JFreeChart object
		JFreeChart chart = ChartFactory.createXYBarChart(title, xLabel, false, yLabel, data, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.white);
		
		// Make the graphical panel for the chart
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		// Set XYPlot options
		XYPlot plot = chart.getXYPlot();
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		
		// Set the Axis Ranges
		float Xmin = new Float(Xvalues.get(0));
		float Xmax = new Float(Xvalues.get(Xvalues.size() - 1));
		float range = Xmax - Xmin;
		Xmin = Xmin - (float) 0.1 * range;
		Xmax = Xmax + (float) 0.1 * range;
		ValueAxis vaxis = plot.getDomainAxis();
		if(maxX > 0)
			vaxis.setRange(0.0, maxX); // Set the range of displayed values
			
		// Set display options
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		double margin = Math.max(1 - 0.05 * range, 0);
		renderer.setMargin(margin);
		renderer.setSeriesPaint(0, color);
		renderer.setBaseFillPaint(Color.white);
		
		return plotChartPane;
	}
	
	/**
	 * Plot points on a XY plane
	 * 
	 * @param name
	 * @return
	 */
	public static ChartPanel make_CART_SCATTER(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createScatterPlot(title, xlabel, ylabel, data, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.white);
		
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		return plotChartPane;
	}
	
	/**
	 * Plot points on a r,theta plane
	 * 
	 * @param name
	 * @return
	 */
	public static ChartPanel make_POLAR_SCATTER(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createScatterPlot(title, xlabel, ylabel, data, PlotOrientation.VERTICAL, false, false, false);
		chart.setBackgroundPaint(Color.white);
		
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		return plotChartPane;
	}
	
	/**
	 * Plot points on a r,theta plane joined by a line
	 * 
	 * @param name
	 * @return
	 */
	public ChartPanel make_POLAR_LINE(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues, double maxy)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createPolarChart(title, data, false, true, false);
		chart.setBackgroundPaint(Color.white);
		
		// Set the polarplot options
		PolarPlot plot = (PolarPlot) chart.getPlot();
		plot.setAngleGridlinesVisible(true);
		plot.setRadiusGridlinesVisible(true);
		int cap = java.awt.BasicStroke.CAP_ROUND;
		int join = java.awt.BasicStroke.JOIN_ROUND;
		java.awt.BasicStroke newStrokeConcentric = new java.awt.BasicStroke((float) 0.5, cap, join, (float) 1.0, new float[] { (float) 1.0, 0 }, 0);
		java.awt.BasicStroke newStrokeRadial = new java.awt.BasicStroke((float) 1.0, cap, join, (float) 1.0, new float[] { (float) 1.0, 0 }, 0);
		plot.setRadiusGridlineStroke(newStrokeConcentric);
		plot.setAngleGridlineStroke(newStrokeRadial);
		
		// Set the range options
		ValueAxis axis = plot.getAxis();
		axis.setTickLabelsVisible(false);
		if(maxy > 0)
		{
			axis.setRange(0, maxy);
		}
		
		// Set display options
		DefaultPolarItemRenderer renderer = (DefaultPolarItemRenderer) plot.getRenderer();
		renderer.setSeriesFilled(0, true);
		renderer.setSeriesPaint(0, color);
		
		// Set the frame options
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		return plotChartPane;
	}
	
	/**
	 * Make a histogram on a r, theta plane
	 * 
	 * @param name
	 * @return
	 */
	public static ChartPanel make_PLOAR_HISTOGRAM(String title, String xlabel, String xunit, List<Double> Xvalues, String ylabel, String yunit, List<Double> Yvalues)
	{
		
		// store in series format
		XYSeries series = new XYSeries(title);
		for (int i = 0, len = Yvalues.size(); i < len; i++)
		{
			float Ai = new Float(Xvalues.get(i));
			float Vi = (Xvalues == null) ? i : new Float(Yvalues.get(i));
			series.add(Ai, Vi);
		}
		XYSeriesCollection data = new XYSeriesCollection(series);
		
		JFreeChart chart = ChartFactory.createPolarChart(title, data, false, true, false);
		chart.setBackgroundPaint(Color.white);
		
		ChartPanel plotChartPane = new ChartPanel(chart);
		plotChartPane.setBackground(Color.white);
		
		return plotChartPane;
	}
}

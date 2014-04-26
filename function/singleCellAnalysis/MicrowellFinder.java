package function.singleCellAnalysis;

import function.plugin.old.JEX_Filters;
import function.imageUtility.MaximumFinder;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.HashedPointList;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.awt.Shape;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.Copiable;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import miscellaneous.VectorUtility;

public class MicrowellFinder implements Comparator<IdPoint> {
	
	public static void main(String[] args)
	{
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");
		FloatProcessor imp = makeKernel(20, 13, false);
		ImagePlus im = FunctionUtility.makeImageToSave(imp, "false", 8);
		FileSaver fs = new FileSaver(im);
		String path = DirectoryManager.getUniqueAbsoluteTempPath("tif");
		fs.saveAsTiff(path);
		
		try
		{
			FileUtility.openFileDefaultApplication(path);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static FloatProcessor makeKernel(int outerRad, int innerRad, boolean inverted)
	{
		int out = outerRad * 2 + 1;
		// int in = innerRad*2+1;
		FloatProcessor ret = new FloatProcessor(out, out);
		float[][] pixels = new float[out][out];
		if(!inverted)
		{
			for (int c = 0; c < out; c++)
			{
				for (int r = 0; r < out; r++)
				{
					if(r < outerRad - innerRad || r > outerRad + innerRad + 1)
					{
						pixels[c][r] = 1f;
					}
					else if(c < outerRad - innerRad || c > outerRad + innerRad + 1)
					{
						pixels[c][r] = 1f;
					}
					else
					{
						pixels[c][r] = 0f;
					}
				}
			}
		}
		else
		{
			for (int c = 0; c < out; c++)
			{
				for (int r = 0; r < out; r++)
				{
					if(r < outerRad - innerRad || r > outerRad + innerRad + 1)
					{
						pixels[c][r] = 0f;
					}
					else if(c < outerRad - innerRad || c > outerRad + innerRad + 1)
					{
						pixels[c][r] = 0f;
					}
					else
					{
						pixels[c][r] = 1f;
					}
				}
			}
		}
		
		ret.setFloatArray(pixels);
		return ret;
	}
	
	public static Vector<FloatProcessor> filterAndConvolve(FloatProcessor imp, double radius, boolean edgeFilter, FloatProcessor kernel, boolean isTest)
	{
		Vector<FloatProcessor> results = null;
		if(isTest)
	{
			results = new Vector<FloatProcessor>();
			results.add(kernel);
		}
		
		// If it is a bright field image. Don't do any preprocessing.
		// Otherwise, it is fluorescence and we would like to perform a mean radius and edge finding filter.
		if(radius > 0)
		{
			// Filter the image using the given radius
			RankFilters rF = new RankFilters();
			rF.setup(JEX_Filters.MEAN, new ImagePlus("filtered", imp));
			rF.makeKernel(radius);
			rF.run(imp);
		}
		if(isTest)
		{
			results.add(new FloatProcessor(imp.getFloatArray()));
		}
		
		if(edgeFilter)
		{
			imp.findEdges();
		}
		if(isTest)
		{
			results.add(new FloatProcessor(imp.getFloatArray()));
		}
		// String path = JEXWriter.saveImage(imp);
		// try {
		// FileUtility.openFileDefaultApplication(path);
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		Convolver con = new Convolver();
		con.convolveFloat(imp, (float[]) kernel.getPixels(), kernel.getWidth(), kernel.getHeight());
		if(isTest)
		{
			results.add(imp);
		}
		return results;
	}
	
	public static PointList findMicrowellCentersInConvolvedImage(ImageProcessor imp, ROIPlus roi, double convThresh, double convTol, int gridSpacing, double gridTol, boolean removeBorderNodes, int minNumberOfWells, boolean interpolate, double smoothingTolerance)
	{
		PointList points = findMaxima(imp, convThresh, convTol, roi);
		PointList lattice = findLattice(points, gridSpacing, gridTol, removeBorderNodes, minNumberOfWells, interpolate, smoothingTolerance);
		return lattice;
	}
	
	private static PointList findMaxima(ImageProcessor imp, double threshold, double tol, ROIPlus roi)
	{
		MaximumFinder mf = new MaximumFinder();
		Roi r = null;
		if(roi != null && roi.getPointList().size() > 0)
		{
			r = roi.getRoi();
			imp.setRoi(r);
		}
		ROIPlus points = (ROIPlus) mf.findMaxima(imp, tol, threshold, MaximumFinder.ROI, true, false, r, true);
		
		// The findMaxima algorithm treats all ROIs as a rectangle; thus, we have to filter the points
		// further in case the roi is a polygon or some other non-rectangular shape.
		PointList ret = new PointList();
		if(roi != null && roi.getPointList().size() > 0)
		{
			Shape shape = roi.getShape();
			for (IdPoint p : points.getPointList())
			{
				if(shape.contains(p))
				{
					ret.add(p);
				}
			}
		}
		else
		{
			ret = points.getPointList();
		}
		Logs.log("Found " + ret.size() + " maxima.", 0, MicrowellFinder.class.getSimpleName());
		return ret;
	}
	
	private static PointList findLattice(PointList points, int spacing, double tol, boolean removeBorderNodes, int minNumberOfNodes, boolean interpolate, double smoothingTolerance)
	{
		HashedPointList hash = new HashedPointList(points);
		
		// The point with id of 0 in points is the point with the highest convolution score
		// Set this point as the initial root of the grid, expanding from there to fill the grid
		// with neighbors.
		IdPoint maxPoint = null;
		
		Grid grid = new Grid();
		for (int i = 0; i < points.size(); i++)
		{
			for (IdPoint p : points)
			{
				if(p.id == i)
				{
					maxPoint = p;
					break;
				}
			}
			GridNode root = new GridNode(maxPoint, 0, 0);
			grid.putNode(root);
			root.expandGrid(hash, spacing, tol, grid);
			// If the grid size is significant, then we found a good node to expand from
			// and we can quit. Otherwise try and new point to start from, making sure
			// to clear grid for the next try.
			if(grid.size() >= minNumberOfNodes)
			{
				break;
			}
			grid.clear();
		}
		
		Logs.log("Found " + grid.size() + " grid points.", 0, MicrowellFinder.class.getSimpleName());
		
		// interpolate grid points if possible and add them to grid
		if(interpolate)
		{
			grid.interpolateGridPoints();
		}
		
		// Remove border GridNodes if necessary
		if(removeBorderNodes)
		{
			Vector<GridNode> toRemove = new Vector<GridNode>();
			for (GridNode n : grid)
			{
				if(n.getNeighborCount() != 4)
				{
					toRemove.add(n);
				}
			}
			for (GridNode n : toRemove)
			{
				grid.removeNode(n.r, n.c);
			}
		}
		
		// interpolate grid points again if possible and add them to grid
		if(interpolate)
		{
			grid.interpolateGridPoints();
		}
		
		// Smooth outliers if necessary
		double err = smoothingTolerance * (spacing) / 100.0;
		if(err >= 1.0)
		{ // if the tolerance allows more than a pixel of change, then try to fix some nodes.
			boolean adjustedANode = true;
			while (adjustedANode)
			{
				Grid tempGrid = new Grid();
				for (GridNode node : grid)
				{
					GridNode tempNode = grid.getInterpolatedNode(node.r, node.c);
					if(tempNode != null)
					{
						tempGrid.putNode(tempNode);
					}
				}
				Vector<Double> dists = new Vector<Double>();
				Vector<GridNode> matches = new Vector<GridNode>();
				for (GridNode node : grid)
				{
					GridNode match = tempGrid.getNode(node.r, node.c);
					if(match != null)
					{
						matches.add(match);
						dists.add(Math.pow(node.p.x - match.p.x, 2) + Math.pow(node.p.y - match.p.y, 2));
					}
				}
				if(dists.size() == 0)
				{
					adjustedANode = false;
					continue;
				}
				Vector<Double> sortedDists = new Vector<Double>();
				sortedDists.addAll(dists);
				Collections.sort(sortedDists);
				Double outlier = sortedDists.get(sortedDists.size() - 1); // get the match with the most different location
				if(outlier > err)
				{
					int index = dists.indexOf(outlier);
					grid.putNode(matches.get(index));
				}
				else
				{
					adjustedANode = false;
				}
			}
		}
		
		// renumber after removal of border etc.
		grid.numberRowsColsAndSetIds();
		
		// Get the pointlist version of the grid
		PointList ret = new PointList();
		for (GridNode node : grid)
		{
			ret.add(node.p);
		}
		
		// Sort the points by id and return the pointlist
		Collections.sort(ret, new MicrowellFinder());
		return ret;
	}
	
	@Override
	public int compare(IdPoint p1, IdPoint p2)
	{
		return p1.id - p2.id;
	}
	
}

class GridNode implements Copiable<GridNode> {
	
	public IdPoint p;
	public GridNode N, E, S, W;
	public int r, c;
	boolean expanded = false;
	
	public GridNode(IdPoint p, int r, int c)
	{
		this.p = p;
		this.N = this.E = this.S = this.W = null;
		this.r = r;
		this.c = c;
	}
	
	public void expandGrid(HashedPointList points, int spacing, double tol, Grid grid)
	{
		this.expanded = true;
		this.findNeighbors(points, spacing, tol, grid);
		if(this.N != null && !this.N.expanded)
		{
			this.N.expandGrid(points, spacing, tol, grid);
		}
		if(this.E != null && !this.E.expanded)
		{
			this.E.expandGrid(points, spacing, tol, grid);
		}
		if(this.S != null && !this.S.expanded)
		{
			this.S.expandGrid(points, spacing, tol, grid);
		}
		if(this.W != null && !this.W.expanded)
		{
			this.W.expandGrid(points, spacing, tol, grid);
		}
	}
	
	public void calculateAndSetId(int nCols)
	{
		this.p.id = (this.c + this.r * nCols);
	}
	
	public void findNeighbors(HashedPointList points, int spacing, double tol, Grid grid)
	{
		IdPoint N = new IdPoint(this.p.x, this.p.y - spacing, this.p.id);
		IdPoint E = new IdPoint(this.p.x + spacing, this.p.y, this.p.id);
		IdPoint S = new IdPoint(this.p.x, this.p.y + spacing, this.p.id);
		IdPoint W = new IdPoint(this.p.x - spacing, this.p.y, this.p.id);
		
		int tolInt = (int) (tol * spacing / 100.0);
		N = points.getNearestInRange(N, tolInt, true);
		E = points.getNearestInRange(E, tolInt, true);
		S = points.getNearestInRange(S, tolInt, true);
		W = points.getNearestInRange(W, tolInt, true);
		
		// Check for neighbors and create new nodes if necessary
		GridNode NN = grid.getNode(this.r - 1, this.c);
		GridNode EN = grid.getNode(this.r, this.c + 1);
		GridNode SN = grid.getNode(this.r + 1, this.c);
		GridNode WN = grid.getNode(this.r, this.c - 1);
		if(N != null && NN == null)
		{ // If we find a new point and there isn't a node there, create it, connect it and add it to the grid
			NN = new GridNode(N, this.r - 1, this.c);
		}
		if(E != null && EN == null)
		{ // If we find a new point and there isn't a node there, create it, connect it and add it to the grid
			EN = new GridNode(E, this.r, this.c + 1);
		}
		if(S != null && SN == null)
		{ // If we find a new point and there isn't a node there, create it, connect it and add it to the grid
			SN = new GridNode(S, this.r + 1, this.c);
		}
		if(W != null && WN == null)
		{ // If we find a new point and there isn't a node there, create it, connect it and add it to the grid
			WN = new GridNode(W, this.r, this.c - 1);
		}
		
		// Make any necessary connections and put/reput non-null nodes in the grid.
		if(NN != null)
		{
			this.N = NN;
			NN.S = this;
			grid.putNode(NN);
		}
		if(EN != null)
		{
			this.E = EN;
			EN.W = this;
			grid.putNode(EN);
		}
		if(SN != null)
		{
			this.S = SN;
			SN.N = this;
			grid.putNode(SN);
		}
		if(WN != null)
		{
			this.W = WN;
			WN.E = this;
			grid.putNode(WN);
		}
	}
	
	public int getNeighborCount()
	{
		int count = 0;
		if(this.N != null)
		{
			count = count + 1;
		}
		if(this.E != null)
		{
			count = count + 1;
		}
		if(this.S != null)
		{
			count = count + 1;
		}
		if(this.W != null)
		{
			count = count + 1;
		}
		return count;
	}
	
	@Override
	public int hashCode()
	{
		return this.p.hashCode();
	}
	
	@Override
	public String toString()
	{
		String N = null;
		String E = null;
		String S = null;
		String W = null;
		if(this.N != null)
		{
			N = this.N.p.toString();
		}
		if(this.E != null)
		{
			E = this.E.p.toString();
		}
		if(this.S != null)
		{
			S = this.S.p.toString();
		}
		if(this.W != null)
		{
			W = this.W.p.toString();
		}
		
		return "{" + this.p.id + ", [(" + N + "),(" + E + "),(" + S + "),(" + W + ")]}";
	}
	
	@Override
	public GridNode copy()
	{
		GridNode ret = new GridNode(this.p.copy(), this.r, this.c);
		// Don't copy references to neighbors, will be fixed by Grid.connectNodes();
		ret.expanded = this.expanded;
		return ret;
	}
}

class Grid implements Iterable<GridNode> {
	
	public TreeMap<Integer,TreeMap<Integer,GridNode>> grid;
	
	public Grid()
	{
		this.grid = new TreeMap<Integer,TreeMap<Integer,GridNode>>();
	}
	
	public GridNode getNode(int r, int c)
	{
		GridNode ret = null;
		TreeMap<Integer,GridNode> temp = this.grid.get(r);
		if(temp == null)
		{
			return ret;
		}
		return temp.get(c);
	}
	
	public void removeNode(int r, int c)
	{
		TreeMap<Integer,GridNode> temp = this.grid.get(r);
		if(temp != null)
		{
			GridNode removed = temp.remove(c);
			if(removed != null)
			{
				this.disconnectNode(removed);
			}
			if(temp.size() == 0)
			{
				this.grid.remove(r);
			}
		}
	}
	
	public void putNode(GridNode node)
	{
		if(node == null)
		{
			Logs.log("What", this);
		}
		TreeMap<Integer,GridNode> temp = this.grid.get(node.r);
		if(temp == null)
		{
			temp = new TreeMap<Integer,GridNode>();
			this.grid.put(node.r, temp);
		}
		GridNode tempNode = temp.get(node.c);
		if(tempNode != null)
		{
			this.disconnectNode(tempNode);
		}
		temp.put(node.c, node);
		
		this.connectNodeToGrid(node);
	}
	
	public void numberRowsColsAndSetIds()
	{
		// Find the minimum row and column of any GridNode and set those to zero
		// adjusting all others appropriately to get their true row and column
		// positions in the grid
		
		if(this.size() == 0)
		{
			return;
		}
		
		Pair<Integer,Integer> rowInfo = this.minMaxRow();
		Pair<Integer,Integer> colInfo = this.minMaxCol();
		int minR = rowInfo.p1, minC = colInfo.p1, maxC = colInfo.p2;
		int nCols = maxC - minC + 1;
		
		Grid renumberedGrid = new Grid();
		for (GridNode n : this)
		{
			n.r = n.r - minR;
			n.c = n.c - minC;
			// Renumber the id's based on their row and column locations in the grid
			n.calculateAndSetId(nCols);
			renumberedGrid.putNode(n);
		}
		
		this.grid = renumberedGrid.grid;
	}
	
	// public void connectNodes()
	// {
	// for (GridNode n : this)
	// {
	// this.connectNodeToGrid(n);
	// }
	// }
	
	private void connectNodeToGrid(GridNode n)
	{
		// Check for neighbors and create new nodes if necessary
		GridNode NN = this.getNode(n.r - 1, n.c);
		GridNode EN = this.getNode(n.r, n.c + 1);
		GridNode SN = this.getNode(n.r + 1, n.c);
		GridNode WN = this.getNode(n.r, n.c - 1);
		
		// Make any necessary connections and put/reput non-null nodes in the grid.
		if(NN != null)
		{
			n.N = NN;
			NN.S = n;
		}
		if(EN != null)
		{
			n.E = EN;
			EN.W = n;
		}
		if(SN != null)
		{
			n.S = SN;
			SN.N = n;
		}
		if(WN != null)
		{
			n.W = WN;
			WN.E = n;
		}
	}
	
	private void disconnectNode(GridNode n)
	{
		if(n.N != null)
		{
			n.N.S = null;
			n.N = null;
		}
		if(n.E != null)
		{
			n.E.W = null;
			n.E = null;
		}
		if(n.S != null)
		{
			n.S.N = null;
			n.S = null;
		}
		if(n.W != null)
		{
			n.W.E = null;
			n.W = null;
		}
	}
	
	public int size()
	{
		int total = 0;
		for (Entry<Integer,TreeMap<Integer,GridNode>> e : this.grid.entrySet())
		{
			total = total + e.getValue().size();
		}
		return total;
	}
	
	public Pair<Integer,Integer> minMaxRow()
	{
		if(this.size() == 0)
		{
			return null;
		}
		
		int maxRow = VectorUtility.getMaximum(this.grid.keySet());
		int minRow = VectorUtility.getMinimum(this.grid.keySet());
		return new Pair<Integer,Integer>(minRow, maxRow);
	}
	
	public Pair<Integer,Integer> minMaxCol()
	{
		if(this.size() == 0)
		{
			return null;
		}
		
		Integer minCol = null;
		Integer maxCol = null;
		for (Entry<Integer,TreeMap<Integer,GridNode>> e : this.grid.entrySet())
		{
			for (GridNode node : e.getValue().values())
			{
				if(minCol == null || node.c < minCol)
				{
					minCol = node.c;
				}
				if(maxCol == null || node.c > maxCol)
				{
					maxCol = node.c;
				}
			}
		}
		return new Pair<Integer,Integer>(minCol, maxCol);
	}
	
	public int numRows()
	{
		Pair<Integer,Integer> rows = this.minMaxRow();
		return rows.p2 - rows.p1 + 1;
	}
	
	public int numCols()
	{
		Pair<Integer,Integer> cols = this.minMaxCol();
		return cols.p2 - cols.p1 + 1;
	}
	
	public void clear()
	{
		this.grid.clear();
	}
	
	public void interpolateGridPoints()
	{
		if(this.size() == 0)
		{
			return;
		}
		
		// Find the row and column indicies
		Pair<Integer,Integer> rows = this.minMaxRow();
		Pair<Integer,Integer> cols = this.minMaxCol();
		
		// First scan through grid going to the right and down.
		for (int r = rows.p1; r <= rows.p2; r++)
		{
			for (int c = cols.p1; c <= cols.p2; c++)
			{
				GridNode newNode = this.getInterpolatedNode(r, c);
				if(newNode != null)
				{
					this.putNode(newNode);
				}
			}
		}
		
		// Second scan through grid going to the left and up.
		for (int r = rows.p2; r <= rows.p1; r--)
		{
			for (int c = cols.p2; c <= cols.p1; c--)
			{
				GridNode newNode = this.getInterpolatedNode(r, c);
				if(newNode != null)
				{
					this.putNode(newNode);
				}
			}
		}
		
		// Third scan through will pick up any missing points at intersections
		for (int r = rows.p1; r <= rows.p2; r++)
		{
			for (int c = cols.p1; c <= cols.p2; c++)
			{
				GridNode newNode = this.getInterpolatedNode(r, c);
				if(newNode != null)
				{
					this.putNode(newNode);
				}
			}
		}
	}
	
	public GridNode getInterpolatedNode(int r, int c)
	{
		if(this.getNode(r, c) != null)
		{
			return null;
		}
		
		Point newLoc = this.getInterpolatedPosition(r, c);
		if(newLoc != null)
		{
			GridNode newNode = new GridNode(new IdPoint(newLoc, 0), r, c);
			newNode.calculateAndSetId(this.numCols());
			return newNode;
		}
		
		return null;
	}
	
	public Point getInterpolatedPosition(int r, int c)
	{
		Integer x = null, y = null;
		
		GridNode N = this.getNode(r - 1, c);
		GridNode E = this.getNode(r, c + 1);
		GridNode S = this.getNode(r + 1, c);
		GridNode W = this.getNode(r, c - 1);
		
		if(N != null && E != null && S != null && W != null)
		{
			x = ((N.p.x + S.p.x) + (E.p.x + W.p.x)) / 4;
			y = ((N.p.y + S.p.y) + (E.p.y + W.p.y)) / 4;
		}
		else if(N != null && S != null)
		{
			x = (N.p.x + S.p.x) / 2;
			y = (N.p.y + S.p.y) / 2;
		}
		else if(E != null && W != null)
		{ // use E, W
			x = (E.p.x + W.p.x) / 2;
			y = (E.p.y + W.p.y) / 2;
		}
		else
		{
			return null;
		}
		
		return new Point(x, y);
	}
	
	@Override
	public Iterator<GridNode> iterator()
	{
		Vector<GridNode> nodes = new Vector<GridNode>();
		for (Entry<Integer,TreeMap<Integer,GridNode>> e : this.grid.entrySet())
		{
			nodes.addAll(e.getValue().values());
		}
		return nodes.iterator();
	}
}

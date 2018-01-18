package function.ops.geometry;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import function.ops.stats.DefaultSpearmansRankCorrelationCoefficient;
import function.plugin.IJ2.IJ2PluginUtility;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

@Plugin(type = JEXOps.SymmetryCoefficients.class, priority = Priority.NORMAL_PRIORITY)
public class DefaultSymmetryCoefficients<T extends RealType<T>> extends AbstractBinaryFunctionOp<RandomAccessibleInterval<T>, LabelRegion<?>, TreeMap<String,Double>>
implements JEXOps.SymmetryCoefficients {
	
	@Parameter(required = false, min="1", max="10")
	int foldSymmetryMax = 4;
	
	@SuppressWarnings("rawtypes")
	private UnaryFunctionOp<IterableInterval, RealLocalizable> comOp;
	
	private DefaultSymmetryLineAngle<T> angleOp;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initialize()
	{
		comOp = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.CenterOfGravity.class, RealLocalizable.class, IterableInterval.class);
		angleOp = (DefaultSymmetryLineAngle) Functions.binary(IJ2PluginUtility.ij().op(), DefaultSymmetryLineAngle.class, ValuePair.class, LabelRegion.class, RandomAccessibleInterval.class, RealLocalizable.class, 1);
		
	}
	
	@Override
	public TreeMap<String, Double> calculate(RandomAccessibleInterval<T> image, LabelRegion<?> region) {
		
		// Get and set the center of mass around which to measurement symmetry.
		RealLocalizable com = null;
		if(image != null)
		{
			com = comOp.calculate(Regions.sample(region, image));
		}
		else
		{
			com = region.getCenterOfMass();
		}
		angleOp.setCenter(com);
		
		// Calculate symmetry angles and amplitudes for each fold symmetry
		TreeMap<Integer,ValuePair<Double,Double>> angleData = new TreeMap<>();
		for(int i = 1 ; i <= foldSymmetryMax; i++)
		{
			angleOp.setFoldSymmetry(i);
			
			// ValuePair A: symmetry angle, B: amplitude of symmetry
			ValuePair<Double,Double> temp = angleOp.calculate(region, image);
			angleData.put(i, temp);
		}
		
		TreeMap<Integer,TreeMap<Integer,Double>> rhoData = null;
		if(image != null)
		{
			
			// For each fold, find the average correlation coefficient
			TwinMirroredLabelRegionCursor<T> tc = new TwinMirroredLabelRegionCursor<>(GeomUtils.INTERP_LINEAR, GeomUtils.OOB_BORDER, 0d,0d,0d);
			tc.setImage(image);
			
			rhoData = new TreeMap<>();
			for(int i = 1 ; i <= foldSymmetryMax; i++)
			{
				double angle = angleData.get(i).getA();
				tc.setRegion(region, com, angle);
				double angleOffset = 2d*Math.PI/((double) i);
				TreeMap<Integer,Double> subCalcs = new TreeMap<>();
				for(int j = i ; j > 0; j--)
				{
					// Set the correct angle in the symmetry cursor
					double newAngle = GeomUtils.regularizeTheta(angle + angleOffset * ((double) (j-1)));
					tc.setSymmetryAngle(newAngle);
					
					// Calculate rho for this angle
					double rho = DefaultSpearmansRankCorrelationCoefficient.calculateSpearmanRank(tc);
					
					// Store this subcalculation
					subCalcs.put(j, rho);
					
					// Don't forget to reset this reusable cursor.
					tc.reset();
				}
				
				// Store the subcalcs
				rhoData.put(i, subCalcs);
			}
		}
		
		TreeMap<String,Double> ret = new TreeMap<>();
		for(int i = 1 ; i <= foldSymmetryMax; i++)
		{
			if(rhoData != null)
			{
				for(Entry<Integer,TreeMap<Integer,Double>> e1 : rhoData.entrySet())
				{
					for(Entry<Integer,Double> e2 : e1.getValue().entrySet())
					{
						ret.put("SymmetryCorrelation." + e1.getKey() + "." + e2.getKey(), e2.getValue());
					}
				}
			}
			ret.put("SymmetryAmplitude." + i, angleData.get(i).getB());
		}
		
		return ret;
	}
}
package function.plugin.plugins.featureExtraction.ops;

import net.imagej.ops.Op;

public interface ComputerWrapper<I,O> {
	
	public void compute(Class<? extends Op> op, I input, O output);

}

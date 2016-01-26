package image.roi;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.Sampler;

public interface PointSampler<T> extends Sampler<T>, RealLocalizable, RealPositionable {

}

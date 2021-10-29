package function.ops;

import net.imagej.ops.Op;

public class JEXOps {
	
	/**
	 * Base interface for "logicalEqual" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.Logic.LogicalEqual.class
	 * </pre>
	 */
	public interface LogicalEqual extends Op {
		String NAME = "logic.logicalEqual";
	}

	/**
	 * Base interface for "logicalNotEqual" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.Logic.LogicalNotEqual.class
	 * </pre>
	 */
	public interface LogicalNotEqual extends Op {
		String NAME = "logic.logicalNotEqual";
	}
	
	/**
	 * Base interface for "spearmansRankCorrelationCoefficient" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.SpearmansRankCorrelationCoefficient.class
	 * </pre>
	 */
	public interface SpearmansRankCorrelationCoefficient extends Op {
		String NAME = "stats.spearmansRankCorrelationCoefficient";
	}
	
	/**
	 * Base interface for "radiusOfGyration" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.RadiusOfGyration.class
	 * </pre>
	 */
	public interface RadiusOfGyrationSquared extends Op {
		String NAME = "stats.radiusOfGyrationSquared";
	}
	
	/**
	 * Base interface for "radialLocalization" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.RadialLocalization.class
	 * </pre>
	 */
	public interface RadialLocalization extends Op {
		String NAME = "stats.radiusOfGyration";
	}
	
	/**
	 * Base interface for "limits" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.Limits.class
	 * </pre>
	 */
	public interface Limits extends Op {
		String NAME = "stats.limits";
	}
	
	/**
	 * Base interface for "mad" (median absolute deviation) operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.MAD.class
	 * </pre>
	 */
	public interface MAD extends Op {
		String NAME = "stats.mad";
	}
	
	/**
	 * Base interface for "symmetrylineangle" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.SymmetryLineAngle.class
	 * </pre>
	 */
	public interface SymmetryLineAngle extends Op {
		String NAME = "geometry.symmetrylineangle";
	}
	
	/**
	 * Base interface for "symmetrycoefficients" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.SymmetryLineAngle.class
	 * </pre>
	 */
	public interface SymmetryCoefficients extends Op {
		String NAME = "geometry.symmetrycoefficients";
	}
	
	/**
	 * Base interface for "smallestenclosingcircle" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = JEXOps.SmallestEnclosingCircle.class
	 * </pre>
	 */
	public interface SmallestEnclosingCircle extends Op {
		String NAME = "geometry.smallestenclosingcircle";
	}
	
}

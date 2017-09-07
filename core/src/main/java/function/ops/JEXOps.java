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
	public interface RadiusOfGyration extends Op {
		String NAME = "stats.radiusOfGyration";
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
	
}

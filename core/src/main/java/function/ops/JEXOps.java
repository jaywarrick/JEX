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
	 * @Plugin(type = Ops.Logic.LogicalEqual.class
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
	 * @Plugin(type = Ops.Logic.LogicalNotEqual.class
	 * </pre>
	 */
	public interface LogicalNotEqual extends Op {
		String NAME = "logic.logicalNotEqual";
	}

	/**
	 * Base interface for "logicalNotEqual" operations.
	 * <p>
	 * Implementing classes should be annotated with:
	 * </p>
	 *
	 * <pre>
	 * @Plugin(type = Ops.Logic.LogicalNotEqual.class
	 * </pre>
	 */
	public interface SpearmansRankCorrelationCoefficient extends Op {
		String NAME = "stats.spearmansRankCorrelationCoefficient";
	}
	
}

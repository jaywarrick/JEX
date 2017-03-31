package function.ops;

import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractNamespace;
import net.imagej.ops.Namespace;
import net.imagej.ops.OpMethod;
import net.imglib2.type.numeric.RealType;

@Plugin(type = Namespace.class)
public class JEXNamespace extends AbstractNamespace {
	
	//////////////////////////////////
	/////////// Logic ////////////////
	//////////////////////////////////
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.Equal.class)
	public <I extends RealType<I>, O extends RealType<O>> O equal(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.Equal.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.LogicalEqual.class)
	public <I extends RealType<I>, O extends RealType<O>> O logicalEqual(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.LogicalEqual.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.GreaterThan.class)
	public <I extends RealType<I>, O extends RealType<O>> O greaterThan(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.GreaterThan.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.And.class)
	public <I extends RealType<I>, O extends RealType<O>> O and(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.And.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.GreaterThanOrEqual.class)
	public <I extends RealType<I>, O extends RealType<O>> O greaterThanOrEqual(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.GreaterThanOrEqual.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.LessThan.class)
	public <I extends RealType<I>, O extends RealType<O>> O lessThan(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.LessThan.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.NotEqual.class)
	public <I extends RealType<I>, O extends RealType<O>> O notEqual(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.NotEqual.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.LessThanOrEqual.class)
	public <I extends RealType<I>, O extends RealType<O>> O lessThanOrEqual(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.LessThanOrEqual.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.LogicalNotEqual.class)
	public <I extends RealType<I>, O extends RealType<O>> O logicalNotEqual(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.LogicalNotEqual.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.Or.class)
	public <I extends RealType<I>, O extends RealType<O>> O or(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.Or.class, out, in);
		return result;
	}
	
	@OpMethod(op = net.imagej.ops.logic.RealLogic.XOr.class)
	public <I extends RealType<I>, O extends RealType<O>> O xor(final O out, final I in) {
		@SuppressWarnings("unchecked")
		final O result =
			(O) ops().run(net.imagej.ops.logic.RealLogic.XOr.class, out, in);
		return result;
	}
	
	// -- Named methods --

		@Override
		public String getName() {
			return "jexNamespace";
		}
}

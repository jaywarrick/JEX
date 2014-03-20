package guiObject;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JScrollBar;

import logs.Logs;
import signals.SSCenter;

public class SignalScrollBar extends JScrollBar implements AdjustmentListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// statics
	public static final String SIG_SliderChanged_NULL = "valuechanged";
	
	private boolean signaling = true;
	
	public SignalScrollBar()
	{
		super();
		this.addAdjustmentListener(this);
	}
	
	public SignalScrollBar(int orientation, int value, int extent, int min, int max)
	{
		super(orientation, value, extent, min, max);
		this.addAdjustmentListener(this);
	}
	
	public void silentSetValue(int value)
	{
		this.signaling = false;
		this.setValue(value);
		this.signaling = true;
	}
	
	/**
	 * Emit signal when value changed
	 */
	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		if(signaling)
		{
			Logs.log("Scroll bar moving", 0, this);
			SSCenter.defaultCenter().emit(this, SIG_SliderChanged_NULL, (Object[]) null);
		}
	}
}
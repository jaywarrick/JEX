/*
 * SIMPLE VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: http://www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

public interface VideoOutputFormat {
	
	public VideoFileT getFileT();
	
	public VideoEncoderT getCoderT();
	
	public String getFileExtension();
}

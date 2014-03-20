/*
 * VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

public enum VideoEncoderT
{
	RGB("RGB (uncompressed)"), MJPG("Motion JPEG"), H261, H263, INDEO32, INDEO41, INDEO50, CINEPAK("Cinepak"), SORENSON("Sorenson"), SORENSON3("Sorenson 3"), YUV("YUV Component Video"), RLE("Run-Length Encoding"), JPG("Jpeg"), MJPGA("Motion Jpeg A"), MJPGB("Motion Jpeg B"), DMLJPG("OpenDML Jpeg"), DVCPAL("DVC PAL"), DVCNTSC("DVC NTSC"), DVCProPAL("DVC-Pro PAL"), DVCProNTSC("DVC-Pro NTSC"), AVRJPG, BMP("Windows BMP"), GIF, MSV1, MPEG4("MPEG-4");
	
	public final String description;
	
	VideoEncoderT()
	{
		this.description = this.toString();
	}
	
	VideoEncoderT(String desc)
	{
		this.description = desc;
	}
}
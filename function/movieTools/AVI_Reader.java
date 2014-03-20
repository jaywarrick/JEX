package function.movieTools;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Vector;

import javax.imageio.ImageIO;

/**
 * <pre>
 * ImageJ Plugin for reading an AVI file into an image stack
 *  (one slice per video frame)
 * 
 * 
 * Restrictions and Notes:
 *      - Only few formats supported:
 *          - uncompressed 8 bit with palette (=LUT)
 *          - uncompressed 8 & 16 bit grayscale
 *          - uncompressed 24 & 32 bit RGB (alpha channel ignored)
 *          - uncompressed 32 bit AYUV (alpha channel ignored)
 *          - various YUV 4:2:2 compressed formats
 *          - png or jpeg-encoded individual frames.
 *            Note that most MJPG (motion-JPEG) formats are not read correctly.
 *      - Does not read avi formats with more than one frame per chunk
 *      - Palette changes during the video not supported
 *      - Out-of-sequence frames (sequence given by index) not supported
 *      - Different frame sizes in one file (rcFrame) not supported
 *      - Conversion of (A)YUV formats to grayscale is non-standard:
 *        All 255 levels are kept as in the input (i.e. the full dynamic
 *        range of data from a frame grabber is preserved).
 *        For standard behavior, use "Brightness&Contrast", Press "Set",
 *        enter "Min." 16, "Max." 235, and press "Apply".
 * 
 *      - Note: As a last frame, one can enter '0' (= last frame),
 *        '-1' (last frame -1), etc.
 * 
 * Version History:
 *   2008-04-29
 *        based on a plugin by Daniel Marsh and Wayne Rasband;
 *        modifications by Michael Schmid
 *      - Support for several other formats added, especially some YUV
 *        (also named YCbCr) formats
 *      - Uneven chunk sizes fixed
 *      - Negative biHeight fixed
 *      - Audio or second video stream don't cause a problem
 *      - Can read part of a file (specify start & end frame numbers)
 *      - Can convert YUV and RGB to grayscale (does not convert 8-bit with palette)
 *      - Can flip vertically
 *      - Can create a virtual stack
 *      - Added slice label: time of the frame in the movie
 *      - Added a public method 'getStack' that does not create an image window
 *      - More compact code, especially for reading the header (rewritten)
 *      - In the code, bitmapinfo items have their canonical names.
 *   2008-06-08
 *      - Support for png and jpeg/mjpg encoded files added
 *      - Retrieves animation speed from image frame rate
 *      - Exception handling without multiple error messages
 *   2008-07-03
 *      - Support for 16bit AVIs coded by MIL (Matrox Imaging Library)
 *   2009-03-06
 *      - Jesper Soendergaard Pedersen added support for extended (large) AVI files,
 *        also known as 'AVI 2.0' or 'OpenDML 1.02 AVI file format extension'
 *        For Virtual stacks, it reads the 'AVI 2.0' index (indx and ix00 tags).
 *        This results in a dramatic speed increase in loading of virtual stacks.
 *        If indx and ix00 are not found or bIndexType is unsupported, as well as for
 *        non-virtual stacks it finds the frames 'the old way', by scanning the whole file.
 *      - Fixes a bug where it read too many frames.
 *        This version was published as external plugin.
 *   2011-12-03
 *      - Minor updates & cleanup for integration into ImageJ again.
 *      - Multithread-compliant.
 *   2011-12-10
 *      - Based on a plugin by Jesper Soendergaard Pedersen, also reads the 'idx1' index of
 *        AVI 1 files, speeding up initial reading of virtual stacks also for smaller files.
 *      - When the first frame to read is > 1, uses the index to quickly skip the initial frames.
 *      - Creates a unique window name.
 *      - Opens MJPG files also if they do not contain Huffman tables
 * 
 * The AVI format looks like this:
 * RIFF AVI                 RIFF HEADER, AVI CHUNK                  
 *   | LIST hdrl            MAIN AVI HEADER
 *   | | avih               AVI HEADER
 *   | | LIST strl          STREAM LIST(s) (One per stream)
 *   | | | strh             STREAM HEADER (Required after above; fourcc type is 'vids' for video stream)
 *   | | | strf             STREAM FORMAT (for video: BitMapInfo; may also contain palette)
 *   | | | strd             OPTIONAL -- STREAM DATA (ignored in this plugin)
 *   | | | strn             OPTIONAL -- STREAM NAME (ignored in this plugin)
 *   | | | indx             OPTIONAL -- MAIN 'AVI 2.0' INDEX
 *   | LIST movi            MOVIE DATA
 *   | | ix00               partial video index of 'AVI 2.0', usually missing in AVI 1 (ix01 would be for audio)
 *   | | [rec]              RECORD DATA (one record per frame for interleaved video; optional, unsupported in this plugin)
 *   | | |-dataSubchunks    RAW DATA: '??wb' for audio, '??db' and '??dc' for uncompressed and
 *   | |                    compressed video, respectively. "??" denotes stream number, usually "00" or "01"
 *   | idx1                 AVI 1 INDEX ('old-style'); may be missing in very old formats
 * RIFF AVIX                'AVI 2.0' only: further chunks
 *   | LIST movi            more movie data, as above, usually with ix00 index
 *                          Any number of further chunks (RIFF tags) may follow
 * 
 * Items ('chunks') with one fourcc (four-character code such as 'strh') start with two 4-byte words:
 * the fourcc and the size of the data area.
 * Items with two fourcc (e.g. 'LIST hdrl') have three 4-byte words: the first fourcc, the size and the
 * second fourcc. Note that the size includes the 4 bytes needed for the second fourcc.
 * 
 * Chunks with fourcc 'JUNK' can appear anywhere and should be ignored.
 * 
 * </pre>
 */

public class AVI_Reader extends VirtualStack implements PlugIn {
	
	// four-character codes for avi chunk types
	// NOTE: byte sequence is reversed - ints in Intel (little endian) byte
	// order!
	private final static int FOURCC_RIFF = 0x46464952; // 'RIFF'
	private final static int FOURCC_AVI = 0x20495641; // 'AVI
	// '
	private final static int FOURCC_AVIX = 0x58495641; // 'AVIX'
	// //
	// extended
	// AVI
	private final static int FOURCC_ix00 = 0x30307869; // 'ix00'
	// //
	// index
	// within
	private final static int FOURCC_indx = 0x78646e69; // 'indx'
	// //
	// main
	// index
	private final static int FOURCC_idx1 = 0x31786469; // 'idx1'
	// //
	// index
	// of
	// single
	// 'movi'
	// block
	private final static int FOURCC_LIST = 0x5453494c; // 'LIST'
	private final static int FOURCC_hdrl = 0x6c726468; // 'hdrl'
	private final static int FOURCC_avih = 0x68697661; // 'avih'
	private final static int FOURCC_strl = 0x6c727473; // 'strl'
	private final static int FOURCC_strh = 0x68727473; // 'strh'
	private final static int FOURCC_strf = 0x66727473; // 'strf'
	private final static int FOURCC_movi = 0x69766f6d; // 'movi'
	private final static int FOURCC_rec = 0x20636572; // 'rec
	// '
	private final static int FOURCC_JUNK = 0x4b4e554a; // 'JUNK'
	private final static int FOURCC_vids = 0x73646976; // 'vids'
	private final static int FOURCC_00db = 0x62643030; // '00db'
	private final static int FOURCC_00dc = 0x63643030; // '00dc'
	
	// four-character codes for supported compression formats; see fourcc.org
	private final static int NO_COMPRESSION = 0; // uncompressed,
	// also
	// 'RGB
	// ',
	// 'RAW
	// '
	private final static int NO_COMPRESSION_RGB = 0x20424752; // 'RGB
	// ' -a
	// name
	// for
	// uncompressed
	private final static int NO_COMPRESSION_RAW = 0x20574152; // 'RAW
	// ' -a
	// name
	// for
	// uncompressed
	private final static int NO_COMPRESSION_Y800 = 0x30303859; // 'Y800'
	// -a
	// name
	// for
	// 8-bit
	// grayscale
	private final static int NO_COMPRESSION_Y8 = 0x20203859; // 'Y8 '
	// -another
	// name
	// for
	// Y800
	private final static int NO_COMPRESSION_GREY = 0x59455247; // 'GREY'
	// -another
	// name
	// for
	// Y800
	private final static int NO_COMPRESSION_Y16 = 0x20363159; // 'Y16
	// ' -a
	// name
	// for
	// 16-bit
	// uncompressed
	// grayscale
	private final static int NO_COMPRESSION_MIL = 0x204c494d; // 'MIL
	// ' -
	// Matrox
	// Imaging
	// Library
	private final static int AYUV_COMPRESSION = 0x56555941; // 'AYUV'
	// -uncompressed,
	// but
	// alpha,
	// Y, U,
	// V
	// bytes
	private final static int UYVY_COMPRESSION = 0x59565955; // 'UYVY'
	// -
	// 4:2:2
	// with
	// byte
	// order
	// u y0
	// v y1
	private final static int Y422_COMPRESSION = 0x564E5955; // 'Y422'
	// -another
	// name
	// for
	// of
	// UYVY
	private final static int UYNV_COMPRESSION = 0x32323459; // 'UYNV'
	// -another
	// name
	// for
	// of
	// UYVY
	private final static int CYUV_COMPRESSION = 0x76757963; // 'cyuv'
	// -as
	// UYVY
	// but
	// not
	// top-down
	private final static int V422_COMPRESSION = 0x32323456; // 'V422'
	// -as
	// UYVY
	// but
	// not
	// top-down
	private final static int YUY2_COMPRESSION = 0x32595559; // 'YUY2'
	// -
	// 4:2:2
	// with
	// byte
	// order
	// y0 u
	// y1 v
	private final static int YUNV_COMPRESSION = 0x564E5559; // 'YUNV'
	// -another
	// name
	// for
	// YUY2
	private final static int YUYV_COMPRESSION = 0x56595559; // 'YUYV'
	// -another
	// name
	// for
	// YUY2
	private final static int YVYU_COMPRESSION = 0x55595659; // 'YVYU'
	// -
	// 4:2:2
	// with
	// byte
	// order
	// y0 u
	// y1 v
	private final static int JPEG_COMPRESSION = 0x6765706a; // 'jpeg'
	// JPEG
	// compression
	// of
	// individual
	// frames
	private final static int JPEG_COMPRESSION2 = 0x4745504a; // 'JPEG'
	// JPEG
	// compression
	// of
	// individual
	// frames
	private final static int JPEG_COMPRESSION3 = 0x04; // BI_JPEG:
	// JPEG
	// compression
	// of
	// individual
	// frames
	private final static int MJPG_COMPRESSION = 0x47504a4d; // 'MJPG'
	// Motion
	// JPEG,
	// also
	// reads
	// compression
	// of
	// individual
	// frames
	private final static int PNG_COMPRESSION = 0x20676e70; // 'png
	// ' PNG
	// compression
	// of
	// individual
	// frames
	private final static int PNG_COMPRESSION2 = 0x20474e50; // 'PNG
	// ' PNG
	// compression
	// of
	// individual
	// frames
	private final static int PNG_COMPRESSION3 = 0x05; // BI_PNG
	// PNG
	// compression
	// of
	// individual
	// frames
	
	private final static int BITMASK24 = 0x10000; // for
	// 24-bit
	// (in
	// contrast
	// to 8,
	// 16,...
	// not a
	// bitmask)
	private final static long SIZE_MASK = 0xffffffffL; // for
	// conversion
	// of
	// sizes
	// from
	// unsigned
	// int
	// to
	// long
	
	// flags from AVI chunk header
	private final static int AVIF_HASINDEX = 0x00000010; // Index
	// at
	// end
	// of
	// file?
	private final static int AVIF_MUSTUSEINDEX = 0x00000020; // ignored
	// by
	// this
	// plugin
	private final static int AVIF_ISINTERLEAVED = 0x00000100; // ignored
	// by
	// this
	// plugin
	
	// constants used to read 'AVI 2' index chunks (others than those defined
	// here are not supported)
	private final static byte AVI_INDEX_OF_CHUNKS = 0x01; // index
	// of
	// frames
	private final static byte AVI_INDEX_OF_INDEXES = 0x00; // main
	// indx
	// pointing
	// to
	// ix00
	// etc
	// subindices
	
	// static versions of dialog parameters that will be remembered
	private static boolean staticConvertToGray;
	private static boolean staticFlipVertical;
	private static boolean staticIsVirtual;
	// dialog parameters
	private int firstFrame = 1; // the
	// first
	// frame
	// to
	// read
	private int lastFrame = 0; // the
	// last
	// frame
	// to
	// read;
	// 0
	// means
	// 'read
	// all'
	private boolean convertToGray; // whether
	// to
	// convert
	// color
	// video
	// to
	// grayscale
	private boolean flipVertical; // whether
	// to
	// flip
	// image
	// vertical
	private boolean isVirtual; // whether
	// to
	// open
	// as
	// virtual
	// stack
	// the input file
	private RandomAccessFile raFile;
	private String raFilePath;
	private boolean headerOK = false; // whether
	// header
	// has
	// been
	// read
	// more avi file properties etc
	private int streamNumber; // number
	// of
	// the
	// (first)
	// video
	// stream
	private int type0xdb, type0xdc; // video
	// stream
	// chunks
	// must
	// have
	// one
	// of
	// these
	// two
	// types
	// (e.g.
	// '00db'
	// for
	// straem
	// 0)
	private long fileSize; // file
	// size
	private long aviSize; // size
	// of
	// 'AVI'
	// chunk
	private long headerPositionEnd; // 'movi'
	// will
	// start
	// somewhere
	// here
	private long indexPosition; // position
	// of
	// the
	// main
	// index
	// (indx)
	private long indexPositionEnd; // indx
	// seek
	// end
	private long moviPosition; // position
	// of
	// 'movi'
	// list
	private int paddingGranularity = 2; // tags
	// start
	// at
	// even
	// address
	private int frameNumber = 1; // frame
	// currently
	// read;
	// global
	// because
	// distributed
	// over
	// 1st
	// AVi
	// and
	// further
	// RIFF
	// AVIX
	// chunks
	private int lastFrameToRead = Integer.MAX_VALUE;
	private int totalFramesFromIndex; // number
	// of
	// frames
	// from
	// 'AVI
	// 2.0'
	// indices
	private boolean indexForCountingOnly; // don't
	// read
	// the
	// index,
	// only
	// count
	// int
	// totalFramesFromIndex
	// how
	// many
	// entries
	// derived from BitMapInfo
	private int dataCompression; // data
	// compression
	// type
	// used
	private int scanLineSize;
	private boolean dataTopDown; // whether
	// data
	// start
	// at
	// top
	// of
	// image
	private ColorModel cm;
	private boolean variableLength; // compressed
	// (PNG,
	// JPEG)
	// frames
	// have
	// variable
	// length
	// for conversion to ImageJ stack
	private Vector frameInfos; // for
	// virtual
	// stack:
	// long[]
	// with
	// frame
	// pos&size
	// in
	// file,
	// time(usec)
	private ImageStack stack;
	private ImagePlus imp;
	// for debug messages and error handling
	private boolean verbose = IJ.debugMode;
	private long startTime;
	private boolean aborting;
	
	// From AVI Header Chunk
	private int dwMicroSecPerFrame;
	private int dwMaxBytesPerSec;
	private int dwReserved1;
	private int dwFlags;
	private int dwTotalFrames; // AVI
	// 2.0:
	// will
	// be
	// replaced
	// by
	// number
	// of
	// frames
	// from
	// index
	private int dwInitialFrames;
	private int dwStreams;
	private int dwSuggestedBufferSize;
	private int dwWidth;
	private int dwHeight;
	
	// From Stream Header Chunk
	private int fccStreamHandler;
	private int dwStreamFlags;
	private int dwPriorityLanguage; // actually
	// 2
	// 16-bit
	// words:
	// wPriority
	// and
	// wLanguage
	private int dwStreamInitialFrames;
	private int dwStreamScale;
	private int dwStreamRate;
	private int dwStreamStart;
	private int dwStreamLength;
	private int dwStreamSuggestedBufferSize;
	private int dwStreamQuality;
	private int dwStreamSampleSize;
	
	// From Stream Format Chunk: BITMAPINFO contents (40 bytes)
	private int biSize; // size
	// of
	// this
	// header
	// in
	// bytes
	// (40)
	private int biWidth;
	private int biHeight;
	private short biPlanes; // no.
	// of
	// color
	// planes:
	// for
	// the
	// formats
	// decoded;
	// here
	// always
	// 1
	private short biBitCount; // Bits
	// per
	// Pixel
	private int biCompression;
	private int biSizeImage; // size
	// of
	// image
	// in
	// bytes
	// (may
	// be 0:
	// if
	// so,
	// calculate)
	private int biXPelsPerMeter; // horizontal
	// resolution,
	// pixels/meter
	// (may
	// be 0)
	private int biYPelsPerMeter; // vertical
	// resolution,
	// pixels/meter
	// (may
	// be 0)
	private int biClrUsed; // no.
	// of
	// colors
	// in
	// palette
	// (if
	// 0,
	// calculate)
	private int biClrImportant; // no.
	
	// of
	// important
	// colors
	// (appear
	// first
	// in
	// palette)
	// (0
	// means
	// all
	// are
	// important)
	
	/**
	 * The plugin is invoked by ImageJ using this method. String 'arg' may be used to select the path.
	 */
	@Override
	public void run(String arg)
	{
		OpenDialog od = new OpenDialog("Select AVI File", arg); // file dialog
		String fileName = od.getFileName();
		if(fileName == null)
		{
			return;
		}
		String fileDir = od.getDirectory();
		String path = fileDir + fileName;
		try
		{
			this.openAndReadHeader(path); // open and read header
		}
		catch (Exception e)
		{
			this.error(this.exceptionMessage(e));
			return;
		}
		if(!this.showDialog(fileName))
		{
			return;
		} // ask for parameters
		try
		{
			ImageStack stack = this.makeStack(path, this.firstFrame, this.lastFrame, this.isVirtual, this.convertToGray, this.flipVertical); // read
			// data
		}
		catch (Exception e)
		{
			this.error(this.exceptionMessage(e));
			return;
		}
		if(this.stack == null || this.aborting || (this.stack.isVirtual() && this.stack.getProcessor(1) == null))
		{
			return;
		}
		if(this.stack.getSize() == 0)
		{
			String rangeText = "";
			if(this.firstFrame > 1 || this.lastFrame != 0)
			{
				rangeText = "\nin Range " + this.firstFrame + (this.lastFrame > 0 ? " - " + this.lastFrame : " - end");
			}
			this.error("Error: No Frames Found" + rangeText);
			return;
		}
		this.imp = new ImagePlus(WindowManager.getUniqueName(fileName), this.stack);
		if(this.imp.getBitDepth() == 16)
		{
			this.imp.getProcessor().resetMinAndMax();
		}
		this.setFramesPerSecond(this.imp);
		FileInfo fi = new FileInfo();
		fi.fileName = fileName;
		fi.directory = fileDir;
		this.imp.setFileInfo(fi);
		if(arg.equals(""))
		{
			this.imp.show();
		}
		IJ.showTime(this.imp, this.startTime, "Read AVI in ", this.stack.getSize());
	}
	
	/**
	 * The plugin is invoked by ImageJ using this method. String 'arg' may be used to select the path.
	 */
	public ImageStack runFile(String arg)
	{
		try
		{
			this.stack = this.makeStack(arg, 1, 10, false, true, false); // read
			// data
		}
		catch (Exception e)
		{
			this.error(this.exceptionMessage(e));
			return null;
		}
		if(this.stack == null || this.aborting || (this.stack.isVirtual() && this.stack.getProcessor(1) == null))
		{
			return null;
		}
		if(this.stack.getSize() == 0)
		{
			String rangeText = "";
			if(this.firstFrame > 1 || this.lastFrame != 0)
			{
				rangeText = "\nin Range " + this.firstFrame + (this.lastFrame > 0 ? " - " + this.lastFrame : " - end");
			}
			this.error("Error: No Frames Found" + rangeText);
			return null;
		}
		
		return this.stack;
	}
	
	/** Returns the ImagePlus opened by run(). */
	public ImagePlus getImagePlus()
	{
		return this.imp;
	}
	
	/**
	 * Create an ImageStack from an avi file with given path.
	 * 
	 * @param path
	 *            Directoy+filename of the avi file
	 * @param firstFrame
	 *            Number of first frame to read (first frame of the file is 1)
	 * @param lastFrame
	 *            Number of last frame to read or 0 for reading all, -1 for all but last...
	 * @param isVirtual
	 *            Whether to return a virtual stack
	 * @param convertToGray
	 *            Whether to convert color images to grayscale
	 * @return Returns the stack; null on failure. The stack returned may be non-null, but have a length of zero if no suitable frames were found
	 */
	public ImageStack makeStack(String path, int firstFrame, int lastFrame, boolean isVirtual, boolean convertToGray, boolean flipVertical)
	{
		this.firstFrame = firstFrame;
		this.lastFrame = lastFrame;
		this.isVirtual = isVirtual;
		this.convertToGray = convertToGray;
		this.flipVertical = flipVertical;
		String exceptionMessage = null;
		IJ.showProgress(.001);
		try
		{
			this.readAVI(path);
		}
		catch (OutOfMemoryError e)
		{
			this.stack.trim();
			IJ.showMessage("AVI Reader", "Out of memory.  " + this.stack.getSize() + " of " + this.dwTotalFrames + " frames will be opened.");
		}
		catch (Exception e)
		{
			exceptionMessage = this.exceptionMessage(e);
		}
		finally
		{
			try
			{
				this.raFile.close();
				if(this.verbose)
				{
					IJ.log("File closed.");
				}
			}
			catch (Exception e)
			{}
			IJ.showProgress(1.0);
		}
		if(exceptionMessage != null)
		{
			this.error(exceptionMessage);
			return null;
		}
		if(isVirtual && this.frameInfos != null)
		{
			this.stack = this;
		}
		if(this.stack != null && this.cm != null)
		{
			this.stack.setColorModel(this.cm);
		}
		return this.stack;
	}
	
	/**
	 * Returns an ImageProcessor for the specified slice of this virtual stack (if it is one) where 1<=n<=nslices. Returns null if no virtual stack or no slices.
	 */
	@Override
	public synchronized ImageProcessor getProcessor(int n)
	{
		if(this.frameInfos == null || this.frameInfos.size() == 0 || this.raFilePath == null)
		{
			return null;
		}
		if(n < 1 || n > this.frameInfos.size())
		{
			throw new IllegalArgumentException("Argument out of range: " + n);
		}
		Object pixels = null;
		RandomAccessFile rFile = null;
		String exceptionMessage = null;
		try
		{
			rFile = new RandomAccessFile(new File(this.raFilePath), "r");
			long[] frameInfo = (long[]) (this.frameInfos.get(n - 1));
			pixels = this.readFrame(rFile, frameInfo[0], (int) frameInfo[1]);
		}
		catch (Exception e)
		{
			exceptionMessage = this.exceptionMessage(e);
		}
		finally
		{
			try
			{
				rFile.close();
			}
			catch (Exception e)
			{}
		}
		if(exceptionMessage != null)
		{
			this.error(exceptionMessage);
			return null;
		}
		if(pixels == null)
		{
			return null;
		} // failed
		if(pixels instanceof byte[])
		{
			return new ByteProcessor(this.dwWidth, this.biHeight, (byte[]) pixels, this.cm);
		}
		else if(pixels instanceof short[])
		{
			return new ShortProcessor(this.dwWidth, this.biHeight, (short[]) pixels, this.cm);
		}
		else
		{
			return new ColorProcessor(this.dwWidth, this.biHeight, (int[]) pixels);
		}
	}
	
	/** Returns the image width of the virtual stack */
	@Override
	public int getWidth()
	{
		return this.dwWidth;
	}
	
	/** Returns the image height of the virtual stack */
	@Override
	public int getHeight()
	{
		return this.biHeight;
	}
	
	/** Returns the number of images in this virtual stack (if it is one) */
	@Override
	public int getSize()
	{
		if(this.frameInfos == null)
		{
			return 0;
		}
		else
		{
			return this.frameInfos.size();
		}
	}
	
	/**
	 * Returns the label of the specified slice in this virtual stack (if it is one).
	 */
	@Override
	public String getSliceLabel(int n)
	{
		if(this.frameInfos == null || n < 1 || n > this.frameInfos.size())
		{
			throw new IllegalArgumentException("No Virtual Stack or argument out of range: " + n);
		}
		return this.frameLabel(((long[]) (this.frameInfos.get(n - 1)))[2]);
	}
	
	/**
	 * Deletes the specified image from this virtual stack (if it is one), where 1<=n<=nslices.
	 */
	@Override
	public void deleteSlice(int n)
	{
		if(this.frameInfos == null || this.frameInfos.size() == 0)
		{
			return;
		}
		if(n < 1 || n > this.frameInfos.size())
		{
			throw new IllegalArgumentException("Argument out of range: " + n);
		}
		this.frameInfos.removeElementAt(n - 1);
	}
	
	/** Parameters dialog, returns false on cancel */
	private boolean showDialog(String fileName)
	{
		if(this.lastFrame != -1)
		{
			this.lastFrame = this.dwTotalFrames;
		}
		if(!IJ.isMacro())
		{
			this.convertToGray = staticConvertToGray;
			this.flipVertical = staticFlipVertical;
			this.isVirtual = staticIsVirtual;
		}
		GenericDialog gd = new GenericDialog("AVI Reader");
		gd.addNumericField("First Frame: ", this.firstFrame, 0);
		gd.addNumericField("Last Frame: ", this.lastFrame, 0, 6, "");
		gd.addCheckbox("Use Virtual Stack", this.isVirtual);
		gd.addCheckbox("Convert to Grayscale", this.convertToGray);
		gd.addCheckbox("Flip Vertical", this.flipVertical);
		gd.showDialog();
		if(gd.wasCanceled())
		{
			return false;
		}
		this.firstFrame = (int) gd.getNextNumber();
		this.lastFrame = (int) gd.getNextNumber();
		this.isVirtual = gd.getNextBoolean();
		this.convertToGray = gd.getNextBoolean();
		this.flipVertical = gd.getNextBoolean();
		if(!IJ.isMacro())
		{
			staticConvertToGray = this.convertToGray;
			staticFlipVertical = this.flipVertical;
			staticIsVirtual = this.isVirtual;
		}
		IJ.register(this.getClass());
		return true;
	}
	
	/** Read into a (virtual) stack */
	private void readAVI(String path) throws Exception, IOException
	{
		if(!this.headerOK)
		{
			this.openAndReadHeader(path);
		}
		this.startTime += System.currentTimeMillis();// taking previously
		// elapsed
		// time into account
		/** MOVED UP HERE BY JSP */
		if(this.lastFrame > 0)
		{
			this.lastFrameToRead = this.lastFrame;
		}
		if(this.lastFrame < 0 && this.dwTotalFrames > 0)
		{
			this.lastFrameToRead = this.dwTotalFrames + this.lastFrame;
		}
		if(this.lastFrameToRead < this.firstFrame)
		{
			return;
		}
		boolean hasIndex = (this.dwFlags & AVIF_HASINDEX) != 0;
		if(this.isVirtual || this.firstFrame > 1)
		{ // avoid scanning frame-by-frame where we only need the positions
			this.frameInfos = new Vector(100); // holds frame positions, sizes
			// and
			// time since start
			long nextPosition = -1;
			if(this.indexPosition > 0)
			{ // attempt to get AVI2.0 index instead of scanning for all frames
				this.raFile.seek(this.indexPosition);
				nextPosition = this.findFourccAndRead(FOURCC_indx, false, this.indexPositionEnd, false);
			}
			if(hasIndex && this.frameInfos.size() == 0)
			{ // got nothing from indx, attempt to read AVI 1 index 'idx1'
				this.raFile.seek(this.headerPositionEnd);
				this.moviPosition = this.findFourccAndSkip(FOURCC_movi, true, this.fileSize); // go
				// behind
				// the
				// 'movi'
				// list
				if(this.moviPosition > 0)
				{
					nextPosition = this.findFourccAndRead(FOURCC_idx1, false, this.fileSize, false);
				}
			}
			if(this.verbose)
			{
				IJ.log("'frameInfos' has " + this.frameInfos.size() + " entries");
			}
		}
		if(this.isVirtual && this.frameInfos.size() > 0)
		{
			return;
		}
		// Read AVI (movie data) frame by frame - if no index tag is present the
		// pointers
		// for the virtual AVI stack will be read here
		this.raFile.seek(this.headerPositionEnd);
		if(this.firstFrame > 1 && this.frameInfos.size() > 0)
		{
			long[] frameInfo = (long[]) this.frameInfos.get(0);
			this.raFile.seek(frameInfo[0] - 8); // chunk starts 8 bytes before
			// frame
			// data
			this.frameNumber = this.firstFrame;
			if(this.verbose)
			{
				IJ.log("directly go to frame " + this.firstFrame + " @ 0x" + Long.toHexString(frameInfo[0] - 8));
			}
			this.readMovieData(this.fileSize);
		}
		else
		{
			this.frameNumber = 1;
			this.findFourccAndRead(FOURCC_movi, true, this.fileSize, true);
		}
		
		long pos = this.raFile.getFilePointer();
		// IJ.log("at 0x"+Long.toHexString(pos)+" filesize=0x"+Long.toHexString(fileSize));
		// extended AVI: try to find further 'RIFF' chunks, where we expect AVIX
		// tags
		while (pos > 0 && pos < this.fileSize && (this.frameNumber < this.lastFrameToRead + 1))
		{
			pos = this.findFourccAndRead(FOURCC_RIFF, false, this.fileSize, false);
		}
		return;
	}
	
	/** Open the file with given path and read its header */
	private void openAndReadHeader(String path) throws Exception, IOException
	{
		this.startTime = System.currentTimeMillis();
		if(this.verbose)
		{
			IJ.log("OPEN AND READ AVI FILE HEADER " + this.timeString());
		}
		File file = new File(path); // o p e n
		this.raFile = new RandomAccessFile(file, "r");
		this.raFilePath = path;
		this.fileSize = this.raFile.length();
		int fileType = this.readInt(); // f i l e h e a d e r
		if(this.verbose)
		{
			IJ.log("File header: File type='" + this.fourccString(fileType) + "' (should be 'RIFF')" + this.timeString());
		}
		if(fileType != FOURCC_RIFF)
		{
			throw new Exception("Not an AVI file.");
		}
		this.aviSize = this.readInt() & SIZE_MASK; // size of AVI chunk
		int riffType = this.readInt();
		if(this.verbose)
		{
			IJ.log("File header: RIFF type='" + this.fourccString(riffType) + "' (should be 'AVI ')");
		}
		if(riffType != FOURCC_AVI)
		{
			throw new Exception("Not an AVI file.");
		}
		this.findFourccAndRead(FOURCC_hdrl, true, this.fileSize, true);
		this.startTime -= System.currentTimeMillis(); // becomes minus elapsed
		// Time
		this.headerOK = true;
	}
	
	/**
	 * Read AVIX for large files (sequential reading frame-by-frame beyond the first Chunk)
	 **/
	private void readAVIX(long endPosition) throws Exception, IOException
	{
		if(this.verbose)
		{
			IJ.log("Trying to read AVIX" + this.timeString());
		}
		int riffType = this.readInt();
		if(this.verbose)
		{
			IJ.log("File header: RIFF type='" + this.fourccString(riffType) + "' (should be 'AVIX')");
		}
		if(riffType != FOURCC_AVIX)
		{
			throw new Exception("Not an AVI file.");
		}
		this.findFourccAndRead(FOURCC_movi, true, this.fileSize, true); // read
		// movie
		// data
	}
	
	/**
	 * Find the next position of fourcc or LIST fourcc and read contents. Returns next position If not found, throws exception or returns -1
	 */
	private long findFourccAndRead(int fourcc, boolean isList, long endPosition, boolean throwNotFoundException) throws Exception, IOException
	{
		long nextPos;
		boolean contentOk = false;
		do
		{
			int type = this.readType(endPosition);
			if(type == 0)
			{ // reached endPosition without finding
				if(throwNotFoundException)
				{
					throw new Exception("Required item '" + this.fourccString(fourcc) + "' not found");
				}
				else
				{
					return -1;
				}
			}
			long size = this.readInt() & SIZE_MASK;
			nextPos = this.raFile.getFilePointer() + size;
			if(isList && type == FOURCC_LIST)
			{
				type = this.readInt();
			}
			if(this.verbose)
			{
				IJ.log("Searching for '" + this.fourccString(fourcc) + "', found " + this.fourccString(type) + "' " + this.posSizeString(nextPos - size, size));
			}
			if(type == fourcc)
			{
				contentOk = this.readContents(fourcc, nextPos);
			}
			else if(this.verbose)
			{
				IJ.log("Discarded '" + this.fourccString(type) + "': Contents does not fit");
			}
			this.raFile.seek(nextPos);
			if(contentOk)
			{
				return nextPos;
			} // found and read, breaks the loop
		}
		while (!contentOk);
		return nextPos;
	}
	
	/**
	 * Find the next position of fourcc or LIST fourcc, but does not read it, only returns the first position inside the fourcc chunk and puts the file pointer behind the fourcc chunk (if successful) If not found, returns -1
	 */
	private long findFourccAndSkip(int fourcc, boolean isList, long endPosition) throws IOException
	{
		while (true)
		{
			int type = this.readType(endPosition);
			if(type == 0)
			{
				return -1;
			}
			long size = this.readInt() & SIZE_MASK;
			long chunkPos = this.raFile.getFilePointer();
			long nextPos = chunkPos + size; // note that 'size' of a list
			// includes the 'type' that follows
			// now
			if(isList && type == FOURCC_LIST)
			{
				type = this.readInt();
			}
			if(this.verbose)
			{
				IJ.log("Searching for (to skip) '" + this.fourccString(fourcc) + "', found " + this.fourccString(type) + "' " + this.posSizeString(chunkPos, size));
			}
			this.raFile.seek(nextPos);
			if(type == fourcc)
			{
				return chunkPos;
			} // found and skipped, breaks the loop
		}
	}
	
	/** read contents defined by four-cc code; returns true if contens ok */
	private boolean readContents(int fourcc, long endPosition) throws Exception, IOException
	{
		switch (fourcc)
		{
			case FOURCC_hdrl:
				this.headerPositionEnd = endPosition;
				this.findFourccAndRead(FOURCC_avih, false, endPosition, true);
				this.findFourccAndRead(FOURCC_strl, true, endPosition, true);
				return true;
			case FOURCC_avih:
				this.readAviHeader();
				return true;
			case FOURCC_strl:
				long nextPosition = this.findFourccAndRead(FOURCC_strh, false, endPosition, false);
				if(nextPosition < 0)
				{
					return false;
				}
				this.indexPosition = this.findFourccAndRead(FOURCC_strf, false, endPosition, true);
				this.indexPositionEnd = endPosition;
				this.indexForCountingOnly = true; // try reading indx for
				// counting
				// number of entries
				this.totalFramesFromIndex = 0;
				nextPosition = this.findFourccAndRead(FOURCC_indx, false, endPosition, false);
				if(nextPosition > 0 && this.totalFramesFromIndex > this.dwTotalFrames)
				{
					this.dwTotalFrames = this.totalFramesFromIndex;
				}
				this.indexForCountingOnly = false;
				return true;
			case FOURCC_strh:
				int streamType = this.readInt();
				if(streamType != FOURCC_vids)
				{
					if(this.verbose)
					{
						IJ.log("Non-video Stream '" + this.fourccString(streamType) + " skipped");
					}
					this.streamNumber++;
					return false;
				}
				this.readStreamHeader();
				return true;
			case FOURCC_strf:
				this.readBitMapInfo(endPosition);
				return true;
			case FOURCC_indx:
			case FOURCC_ix00:
				this.readAvi2Index(endPosition);
				return true;
			case FOURCC_idx1:
				this.readOldFrameIndex(endPosition);
				return true;
			case FOURCC_RIFF:
				this.readAVIX(endPosition);
				return true;
			case FOURCC_movi:
				this.readMovieData(endPosition);
				return true;
		}
		throw new Exception("Program error, type " + this.fourccString(fourcc));
	}
	
	void readAviHeader() throws Exception, IOException
	{ // 'avih'
		this.dwMicroSecPerFrame = this.readInt();
		this.dwMaxBytesPerSec = this.readInt();
		this.dwReserved1 = this.readInt(); // in newer avi formats, this is
		// dwPaddingGranularity?
		this.dwFlags = this.readInt();
		this.dwTotalFrames = this.readInt();
		this.dwInitialFrames = this.readInt();
		this.dwStreams = this.readInt();
		this.dwSuggestedBufferSize = this.readInt();
		this.dwWidth = this.readInt();
		this.dwHeight = this.readInt();
		// dwReserved[4] follows, ignored
		
		if(this.verbose)
		{
			IJ.log("AVI HEADER (avih):" + this.timeString());
			IJ.log("   dwMicroSecPerFrame=" + this.dwMicroSecPerFrame);
			IJ.log("   dwMaxBytesPerSec=" + this.dwMaxBytesPerSec);
			IJ.log("   dwReserved1=" + this.dwReserved1);
			IJ.log("   dwFlags=" + this.dwFlags);
			IJ.log("   dwTotalFrames=" + this.dwTotalFrames);
			IJ.log("   dwInitialFrames=" + this.dwInitialFrames);
			IJ.log("   dwStreams=" + this.dwStreams);
			IJ.log("   dwSuggestedBufferSize=" + this.dwSuggestedBufferSize);
			IJ.log("   dwWidth=" + this.dwWidth);
			IJ.log("   dwHeight=" + this.dwHeight);
		}
	}
	
	void readStreamHeader() throws Exception, IOException
	{ // 'strh'
		this.fccStreamHandler = this.readInt();
		this.dwStreamFlags = this.readInt();
		this.dwPriorityLanguage = this.readInt();
		this.dwStreamInitialFrames = this.readInt();
		this.dwStreamScale = this.readInt();
		this.dwStreamRate = this.readInt();
		this.dwStreamStart = this.readInt();
		this.dwStreamLength = this.readInt();
		this.dwStreamSuggestedBufferSize = this.readInt();
		this.dwStreamQuality = this.readInt();
		this.dwStreamSampleSize = this.readInt();
		// rcFrame rectangle follows, ignored
		if(this.verbose)
		{
			IJ.log("VIDEO STREAM HEADER (strh):");
			IJ.log("   fccStreamHandler='" + this.fourccString(this.fccStreamHandler) + "'");
			IJ.log("   dwStreamFlags=" + this.dwStreamFlags);
			IJ.log("   wPriority,wLanguage=" + this.dwPriorityLanguage);
			IJ.log("   dwStreamInitialFrames=" + this.dwStreamInitialFrames);
			IJ.log("   dwStreamScale=" + this.dwStreamScale);
			IJ.log("   dwStreamRate=" + this.dwStreamRate);
			IJ.log("   dwStreamStart=" + this.dwStreamStart);
			IJ.log("   dwStreamLength=" + this.dwStreamLength);
			IJ.log("   dwStreamSuggestedBufferSize=" + this.dwStreamSuggestedBufferSize);
			IJ.log("   dwStreamQuality=" + this.dwStreamQuality);
			IJ.log("   dwStreamSampleSize=" + this.dwStreamSampleSize);
		}
		if(this.dwStreamSampleSize > 1)
		{
			throw new Exception("Video stream with " + this.dwStreamSampleSize + " (more than 1) frames/chunk not supported");
		}
		// what the chunks in that stream will be named (we have two
		// possibilites: uncompressed & compressed)
		this.type0xdb = FOURCC_00db + (this.streamNumber << 8); // '01db' for
		// stream 1,
		// etc. (inverse byte
		// order!)
		this.type0xdc = FOURCC_00dc + (this.streamNumber << 8); // '01dc' for
		// stream 1,
		// etc.
	}
	
	/**
	 * Read 'AVI 2'-type main index 'indx' or an 'ix00' index to frames (only the types AVI_INDEX_OF_INDEXES and AVI_INDEX_OF_CHUNKS are supported)
	 */
	private void readAvi2Index(long endPosition) throws Exception, IOException
	{
		short wLongsPerEntry = this.readShort();
		byte bIndexSubType = this.raFile.readByte();
		byte bIndexType = this.raFile.readByte();
		int nEntriesInUse = this.readInt();
		int dwChunkId = this.readInt();
		long qwBaseOffset = this.readLong();
		this.readInt(); // dwReserved3
		if(this.verbose)
		{
			String bIndexString = bIndexType == AVI_INDEX_OF_CHUNKS ? ": AVI_INDEX_OF_CHUNKS" : bIndexType == AVI_INDEX_OF_INDEXES ? ": AVI_INDEX_OF_INDEXES" : ": UNSUPPOERTED";
			IJ.log("AVI 2 INDEX:");
			IJ.log("   wLongsPerEntry=" + wLongsPerEntry);
			IJ.log("   bIndexSubType=" + bIndexSubType);
			IJ.log("   bIndexType=" + bIndexType + bIndexString);
			IJ.log("   nEntriesInUse=" + nEntriesInUse);
			IJ.log("   dwChunkId='" + this.fourccString(dwChunkId) + "'");
			if(bIndexType == AVI_INDEX_OF_CHUNKS)
			{
				IJ.log("   qwBaseOffset=" + "0x" + Long.toHexString(qwBaseOffset));
			}
		}
		if(bIndexType == AVI_INDEX_OF_INDEXES)
		{ // 'indx' points to other indices
			if(wLongsPerEntry != 4)
			{
				return;
			} // badly formed index, ignore it
			for (int i = 0; i < nEntriesInUse; i++)
			{
				long qwOffset = this.readLong();
				int dwSize = this.readInt();
				int dwDuration = this.readInt(); // ignored; not trustworthy
				// anyhow
				if(this.verbose)
				{
					// IJ.log("qwOffset:"+qwOffset);
					IJ.log("   index data '" + this.fourccString(dwChunkId) + "' " + this.posSizeString(qwOffset, dwSize) + this.timeString());
				}
				long temp = this.raFile.getFilePointer();
				this.raFile.seek(qwOffset);
				this.findFourccAndRead(FOURCC_ix00, false, qwOffset + dwSize, true);
				this.raFile.seek(temp);
				if(this.frameNumber > this.lastFrameToRead)
				{
					break;
				}
			}
		}
		else if(bIndexType == AVI_INDEX_OF_CHUNKS)
		{
			if(wLongsPerEntry != 2)
			{
				return;
			} // badly formed index, ignore it
			if(dwChunkId != this.type0xdb && dwChunkId != this.type0xdc)
			{ // not the stream we search for? (should not happen)
				if(this.verbose)
				{
					IJ.log("INDEX ERROR: SKIPPED ix00, wrong stream number or type, should be " + this.fourccString(this.type0xdb) + " or " + this.fourccString(this.type0xdc));
				}
				return;
			}
			if(this.indexForCountingOnly)
			{ // only count number of entries, don't put into table
				this.totalFramesFromIndex += nEntriesInUse;
				return;
			}
			for (int i = 0; i < nEntriesInUse; i++)
			{
				long dwOffset = this.readInt() & 0xffffffffL;
				long pos = qwBaseOffset + dwOffset;
				int dwSize = this.readInt();
				if(this.isVirtual)
				{
					IJ.showProgress((double) this.frameNumber / this.lastFrameToRead);
				}
				if(this.frameNumber >= this.firstFrame)
				{
					this.frameInfos.add(new long[] { pos, dwSize, (long) this.frameNumber * this.dwMicroSecPerFrame });
					// if (verbose)
					// IJ.log("movie data "+frameNumber+" '"+fourccString(dwChunkId)+"' "+posSizeString(pos,dwSize)+timeString());
				}
				this.frameNumber++;
				if(this.frameNumber > this.lastFrameToRead)
				{
					break;
				}
			}
			if(this.verbose)
			{
				IJ.log("Index read up to frame " + (this.frameNumber - 1));
			}
		}
	}
	
	private void readOldFrameIndex(long endPosition) throws Exception, IOException
	{
		int offset = -1; // difference between absolute frame address and
		// address given in idx1
		int[] offsetsToTry = new int[] { 0, (int) this.moviPosition }; // dwOffset
		// may be
		// w.r.t.
		// file
		// start
		// or
		// w.r.t.
		// 'movi'
		// list.
		while (true)
		{
			if((this.raFile.getFilePointer() + 16) > endPosition)
			{
				break;
			}
			
			int dwChunkId = this.readInt();
			int dwFlags = this.readInt();
			int dwOffset = this.readInt();
			int dwSize = this.readInt();
			// IJ.log("idx1: dwOffset=0x"+Long.toHexString(dwOffset));
			// IJ.log("moviPosition=0x"+Long.toHexString(moviPosition));
			if((dwChunkId == this.type0xdb || dwChunkId == this.type0xdc) && dwSize > 0)
			{
				if(offset < 0)
				{ // find out what the offset refers to
					long temp = this.raFile.getFilePointer();
					for (int i = 0; i < offsetsToTry.length; i++)
					{
						long pos = (dwOffset + offsetsToTry[i]) & SIZE_MASK;
						if(pos < this.moviPosition)
						{
							continue;
						} // frame must be in 'movi' list
						this.raFile.seek(pos);
						int chunkIdAtPos = this.readInt(); // see whether this
						// offset
						// points to the
						// desired
						// chunk
						// IJ.log("read@=0x"+Long.toHexString(pos)+":  '"+fourccString(chunkIdAtPos)+"'");
						if(chunkIdAtPos == dwChunkId)
						{
							offset = offsetsToTry[i];
							break;
						}
					}
					if(this.verbose)
					{
						IJ.log("idx1: dwOffsets are w.r.t. 0x" + (offset < 0 ? " UNKONWN??" : Long.toHexString(offset)));
					}
					this.raFile.seek(temp);
					if(offset < 0)
					{
						return;
					} // neither offset works
				}
				if(this.frameNumber >= this.firstFrame)
				{
					long framepos = (dwOffset + offset) & SIZE_MASK;
					this.frameInfos.add(new long[] { framepos + 8, dwSize, (long) this.frameNumber * this.dwMicroSecPerFrame });
					// if (verbose)
					// IJ.log("idx1 movie data '"+fourccString(dwChunkId)+"' "+posSizeString(framepos,dwSize)+timeString());
				}
				this.frameNumber++;
				if(this.frameNumber > this.lastFrameToRead)
				{
					break;
				}
			} // if(dwChunkId...)
		} // while(true)
		if(this.verbose)
		{
			IJ.log("Index read up to frame " + (this.frameNumber - 1));
		}
		
	}
	
	/**
	 * Read stream format chunk: starts with BitMapInfo, may contain palette
	 */
	void readBitMapInfo(long endPosition) throws Exception, IOException
	{
		this.biSize = this.readInt();
		this.biWidth = this.readInt();
		this.biHeight = this.readInt();
		this.biPlanes = this.readShort();
		this.biBitCount = this.readShort();
		this.biCompression = this.readInt();
		this.biSizeImage = this.readInt();
		this.biXPelsPerMeter = this.readInt();
		this.biYPelsPerMeter = this.readInt();
		this.biClrUsed = this.readInt();
		this.biClrImportant = this.readInt();
		if(this.verbose)
		{
			IJ.log("   biSize=" + this.biSize);
			IJ.log("   biWidth=" + this.biWidth);
			IJ.log("   biHeight=" + this.biHeight);
			IJ.log("   biPlanes=" + this.biPlanes);
			IJ.log("   biBitCount=" + this.biBitCount);
			IJ.log("   biCompression=0x" + Integer.toHexString(this.biCompression) + " '" + this.fourccString(this.biCompression) + "'");
			IJ.log("   biSizeImage=" + this.biSizeImage);
			IJ.log("   biXPelsPerMeter=" + this.biXPelsPerMeter);
			IJ.log("   biYPelsPerMeter=" + this.biYPelsPerMeter);
			IJ.log("   biClrUsed=" + this.biClrUsed);
			IJ.log("   biClrImportant=" + this.biClrImportant);
		}
		
		int allowedBitCount = 0;
		boolean readPalette = false;
		switch (this.biCompression)
		{
			case NO_COMPRESSION:
			case NO_COMPRESSION_RGB:
			case NO_COMPRESSION_RAW:
				this.dataCompression = NO_COMPRESSION;
				this.dataTopDown = this.biHeight < 0; // RGB mode is usually
				// bottom-up,
				// negative height signals top-down
				allowedBitCount = 8 | BITMASK24 | 32; // we don't support 1, 2
				// and 4 byte data
				readPalette = this.biBitCount <= 8;
				break;
			case NO_COMPRESSION_Y8:
			case NO_COMPRESSION_GREY:
			case NO_COMPRESSION_Y800:
				this.dataTopDown = true;
				this.dataCompression = NO_COMPRESSION;
				allowedBitCount = 8;
				break;
			case NO_COMPRESSION_Y16:
			case NO_COMPRESSION_MIL:
				this.dataCompression = NO_COMPRESSION;
				allowedBitCount = 16;
				break;
			case AYUV_COMPRESSION:
				this.dataCompression = AYUV_COMPRESSION;
				allowedBitCount = 32;
				break;
			case UYVY_COMPRESSION:
			case UYNV_COMPRESSION:
				this.dataTopDown = true;
			case CYUV_COMPRESSION: // same, not top-down
			case V422_COMPRESSION:
				this.dataCompression = UYVY_COMPRESSION;
				allowedBitCount = 16;
				break;
			case YUY2_COMPRESSION:
			case YUNV_COMPRESSION:
			case YUYV_COMPRESSION:
				this.dataTopDown = true;
				this.dataCompression = YUY2_COMPRESSION;
				allowedBitCount = 16;
				break;
			case YVYU_COMPRESSION:
				this.dataTopDown = true;
				this.dataCompression = YVYU_COMPRESSION;
				allowedBitCount = 16;
				break;
			case JPEG_COMPRESSION:
			case JPEG_COMPRESSION2:
			case JPEG_COMPRESSION3:
			case MJPG_COMPRESSION:
				this.dataCompression = JPEG_COMPRESSION;
				this.variableLength = true;
				break;
			case PNG_COMPRESSION:
			case PNG_COMPRESSION2:
			case PNG_COMPRESSION3:
				this.variableLength = true;
				this.dataCompression = PNG_COMPRESSION;
				break;
			default:
				throw new Exception("Unsupported compression: " + Integer.toHexString(this.biCompression) + (this.biCompression >= 0x20202020 ? " '" + this.fourccString(this.biCompression) + "'" : ""));
		}
		
		int bitCountTest = this.biBitCount == 24 ? BITMASK24 : this.biBitCount; // convert
		// "24" to
		// a flag
		if(allowedBitCount != 0 && (bitCountTest & allowedBitCount) == 0)
		{
			throw new Exception("Unsupported: " + this.biBitCount + " bits/pixel for compression '" + this.fourccString(this.biCompression) + "'");
		}
		
		if(this.biHeight < 0)
		{
			this.biHeight = -this.biHeight;
		}
		
		// Scan line is padded with zeroes to be a multiple of four bytes
		this.scanLineSize = ((this.biWidth * this.biBitCount + 31) / 32) * 4;
		
		// a value of biClrUsed=0 means we determine this based on the bits per
		// pixel
		if(readPalette && this.biClrUsed == 0)
		{
			this.biClrUsed = 1 << this.biBitCount;
		}
		
		if(this.verbose)
		{
			IJ.log("   > data compression=0x" + Integer.toHexString(this.dataCompression) + " '" + this.fourccString(this.dataCompression) + "'");
			IJ.log("   > palette colors=" + this.biClrUsed);
			IJ.log("   > scan line size=" + this.scanLineSize);
			IJ.log("   > data top down=" + this.dataTopDown);
		}
		
		// read color palette
		if(readPalette)
		{
			long spaceForPalette = endPosition - this.raFile.getFilePointer();
			if(this.verbose)
			{
				IJ.log("   Reading " + this.biClrUsed + " Palette colors: " + this.posSizeString(spaceForPalette));
			}
			if(spaceForPalette < this.biClrUsed * 4)
			{
				throw new Exception("Not enough data (" + spaceForPalette + ") for palette of size " + (this.biClrUsed * 4));
			}
			byte[] pr = new byte[this.biClrUsed];
			byte[] pg = new byte[this.biClrUsed];
			byte[] pb = new byte[this.biClrUsed];
			for (int i = 0; i < this.biClrUsed; i++)
			{
				pb[i] = this.raFile.readByte();
				pg[i] = this.raFile.readByte();
				pr[i] = this.raFile.readByte();
				this.raFile.readByte();
			}
			this.cm = new IndexColorModel(this.biBitCount, this.biClrUsed, pr, pg, pb);
		}
	}
	
	/**
	 * Read from the 'movi' chunk. Skips audio ('..wb', etc.), 'LIST' 'rec' etc, only reads '..db' or '..dc'
	 */
	void readMovieData(long endPosition) throws Exception, IOException
	{
		if(this.verbose)
		{
			IJ.log("MOVIE DATA " + this.posSizeString(endPosition - this.raFile.getFilePointer()) + this.timeString());
		}
		if(this.verbose)
		{
			IJ.log("Searching for stream " + this.streamNumber + ": '" + this.fourccString(this.type0xdb) + "' or '" + this.fourccString(this.type0xdc) + "' chunks");
		}
		if(this.isVirtual)
		{
			if(this.frameInfos == null)
			{
				this.frameInfos = new Vector(100);
			} // holds frame positions in file
			  // (for non-constant frame sizes,
			  // should hold long[] with pos and
			  // size)
		}
		else if(this.stack == null)
		{
			this.stack = new ImageStack(this.dwWidth, this.biHeight);
		}
		while (true)
		{ // loop over all chunks
			int type = this.readType(endPosition);
			if(type == 0)
			{
				break;
			} // end of 'movi' reached?
			long size = this.readInt() & SIZE_MASK;
			long pos = this.raFile.getFilePointer();
			long nextPos = pos + size;
			if((type == this.type0xdb || type == this.type0xdc) && size > 0)
			{
				IJ.showProgress((double) this.frameNumber / this.lastFrameToRead);
				if(this.verbose)
				{
					IJ.log(this.frameNumber + " movie data '" + this.fourccString(type) + "' " + this.posSizeString(size) + this.timeString());
				}
				if(this.frameNumber >= this.firstFrame)
				{
					if(this.isVirtual)
					{
						this.frameInfos.add(new long[] { pos, size, this.frameNumber * this.dwMicroSecPerFrame });
					}
					else
					{ // read the frame
						Object pixels = this.readFrame(this.raFile, pos, (int) size);
						String label = this.frameLabel(this.frameNumber * this.dwMicroSecPerFrame);
						this.stack.addSlice(label, pixels);
					}
				}
				this.frameNumber++;
				if(this.frameNumber > this.lastFrameToRead)
				{
					break;
				}
			}
			else if(this.verbose)
			{
				IJ.log("skipped '" + this.fourccString(type) + "' " + this.posSizeString(size));
			}
			if(nextPos > endPosition)
			{
				break;
			}
			this.raFile.seek(nextPos);
		}
	}
	
	/** Reads a frame at a given position in the file, returns pixels array */
	private Object readFrame(RandomAccessFile rFile, long filePos, int size) throws Exception, IOException
	{
		rFile.seek(filePos);
		// if (verbose)
		// IJ.log("virtual AVI: readFrame @"+posSizeString(filePos, size));
		if(this.variableLength)
		{
			return this.readCompressedFrame(rFile, size);
		}
		else
		{
			return this.readFixedLengthFrame(rFile, size);
		}
	}
	
	/**
	 * Reads a JPEG or PNG-compressed frame from a RandomAccessFile and returns the pixels array of the resulting image abd sets the ColorModel cm (if appropriate)
	 */
	private Object readCompressedFrame(RandomAccessFile rFile, int size) throws Exception, IOException
	{
		InputStream inputStream = new raInputStream(rFile, size, this.biCompression == MJPG_COMPRESSION);
		BufferedImage bi = ImageIO.read(inputStream);
		if(bi == null)
		{
			throw new Exception("can't read frame, ImageIO returns null");
		}
		int type = bi.getType();
		ImageProcessor ip = null;
		// IJ.log("BufferedImage Type="+type);
		if(type == BufferedImage.TYPE_BYTE_GRAY)
		{
			ip = new ByteProcessor(bi);
		}
		else if(type == bi.TYPE_BYTE_INDEXED)
		{
			this.cm = bi.getColorModel();
			ip = new ByteProcessor((Image) bi);
		}
		else
		{
			ip = new ColorProcessor(bi);
		}
		if(this.convertToGray)
		{
			ip = ip.convertToByte(false);
		}
		if(this.flipVertical)
		{
			ip.flipVertical();
		}
		return ip.getPixels();
	}
	
	/**
	 * Read a fixed-length frame (RandomAccessFile rFile, long filePos, int size) return the pixels array of the resulting image
	 */
	private Object readFixedLengthFrame(RandomAccessFile rFile, int size) throws Exception, IOException
	{
		if(size < this.scanLineSize * this.biHeight)
		{
			throw new Exception("Data chunk size " + size + " too short (" + (this.scanLineSize * this.biHeight) + " required)");
		}
		byte[] rawData = new byte[size];
		int n = rFile.read(rawData, 0, size);
		if(n < rawData.length)
		{
			throw new Exception("Frame ended prematurely after " + n + " bytes");
		}
		
		boolean topDown = this.flipVertical ? !this.dataTopDown : this.dataTopDown;
		Object pixels = null;
		byte[] bPixels = null;
		int[] cPixels = null;
		short[] sPixels = null;
		if(this.biBitCount <= 8 || this.convertToGray)
		{
			bPixels = new byte[this.dwWidth * this.biHeight];
			pixels = bPixels;
		}
		else if(this.biBitCount == 16 && this.dataCompression == NO_COMPRESSION)
		{
			sPixels = new short[this.dwWidth * this.biHeight];
			pixels = sPixels;
		}
		else
		{
			cPixels = new int[this.dwWidth * this.biHeight];
			pixels = cPixels;
		}
		int offset = topDown ? 0 : (this.biHeight - 1) * this.dwWidth;
		int rawOffset = 0;
		for (int i = this.biHeight - 1; i >= 0; i--)
		{ // for all lines
			if(this.biBitCount <= 8)
			{
				this.unpack8bit(rawData, rawOffset, bPixels, offset, this.dwWidth);
			}
			else if(this.convertToGray)
			{
				this.unpackGray(rawData, rawOffset, bPixels, offset, this.dwWidth);
			}
			else if(this.biBitCount == 16 && this.dataCompression == NO_COMPRESSION)
			{
				this.unpackShort(rawData, rawOffset, sPixels, offset, this.dwWidth);
			}
			else
			{
				this.unpack(rawData, rawOffset, cPixels, offset, this.dwWidth);
			}
			rawOffset += this.scanLineSize;
			offset += topDown ? this.dwWidth : -this.dwWidth;
		}
		return pixels;
	}
	
	/**
	 * For one line: copy byte data into the byte array for creating a ByteProcessor
	 */
	void unpack8bit(byte[] rawData, int rawOffset, byte[] pixels, int byteOffset, int w)
	{
		for (int i = 0; i < w; i++)
		{
			pixels[byteOffset + i] = rawData[rawOffset + i];
		}
	}
	
	/**
	 * For one line: Unpack and convert YUV or RGB video data to grayscale (byte array for ByteProcessor)
	 */
	void unpackGray(byte[] rawData, int rawOffset, byte[] pixels, int byteOffset, int w)
	{
		int j = byteOffset;
		int k = rawOffset;
		if(this.dataCompression == 0)
		{
			for (int i = 0; i < w; i++)
			{
				int b0 = (((rawData[k++])) & 0xff);
				int b1 = (((rawData[k++])) & 0xff);
				int b2 = (((rawData[k++])) & 0xff);
				if(this.biBitCount == 32)
				{
					k++;
				} // ignore 4th byte (alpha value)
				pixels[j++] = (byte) ((b0 * 934 + b1 * 4809 + b2 * 2449 + 4096) >> 13); // 0.299*R+0.587*G+0.114*B
			}
		}
		else
		{
			if(this.dataCompression == UYVY_COMPRESSION || this.dataCompression == AYUV_COMPRESSION)
			{
				k++;
			} // skip first byte in these formats (chroma)
			int step = this.dataCompression == AYUV_COMPRESSION ? 4 : 2;
			for (int i = 0; i < w; i++)
			{
				pixels[j++] = rawData[k]; // Non-standard: no scaling from
				// 16-235 to 0-255 here
				k += step;
			}
		}
	}
	
	/**
	 * For one line: Unpack 16bit grayscale data and convert to short array for ShortProcessor
	 */
	void unpackShort(byte[] rawData, int rawOffset, short[] pixels, int shortOffset, int w)
	{
		int j = shortOffset;
		int k = rawOffset;
		for (int i = 0; i < w; i++)
		{
			pixels[j++] = (short) (rawData[k++] & 0xFF | ((rawData[k++] & 0xFF) << 8));
		}
	}
	
	/**
	 * For one line: Read YUV, RGB or RGB+alpha data and writes RGB int array for ColorProcessor
	 */
	void unpack(byte[] rawData, int rawOffset, int[] pixels, int intOffset, int w)
	{
		int j = intOffset;
		int k = rawOffset;
		switch (this.dataCompression)
		{
			case NO_COMPRESSION:
				for (int i = 0; i < w; i++)
				{
					int b0 = (((rawData[k++])) & 0xff);
					int b1 = (((rawData[k++])) & 0xff) << 8;
					int b2 = (((rawData[k++])) & 0xff) << 16;
					if(this.biBitCount == 32)
					{
						k++;
					} // ignore 4th byte (alpha value)
					pixels[j++] = 0xff000000 | b0 | b1 | b2;
				}
				break;
			case YUY2_COMPRESSION:
				for (int i = 0; i < w / 2; i++)
				{
					int y0 = rawData[k++] & 0xff;
					int u = rawData[k++] ^ 0xffffff80; // converts byte range
					// 0...ff to -128 ... 127
					int y1 = rawData[k++] & 0xff;
					int v = rawData[k++] ^ 0xffffff80;
					this.writeRGBfromYUV(y0, u, v, pixels, j++);
					this.writeRGBfromYUV(y1, u, v, pixels, j++);
				}
				break;
			case UYVY_COMPRESSION:
				for (int i = 0; i < w / 2; i++)
				{
					int u = rawData[k++] ^ 0xffffff80;
					int y0 = rawData[k++] & 0xff;
					int v = rawData[k++] ^ 0xffffff80;
					int y1 = rawData[k++] & 0xff;
					this.writeRGBfromYUV(y0, u, v, pixels, j++);
					this.writeRGBfromYUV(y1, u, v, pixels, j++);
				}
				break;
			case YVYU_COMPRESSION:
				for (int i = 0; i < w / 2; i++)
				{
					int y0 = rawData[k++] & 0xff;
					int v = rawData[k++] ^ 0xffffff80;
					int y1 = rawData[k++] & 0xff;
					int u = rawData[k++] ^ 0xffffff80;
					this.writeRGBfromYUV(y0, u, v, pixels, j++);
					this.writeRGBfromYUV(y1, u, v, pixels, j++);
				}
				break;
			case AYUV_COMPRESSION:
				for (int i = 0; i < w; i++)
				{
					k++; // ignore alpha channel
					int y = rawData[k++] & 0xff;
					int v = rawData[k++] ^ 0xffffff80;
					int u = rawData[k++] ^ 0xffffff80;
					this.writeRGBfromYUV(y, u, v, pixels, j++);
				}
				break;
		
		}
	}
	
	/**
	 * Write an intData RGB value converted from YUV, The y range between 16 and 235 becomes 0...255 u, v should be between -112 and +112
	 */
	final void writeRGBfromYUV(int y, int u, int v, int[] pixels, int intArrayIndex)
	{
		// int r = (int)(1.164*(y-16)+1.596*v+0.5);
		// int g = (int)(1.164*(y-16)-0.391*u-0.813*v+0.5);
		// int b = (int)(1.164*(y-16)+2.018*u+0.5);
		int r = (9535 * y + 13074 * v - 148464) >> 13;
		int g = (9535 * y - 6660 * v - 3203 * u - 148464) >> 13;
		int b = (9535 * y + 16531 * u - 148464) >> 13;
		if(r > 255)
		{
			r = 255;
		}
		if(r < 0)
		{
			r = 0;
		}
		if(g > 255)
		{
			g = 255;
		}
		if(g < 0)
		{
			g = 0;
		}
		if(b > 255)
		{
			b = 255;
		}
		if(b < 0)
		{
			b = 0;
		}
		pixels[intArrayIndex] = 0xff000000 | (r << 16) | (g << 8) | b;
	}
	
	/**
	 * Read 8-byte int with Intel (little-endian) byte order (note: RandomAccessFile.readLong has other byte order than AVI)
	 */
	
	final long readLong() throws IOException
	{
		long low = this.readInt() & 0x00000000FFFFFFFFL;
		long high = this.readInt() & 0x00000000FFFFFFFFL;
		long result = high << 32 | low;
		return result; // (high << 32 | low);
	}
	
	/**
	 * Read 4-byte int with Intel (little-endian) byte order (note: RandomAccessFile.readInt has other byte order than AVI)
	 */
	
	final int readInt() throws IOException
	{
		int result = 0;
		for (int shiftBy = 0; shiftBy < 32; shiftBy += 8)
		{
			result |= (this.raFile.readByte() & 0xff) << shiftBy;
		}
		return result;
	}
	
	/**
	 * Read 2-byte short with Intel (little-endian) byte order (note: RandomAccessFile.readShort has other byte order than AVI)
	 */
	final short readShort() throws IOException
	{
		int low = this.raFile.readByte() & 0xff;
		int high = this.raFile.readByte() & 0xff;
		return (short) (high << 8 | low);
	}
	
	/**
	 * Read type of next chunk that is not JUNK. Returns type (or 0 if no non-JUNK chunk until endPosition)
	 */
	private int readType(long endPosition) throws IOException
	{
		while (true)
		{
			long pos = this.raFile.getFilePointer();
			if(pos % this.paddingGranularity != 0)
			{
				pos = (pos / this.paddingGranularity + 1) * this.paddingGranularity;
				this.raFile.seek(pos); // pad to even address
			}
			if(pos >= endPosition)
			{
				return 0;
			}
			int type = this.readInt();
			if(type != FOURCC_JUNK)
			{
				return type;
			}
			long size = this.readInt() & SIZE_MASK;
			if(this.verbose)
			{
				IJ.log("Skip JUNK: " + this.posSizeString(size));
			}
			this.raFile.seek(this.raFile.getFilePointer() + size); // skip junk
		}
	}
	
	private void setFramesPerSecond(ImagePlus imp)
	{
		if(this.dwMicroSecPerFrame < 1000 && this.dwStreamRate > 0)
		{
			this.dwMicroSecPerFrame = (int) (this.dwStreamScale * 1e6 / this.dwStreamRate);
		}
		if(this.dwMicroSecPerFrame >= 1000)
		{
			imp.getCalibration().fps = 1e6 / this.dwMicroSecPerFrame;
		}
	}
	
	private String frameLabel(long timeMicroSec)
	{
		return IJ.d2s(timeMicroSec / 1.e6) + " s";
	}
	
	private String posSizeString(long size) throws IOException
	{
		return this.posSizeString(this.raFile.getFilePointer(), size);
	}
	
	private String posSizeString(long pos, long size) throws IOException
	{
		return "0x" + Long.toHexString(pos) + "-0x" + Long.toHexString(pos + size - 1) + " (" + size + " Bytes)";
	}
	
	private String timeString()
	{
		return " (t=" + (System.currentTimeMillis() - this.startTime) + " ms)";
	}
	
	/**
	 * returns a string of a four-cc code corresponding to an int (Intel byte order)
	 */
	private String fourccString(int fourcc)
	{
		String s = "";
		for (int i = 0; i < 4; i++)
		{
			int c = fourcc & 0xff;
			s += Character.toString((char) c);
			fourcc >>= 8;
		}
		return s;
	}
	
	private void error(String msg)
	{
		this.aborting = true;
		IJ.error("AVI Reader", msg);
	}
	
	private String exceptionMessage(Exception e)
	{
		String msg;
		if(e.getClass() == Exception.class)
		{
			msg = e.getMessage();
		}
		else
		{
			msg = e + "\n" + e.getStackTrace()[0] + "\n" + e.getStackTrace()[1];
		}
		return "An error occurred reading the AVI file.\n \n" + msg;
	}
	
	/**
	 * An input stream reading from a RandomAccessFile (starting at the current position). This class also adds 'Define Huffman Table' (DHT) segments to convert MJPG to JPEG.
	 */
	final private static int BUFFERSIZE = 4096; // should
	// be
	// large
	// enough
	// to
	// hold
	// the
	// full
	// JFIF
	// header
	// up to beginning of the image data and the Huffman tables
	final private static byte[] HUFFMAN_TABLES = new byte[] { // the 'DHT'
	// segment
	(byte) 0xFF, (byte) 0xC4, 0x01, (byte) 0xA2, // these
	// 4
	// bytes
	// are
	// tag
	// &
	// length;
	// data
	// follow
	0x00, 0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x01, 0x00, 0x03, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x10, 0x00, 0x02, 0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00, 0x01, 0x7D, 0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22, 0x71, 0x14, 0x32, (byte) 0x81, (byte) 0x91, (byte) 0xA1, 0x08, 0x23, 0x42, (byte) 0xB1, (byte) 0xC1, 0x15, 0x52, (byte) 0xD1, (byte) 0xF0, 0x24, 0x33, 0x62, 0x72, (byte) 0x82, 0x09, 0x0A, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5, (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0xAA, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0xB8, (byte) 0xB9, (byte) 0xBA, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, 0x11, 0x00, 0x02, 0x01, 0x02, 0x04, 0x04, 0x03, 0x04, 0x07, 0x05, 0x04, 0x04, 0x00, 0x01, 0x02, 0x77, 0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71, 0x13, 0x22, 0x32, (byte) 0x81, 0x08, 0x14, 0x42, (byte) 0x91, (byte) 0xA1, (byte) 0xB1, (byte) 0xC1, 0x09, 0x23, 0x33, 0x52, (byte) 0xF0, 0x15, 0x62, 0x72, (byte) 0xD1, 0x0A, 0x16, 0x24, 0x34, (byte) 0xE1, 0x25, (byte) 0xF1, 0x17, 0x18, 0x19, 0x1A, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5, (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0xAA, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0xB8, (byte) 0xB9, (byte) 0xBA, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA };
	final private static int HUFFMAN_LENGTH = 420;
	
	class raInputStream extends InputStream {
		
		RandomAccessFile rFile; // where to read the data from
		int readableSize; // number of bytes that one should
		// expect to be readable
		boolean fixMJPG; // whether to use an ugly hack to
		// convert MJPG frames to JPEG
		byte[] buffer; // holds beginning of data for fixing
		// Huffman tables
		int bufferPointer; // next position in buffer to read
		int bufferLength; // bytes allocated in buffer
		
		/** Constructor */
		raInputStream(RandomAccessFile rFile, int readableSize, boolean fixMJPG) throws IOException
		{
			this.rFile = rFile;
			this.readableSize = readableSize;
			this.fixMJPG = fixMJPG;
			if(fixMJPG)
			{
				this.buffer = new byte[BUFFERSIZE];
				this.bufferLength = Math.min(BUFFERSIZE - HUFFMAN_LENGTH, readableSize);
				this.bufferLength = rFile.read(this.buffer, 0, this.bufferLength);
				this.addHuffmanTables();
			}
		}
		
		@Override
		public int available()
		{
			return this.readableSize;
		}
		
		// Read methods:
		// There is no check against reading beyond the allowed range, which is
		// start position + readableSize
		// (i.e., reading beyond the frame in the avi file would be possible).
		/** Read a single byte */
		@Override
		public int read() throws IOException
		{
			this.readableSize--;
			if(this.fixMJPG)
			{
				int result = this.buffer[this.bufferPointer] & 0xff;
				this.bufferPointer++;
				if(this.bufferPointer >= this.bufferLength)
				{
					this.fixMJPG = false;
				} // buffer exhausted, no more attempt to fix
				  // it
				return result;
			}
			else
			{
				return this.rFile.read();
			}
		}
		
		/** Read bytes into an array */
		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{
			// IJ.log("read "+len+" bytes, fix="+fixMJPG);
			int nBytes;
			if(this.fixMJPG)
			{
				nBytes = Math.min(len, this.bufferLength - this.bufferPointer);
				System.arraycopy(this.buffer, this.bufferPointer, b, off, nBytes);
				this.bufferPointer += nBytes;
				if(this.bufferPointer >= this.bufferLength)
				{
					this.fixMJPG = false;
					if(len - nBytes > 0)
					{
						nBytes += this.rFile.read(b, off + nBytes, len - nBytes);
					}
				}
			}
			else
			{
				nBytes = this.rFile.read(b, off, len);
			}
			this.readableSize -= nBytes;
			return nBytes;
		}
		
		// Add Huffman table if not present yet
		private void addHuffmanTables()
		{
			if(this.readShort(0) != 0xffd8 || this.bufferLength < 6)
			{
				return;
			} // not a start of JPEG-like data
			int offset = 2;
			int segmentLength = 0;
			do
			{
				int code = this.readShort(offset); // read segment type
				// IJ.log("code=0x"+Long.toHexString(code));
				if(code == 0xffc4)
				{
					return;
				}
				else if(code == 0xffda || code == 0xffd9)
				{ // start of image data or end of image?
					this.insertHuffmanTables(offset);
					return; // finished
				}
				offset += 2;
				segmentLength = this.readShort(offset); // read length of this
				// segment
				offset += segmentLength; // and skip the segment contents
			}
			while (offset < this.bufferLength - 4 && segmentLength >= 0);
		}
		
		// read a short from the buffer
		private int readShort(int offset)
		{
			return ((this.buffer[offset] & 0xff) << 8) | (this.buffer[offset + 1] & 0xff);
		}
		
		// insert Huffman tables at the given position
		private void insertHuffmanTables(int position)
		{
			// IJ.log("inserting Huffman tables");
			System.arraycopy(this.buffer, position, this.buffer, position + HUFFMAN_LENGTH, this.bufferLength - position);
			System.arraycopy(HUFFMAN_TABLES, 0, this.buffer, position, HUFFMAN_LENGTH);
			this.bufferLength += HUFFMAN_LENGTH;
			this.readableSize += HUFFMAN_LENGTH;
		}
	}
}

package services;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import io.humble.video.Codec;
import io.humble.video.Encoder;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.Muxer;
import io.humble.video.MuxerFormat;
import io.humble.video.PixelFormat;
import io.humble.video.Rational;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

@Path("/upload")
public class FileUploadService {

	// private SseBroadcaster broadcaster = new SseBroadcaster();

	/** The path to the folder where we want to store the uploaded files */
	private static final String UPLOAD_FOLDER = "c:/uploadedFiles/";
	private static final String UPLOAD_FILENAME = "IMAGE0.";
	private static final int IMG_WIDTH = 480;
	private static final int IMG_HEIGHT = 320;

	public FileUploadService() {
	}

	@Context
	private UriInfo context;

	/**
	 * Returns text response to caller containing uploaded file location
	 * 
	 * @return error response in case of missing parameters an internal
	 *         exception or success response if file has been stored
	 *         successfully
	 * @throws InterruptedException 
	 * @throws AWTException 
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws AWTException, InterruptedException {

		if (uploadedInputStream == null || fileDetail == null) {
			return Response.status(400).entity("Invalid form data").build();
		}
		// create our destination folder, if it not exists
		try {
			createFolderIfNotExists(UPLOAD_FOLDER);
		} catch (SecurityException se) {
			return Response.status(500).entity("Can not create destination folder on server").build();
		}
		String uploadedFileLocation = UPLOAD_FOLDER + UPLOAD_FILENAME + "jpg";
		String saveVideoLocation = UPLOAD_FOLDER + UPLOAD_FILENAME + "mp4";
		try {
			saveToFile(uploadedInputStream, uploadedFileLocation);
//			BufferedImage img = ImageIO.read(new File(uploadedFileLocation));
//			BufferedImage newBufferedImage = new BufferedImage(img.getWidth(),
//					img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
//			  newBufferedImage.createGraphics().drawImage(img, 0, 0, Color.WHITE, null);
//			recordScreen(saveVideoLocation,null,null, newBufferedImage);
		} catch (IOException e) {
			return Response.status(500).entity("Can not save file \n" + e.getMessage()).build();
		}
		return Response.status(200).entity("File saved to " + uploadedFileLocation).build();
	}

	/**
	 * Utility method to save InputStream data to target location/file
	 * 
	 * @param inStream
	 *            - InputStream to be saved
	 * @param target
	 *            - full path to destination file
	 */
	private void saveToFile(InputStream inStream, String target) throws IOException {
		OutputStream out = null;
		int read = 0;
		byte[] bytes = new byte[1024];
		out = new FileOutputStream(new File(target));
		while ((read = inStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		out.flush();
		out.close();
	}

	/**
	 * Creates a folder to desired location if it not already exists
	 * 
	 * @param dirName
	 *            - full path to the folder
	 * @throws SecurityException
	 *             - in case you don't have permission to create the folder
	 */
	private void createFolderIfNotExists(String dirName) throws SecurityException {
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdir();
		}
	}

	@Path("/my-servlet")
	@GET
	public String echoServletHeaders(@Context HttpServletResponse context) {
		context.setContentType("image/jpeg");
			try {
				// Files.copy(Paths.get(UPLOAD_FOLDER + UPLOAD_FILENAME +
				// "jpg"),Paths.get(UPLOAD_FOLDER + CLONE_FILENAME + "jpg"));
				RandomAccessFile f = new RandomAccessFile(UPLOAD_FOLDER + UPLOAD_FILENAME + "jpg", "rw");
				byte[] fileContent = new byte[(int) f.length()];
				f.readFully(fileContent);
				OutputStream outputStream = context.getOutputStream();
				outputStream.write(("Content-Length: "
						+ fileContent.length + "\r\n\r\n").getBytes());
				outputStream.write(fileContent);
				outputStream.write("\r\n\r\n".getBytes());
			} catch (Exception e) {
			}
			return "";
	}

	private static void recordScreen(String filename, String formatname, String codecname, BufferedImage screen)
			throws AWTException, InterruptedException, IOException {

		final Rational framerate = Rational.make(1, 10);
		/**
		 * First we create a muxer using the passed in filename and formatname
		 * if given.
		 */
		final Muxer muxer = Muxer.make(filename, null, formatname);

		/**
		 * Now, we need to decide what type of codec to use to encode video.
		 * Muxers have limited sets of codecs they can use. We're going to pick
		 * the first one that works, or if the user supplied a codec name, we're
		 * going to force-fit that in instead.
		 */
		final MuxerFormat format = muxer.getFormat();
		final Codec codec;
		if (codecname != null) {
			codec = Codec.findEncodingCodecByName(codecname);
		} else {
			codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
		}

		/**
		 * Now that we know what codec, we need to create an encoder
		 */
		Encoder encoder = Encoder.make(codec);

		/**
		 * Video encoders need to know at a minimum: width height pixel format
		 * Some also need to know frame-rate (older codecs that had a fixed rate
		 * at which video files could be written needed this). There are many
		 * other options you can set on an encoder, but we're going to keep it
		 * simpler here.
		 */
		encoder.setWidth(screen.getWidth());
		encoder.setHeight(screen.getHeight());
		// We are going to use 420P as the format because that's what most video
		// formats these days use
		final PixelFormat.Type pixelformat = PixelFormat.Type.PIX_FMT_YUV420P;
		encoder.setPixelFormat(pixelformat);
		encoder.setTimeBase(framerate);
		/**
		 * An annoynace of some formats is that they need global (rather than
		 * per-stream) headers, and in that case you have to tell the encoder.
		 * And since Encoders are decoupled from Muxers, there is no easy way to
		 * know this beyond
		 */
		if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER)) {
			encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
		}

		/** Open the encoder. */
		encoder.open(null, null);

		/** Add this stream to the muxer. */
		muxer.addNewStream(encoder);

		/** And open the muxer for business. */
		muxer.open(null, null);

		/**
		 * Next, we need to make sure we have the right MediaPicture format
		 * objects to encode data with. Java (and most on-screen graphics
		 * programs) use some variant of Red-Green-Blue image encoding (a.k.a.
		 * RGB or BGR). Most video codecs use some variant of YCrCb formatting.
		 * So we're going to have to convert. To do that, we'll introduce a
		 * MediaPictureConverter object later. object.
		 */
		MediaPictureConverter converter = null;
		final MediaPicture picture = MediaPicture.make(encoder.getWidth(), encoder.getHeight(), pixelformat);

		/**
		 * Now begin our main loop of taking screen snaps. We're going to encode
		 * and then write out any resulting packets.
		 */
		final MediaPacket packet = MediaPacket.make();

		/** Make the screen capture && convert image to TYPE_3BYTE_BGR */

		/**
		 * This is LIKELY not in YUV420P format, so we're going to convert it
		 * using some handy utilities.
		 */
		if (converter == null) {
			converter = MediaPictureConverterFactory.createConverter(screen, picture);
		}

		do {
			encoder.encode(packet, picture);
			if (packet.isComplete()) {
				muxer.write(packet, false);
			}
		} while (packet.isComplete());

		/** now we'll sleep until it's time to take the next snapshot. */

		/**
		 * Encoders, like decoders, sometimes cache pictures so it can do the
		 * right key-frame optimizations. So, they need to be flushed as well.
		 * As with the decoders, the convention is to pass in a null input until
		 * the output is not complete.
		 */
		do {
			encoder.encode(packet, null);
			if (packet.isComplete()) {
				muxer.write(packet, false);
			}
		} while (packet.isComplete());

		/** Finally, let's clean up after ourselves. */
		muxer.close();
	}

	private static BufferedImage resizeImage(BufferedImage originalImage, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();

		return resizedImage;
	}
}

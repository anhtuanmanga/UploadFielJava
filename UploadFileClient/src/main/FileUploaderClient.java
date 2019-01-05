package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;

/**
 * This example shows how to upload files using POST requests with encoding type
 * "multipart/form-data". For more details please read the full tutorial on
 * https://javatutorial.net/java-file-upload-rest-service
 * 
 * @author javatutorial.net
 */
public class FileUploaderClient {

	private static final int IMG_WIDTH = 480;
	private static final int IMG_HEIGHT = 320;

	public static void main(String[] args) throws IOException, InterruptedException {
		while (true) {
			File folder = new File("C:\\Users\\Nguyen Anh Tuan\\Desktop\\Test Frame 2");
			for (final File fileEntry : folder.listFiles()) {
				if (fileEntry.isDirectory()) {
					listFilesForFolder(fileEntry);
				} else {
					// the file we want to upload
					BufferedImage originalImage = ImageIO.read(fileEntry);
						
					int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
					
					BufferedImage resizeImageJpg = resizeImage(originalImage, type);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					ImageIO.write(resizeImageJpg, "jpg", os);
					FileInputStream fis = null;
					try {
						DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());

						// server back-end URL
						HttpPost httppost = new HttpPost("http://localhost:8080/UploadFile/rest/upload");
						MultipartEntity entity = new MultipartEntity();
						// set the file input stream and file name as arguments
						entity.addPart("file", new InputStreamBody(new ByteArrayInputStream(os.toByteArray()), fileEntry.getName()));
						httppost.setEntity(entity);
						// execute the request
						HttpResponse response = httpclient.execute(httppost);

						int statusCode = response.getStatusLine().getStatusCode();
						HttpEntity responseEntity = response.getEntity();
						String responseString = EntityUtils.toString(responseEntity, "UTF-8");

						System.out.println("[" + statusCode + "] " + responseString);
						TimeUnit.MILLISECONDS.sleep(10);
					} catch (ClientProtocolException e) {
						System.err.println("Unable to make connection");
						e.printStackTrace();
						return;
					} catch (IOException e) {
						System.err.println("Unable to read file");
						e.printStackTrace();
						return;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} finally {
						try {
							if (fis != null)
								fis.close();
						} catch (IOException e) {
							return;
						}
					}
				}
			}
		}
	}

	private static BufferedImage resizeImage(BufferedImage originalImage, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();

		return resizedImage;
	}

	public static void listFilesForFolder(final File folder) throws IOException, InterruptedException {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				// the file we want to upload
				BufferedImage originalImage = ImageIO.read(fileEntry);
				int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
					
				BufferedImage resizeImageJpg = resizeImage(originalImage, type);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				ImageIO.write(resizeImageJpg, "jpg", os);
				FileInputStream fis = null;
				try {
					DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());

					// server back-end URL
					HttpPost httppost = new HttpPost("http://localhost:8080/UploadFile/rest/upload");
					MultipartEntity entity = new MultipartEntity();
					// set the file input stream and file name as arguments
					entity.addPart("file", new InputStreamBody(new ByteArrayInputStream(os.toByteArray()), fileEntry.getName()));
					httppost.setEntity(entity);
					// execute the request
					HttpResponse response = httpclient.execute(httppost);

					int statusCode = response.getStatusLine().getStatusCode();
					HttpEntity responseEntity = response.getEntity();
					String responseString = EntityUtils.toString(responseEntity, "UTF-8");

					System.out.println("[" + statusCode + "] " + responseString);
					TimeUnit.MILLISECONDS.sleep(10);
				} catch (ClientProtocolException e) {
					System.err.println("Unable to make connection");
					e.printStackTrace();
					return;
				} catch (IOException e) {
					System.err.println("Unable to read file");
					e.printStackTrace();
					return;
				} finally {
					try {
						if (fis != null)
							fis.close();
					} catch (IOException e) {
						return;
					}
				}
			}
		}
	}

}

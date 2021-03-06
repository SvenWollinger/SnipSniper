package io.wollinger.snipsniper.utils;

import io.wollinger.snipsniper.Config;
import io.wollinger.snipsniper.SnipSniper;
import org.apache.commons.lang3.SystemUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

public class Utils {
	
	public static boolean isInteger(String string) {
	    try {
	        Integer.valueOf(string);
	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
	}

	public static boolean isDouble(String string) {
		try {
			Double.valueOf(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String replaceVars(String string) {
		if(string.contains("%username%")) string = string.replace("%username%", System.getProperty("user.name"));
		if(SystemUtils.IS_OS_WINDOWS) if(string.contains("%userprofile%")) string = string.replace("%userprofile%", System.getenv("USERPROFILE"));
		if(SystemUtils.IS_OS_LINUX) if(string.contains("%userprofile%")) string = string.replace("%userprofile%", System.getProperty("user.home"));
		return string;
	}

	public static Color getContrastColor(Color color) {
		double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
		return y >= 128 ? Color.black : Color.white;
	}

	public static void printArgs(PrintStream out, final String message, final Object... args) {
		out.println(formatArgs(message, args));
	}

	public static BufferedImage getDragPasteImage(BufferedImage icon, String text) {
		BufferedImage dropImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		Graphics g = dropImage.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0,0,dropImage.getWidth(), dropImage.getHeight());
		g.setColor(Color.BLACK);
		g.setFont(new Font("Meiryo", Font.BOLD, 20));
		int width = g.getFontMetrics().stringWidth(text);
		g.drawString(text, dropImage.getWidth()/2 - width/2, dropImage.getHeight()/2);
		g.drawImage(icon, dropImage.getWidth()/3,dropImage.getHeight()/10, dropImage.getWidth()/3, dropImage.getHeight()/3, null);
		g.dispose();
		return dropImage;
	}

	//https://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
	public static boolean restartApplication(String encoding, String... args) throws URISyntaxException, IOException {
		final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		final File currentJar = new File(SnipSniper.class.getProtectionDomain().getCodeSource().getLocation().toURI());

		if(!currentJar.getName().endsWith(".jar"))
			return false;

		final ArrayList<String> command = new ArrayList<>();
		command.add(javaBin);
		if(encoding != null && !encoding.isEmpty())
			command.add("-Dfile.encoding=" + encoding);
		command.add("-jar");
		command.add(currentJar.getPath());
		command.add("-r");
		Collections.addAll(command, args);

		final ProcessBuilder builder = new ProcessBuilder(command);
		builder.start();
		SnipSniper.exit(true);
		return true;
	}

	public static String getFileExtension(File file) {
		String name = file.getName();
		int lastIndexOf = name.lastIndexOf(".");
		if (lastIndexOf == -1) {
			return ""; // empty extension
		}
		return name.substring(lastIndexOf);
	}

	public static Image getImageFromClipboard() {
		Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			try {
				return (Image) transferable.getTransferData(DataFlavor.imageFlavor);
			} catch (UnsupportedFlavorException | IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Dimension getScaledDimension(BufferedImage image, Dimension boundary) {
		return Utils.getScaledDimension(new Dimension(image.getWidth(), image.getHeight()), boundary);
	}

	//https://stackoverflow.com/a/10245583
	public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
		int original_width = imgSize.width;
		int original_height = imgSize.height;
		int bound_width = boundary.width;
		int bound_height = boundary.height;
		int new_width = original_width;
		int new_height = original_height;

		// first check if we need to scale width
		if (original_width > bound_width) {
			//scale width to fit
			new_width = bound_width;
			//scale height to maintain aspect ratio
			new_height = (new_width * original_height) / original_width;
		}

		// then check if we need to scale even with the new height
		if (new_height > bound_height) {
			//scale height to fit instead
			new_height = bound_height;
			//scale width to maintain aspect ratio
			new_width = (new_height * original_width) / original_height;
		}

		return new Dimension(new_width, new_height);
	}

	public static BufferedImage imageToBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		BufferedImage image = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return image;
	}

	public static String getDateWithProperZero(int date) {
		String dateString = date + "";
		if(date < 10)
			dateString = "0" + date;
		return dateString;
	}

	public static String saveImage(String profileID, BufferedImage finalImg, String modifier, Config config) {
		File file;
		String filename = Utils.constructFilename(modifier);
		String savePath = config.getString(ConfigHelper.PROFILE.pictureFolder);

		String savePathModifier = "";

		if(config.getBool(ConfigHelper.PROFILE.dateFolders)) {
			LocalDate currentDate = LocalDate.now();

			String dayString = getDateWithProperZero(currentDate.getDayOfMonth());
			String monthString = getDateWithProperZero(currentDate.getMonthValue());

			savePathModifier = "\\" + config.getString(ConfigHelper.PROFILE.dateFoldersFormat);
			savePathModifier = savePathModifier.replaceAll("%day%", dayString);
			savePathModifier = savePathModifier.replaceAll("%month%", monthString);
			savePathModifier = savePathModifier.replaceAll("%year%", currentDate.getYear() + "");
		}

		File path = new File(savePath + savePathModifier);
		file = new File(path.getAbsolutePath() + "//" + filename);
		try {
			if(config.getBool(ConfigHelper.PROFILE.saveToDisk)) {
				if(!path.exists()) {
					if(!path.mkdirs()) {
						LogManager.log(profileID, "Failed saving, directory missing & could not create it!", Level.WARNING);
						return null;
					}
				}
				if(file.createNewFile()) {
					ImageIO.write(finalImg, "png", file);
					LogManager.log(profileID, "Saved image on disk. Location: " + file, Level.INFO);
					return file.getAbsolutePath();
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not save image to \"" + file + "\"!" , "Error", JOptionPane.INFORMATION_MESSAGE);
			LogManager.log(profileID, "Failed Saving. Wanted Location: " + file, Level.WARNING);
			LogManager.log(profileID, "Detailed Error: " + e.getMessage(), Level.WARNING);
			e.printStackTrace();
			return null;
		}
		return null;
	}

	public static void copyToClipboard(String id, BufferedImage img) {
		ImageSelection imgSel = new ImageSelection(img);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
		LogManager.log(id, "Copied Image to clipboard", Level.INFO);
	}

	public static String constructFilename(String modifier) {
		LocalDateTime now = LocalDateTime.now();
		String filename = now.toString().replace(".", "_").replace(":", "_");
		filename += modifier + ".png";
		return filename;
	}

	public static String formatArgs(final String message, final Object ...args) {
		final int size = args.length;
		String newMessage = message;
		for(int i = 0; i < size; i++) {
			final String id = "\\{" + i + "}";
			String replacer = "NULL";
			if(args[i] != null)
				replacer = args[i].toString();
			newMessage = newMessage.replaceAll(id, replacer);
		}
		return newMessage;
	}
	
	public static BufferedImage resizeImage(BufferedImage original, int width, int height) {
		BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = newImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(original, 0,0,width, height, null);
		g.dispose();
		return newImage;
	}

	public static String rgb2hex(Color color) {
		String additional = "";
		if(color.getAlpha() != 255)
			additional = "_" + color.getAlpha();
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()) + additional;
	}

	public static Color hex2rgb(String colorStr) {
		String[] values = colorStr.split("_");
		int alpha = 255;
		if(values.length > 1)
			alpha = Integer.parseInt(values[1]);
	    return new Color(Integer.valueOf(values[0].substring(1, 3), 16), Integer.valueOf( values[0].substring(3, 5), 16), Integer.valueOf(values[0].substring(5, 7), 16), alpha);
	}

	public static synchronized BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), null);
	    g.dispose();
	    return b;
	}

	public static String loadFileFromJar(String file) throws IOException {
		StringBuilder content = new StringBuilder();
		InputStream inputStream = ClassLoader.getSystemResourceAsStream(file);
		if(inputStream == null)
			throw new FileNotFoundException(Utils.formatArgs("Could not load file {0} from jar!", file));
		InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader in = new BufferedReader(streamReader);

		for (String line; (line = in.readLine()) != null;)
			content.append(line);

		inputStream.close();
		streamReader.close();
		return content.toString();
	}

	public static Rectangle fixRectangle(Rectangle rect) {
		Rectangle newRect = new Rectangle();
		newRect.x = Math.min(rect.x, rect.width);
		newRect.y = Math.min(rect.y, rect.height);
		newRect.width = Math.max(rect.x, rect.width);
		newRect.height = Math.max(rect.y, rect.height);
		return newRect;
	}
}

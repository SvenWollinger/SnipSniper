package io.wollinger.snipsniper.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class Utils {
	
	public static boolean isInteger(String string) {
	    try {
	        Integer.valueOf(string);
	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
	}
	
	public static String rgb2hex(Color _color) {
		return String.format("#%02x%02x%02x", _color.getRed(), _color.getGreen(), _color.getBlue()); 
	}
	
	public static BufferedImage resizeImage(BufferedImage _original, int _width, int _height) {
		BufferedImage newImage = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = newImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(_original, 0,0,_width, _height, null);
		g.dispose();
		return newImage;
	}
	
	public static Color hex2rgb(String colorStr) {
	    return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf( colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), null);
	    return b;
	}
}
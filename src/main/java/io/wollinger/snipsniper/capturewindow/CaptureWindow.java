package io.wollinger.snipsniper.capturewindow;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.logging.Level;

import javax.swing.JFrame;

import io.wollinger.snipsniper.Config;
import io.wollinger.snipsniper.SnipSniper;
import io.wollinger.snipsniper.sceditor.SCEditorWindow;
import io.wollinger.snipsniper.systray.Sniper;
import io.wollinger.snipsniper.utils.*;
import org.apache.commons.lang3.SystemUtils;

public class CaptureWindow extends JFrame implements WindowListener{
	private final Sniper sniperInstance;
	private final Config config;
	private final RenderingHints qualityHints;
	private final CaptureWindowListener listener;

	private Rectangle bounds = null;
	
	public BufferedImage screenshot = null;
	public BufferedImage screenshotTinted = null;

	public boolean startedCapture = false;
	public boolean isRunning = true;

	public CaptureWindow(Sniper sniperInstance) {
		this.sniperInstance = sniperInstance;
		config = sniperInstance.getConfig();

		qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		if(SystemTray.isSupported()) sniperInstance.getTrayIcon().setImage(Icons.alt_icons[sniperInstance.profileID]);
		if(sniperInstance.getConfig().getInt(ConfigHelper.PROFILE.snipeDelay) != 0) {
			try {
				Thread.sleep(sniperInstance.getConfig().getInt(ConfigHelper.PROFILE.snipeDelay) * 1000L);
			} catch (InterruptedException e) {
				LogManager.log(sniperInstance.getID(), "There was an error with the delay! Message: " + e.getMessage(), Level.SEVERE);
				LogManager.log(sniperInstance.getID(), "More info: " + Arrays.toString(e.getStackTrace()), Level.SEVERE);
			}
		}
		
		screenshot();

		setUndecorated(true);
		setIconImage(Icons.icon_taskbar);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		listener = new CaptureWindowListener(this);
		addWindowListener(this);
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addKeyListener(listener);
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent focusEvent) { }

			@Override
			public void focusLost(FocusEvent focusEvent) {
				setSize();
			}
		});
     	setVisible(true);
		setSize();
		if(SystemUtils.IS_OS_LINUX) GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
	   	loop();
	}
	
	
	public void loop() {
		Thread thread = new Thread(() -> {
			final double nsPerTick = 1000000000D / config.getInt(ConfigHelper.PROFILE.maxFPS);
			long lastTime = System.nanoTime();
			long lastTimer = System.currentTimeMillis();
			double delta = 0;
			boolean screenshotDone = false;

			while (isRunning) {
				if (screenshotDone) {
					if(!isVisible()) setVisible(true);
					setSize();
					specialRepaint();
				}
				if (screenshot != null && screenshotTinted != null && !screenshotDone) screenshotDone = true;

				long now = System.nanoTime();
				delta += (now - lastTime) / nsPerTick;
				lastTime = now;

				while (delta >= 1) {
					delta -= 1;
					if (screenshotDone) specialRepaint();
				}

				if (System.currentTimeMillis() - lastTimer >= 1000)
					lastTimer += 1000;
			}
		});
		thread.start();
	}
	
	public void specialRepaint() {
		if(selectArea != null) {
			final Rectangle rect = selectArea;
			
			int x = Math.min( rect.x, rect.width);
			int y = Math.min( rect.y, rect.height);
			int width = Math.max(rect.x, rect.width);
			int height = Math.max(rect.y, rect.height);

			repaint(x,y,width,height);
		} else {
			repaint();
		}

	}
	
	public synchronized void screenshot() {
		bounds = getTotalBounds();
		Rectangle screenshotRect = new Rectangle((int)bounds.getX(),(int)bounds.getY(), bounds.width, bounds.height);
		try {
			screenshot = new Robot().createScreenCapture(screenshotRect);
		} catch (AWTException e) {
			LogManager.log(sniperInstance.getID(), "Couldn't take screenshot. Message: " + e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
		screenshotTinted = Utils.copyImage(screenshot);
		Graphics g2 = screenshotTinted.getGraphics();
		g2.setColor(new Color(100,100,100,100));
		g2.fillRect(0, 0, screenshotTinted.getTileWidth(), screenshotTinted.getHeight());
	    g2.dispose();
	}
	
	public void setSize() {
		setLocation((int)bounds.getX(),(int)bounds.getY());
		setSize(bounds.width, bounds.height);
		requestFocus();
		setAlwaysOnTop(true);
		repaint();
	}
	
	Rectangle calcRectangle() {
		int minX = 0, maxX = 0, minY = 0, maxY = 0;
		Point startPoint = listener.getStartPoint(PointType.NORMAL);
		Point cPoint = listener.getCurrentPoint(PointType.NORMAL);
		if(startPoint != null && cPoint != null) {
			minX = Math.min( startPoint.x, cPoint.x);
			maxX = Math.max( startPoint.x, cPoint.x);
			minY = Math.min( startPoint.y, cPoint.y);
			maxY = Math.max( startPoint.y, cPoint.y);
		}
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}
	
	Rectangle getTotalBounds() {
		Rectangle2D result = new Rectangle2D.Double();
		GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (GraphicsDevice gd : localGE.getScreenDevices()) {
			for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
				Rectangle2D.union(result, graphicsConfiguration.getBounds(), result);
			}
		}
		return result.getBounds();
	}
	
	void capture() {
		BufferedImage finalImg;
		isRunning = false;
		dispose();

		int borderSize = config.getInt(ConfigHelper.PROFILE.borderSize);
		Rectangle captureArea = calcRectangle();

		if (captureArea.width == 0 || captureArea.height == 0) {
			sniperInstance.getTrayIcon().displayMessage("Error: Screenshot width or height is 0!", "ERROR", MessageType.ERROR);
			sniperInstance.killCaptureWindow();
			return;
		}

		BufferedImage croppedBuffer = screenshot.getSubimage(captureArea.x, captureArea.y, captureArea.width, captureArea.height);
		finalImg = new BufferedImage(croppedBuffer.getWidth() + borderSize *2, croppedBuffer.getHeight() + borderSize *2, BufferedImage.TYPE_INT_RGB);
		Graphics g = finalImg.getGraphics();
		g.setColor(config.getColor(ConfigHelper.PROFILE.borderColor));
		g.fillRect(0, 0, finalImg.getWidth(),finalImg.getHeight());
		g.drawImage(croppedBuffer, borderSize, borderSize, croppedBuffer.getWidth(), croppedBuffer.getHeight(), this);
		g.dispose();

		String finalLocation = null;
		boolean inClipboard = false;

		if(config.getBool(ConfigHelper.PROFILE.saveToDisk)) {
			finalLocation = Utils.saveImage(sniperInstance.getID(), finalImg, "", config);
		}

		if(config.getBool(ConfigHelper.PROFILE.copyToClipboard)) {
			Utils.copyToClipboard(sniperInstance.getID(), finalImg);
			inClipboard = true;
		}

		Point startPointTotal = listener.getStartPoint(PointType.TOTAL);
		Point cPointTotal = listener.getCurrentPoint(PointType.TOTAL);

		int posX = (int) cPointTotal.getX();
		int posY = (int) cPointTotal.getY();
		boolean leftToRight = false;

		if (!(startPointTotal.getX() > cPointTotal.getX())) {
			posX -= finalImg.getWidth();
			leftToRight = true;
		}
		if (!(startPointTotal.getY() > cPointTotal.getY())) {
			posY -= finalImg.getHeight();
			leftToRight = true;
		}
		if (config.getBool(ConfigHelper.PROFILE.openEditor)) {
			new SCEditorWindow("EDI" + sniperInstance.profileID, finalImg, posX, posY, "SnipSniper Editor", config, leftToRight, finalLocation, inClipboard, false);
		}

		sniperInstance.killCaptureWindow();
	}
	
	private Rectangle selectArea;
	private boolean hasSaved = false;
	private BufferedImage globalBufferImage;
	private BufferedImage selectBufferImage;
	private BufferedImage spyglassBufferImage;

	private final RectangleCollection allBounds = new RectangleCollection();

	private Rectangle lastRect;
	private boolean spyglassToggle = false;

	public void paint(Graphics g) {
		boolean directDraw = config.getBool(ConfigHelper.PROFILE.directDraw);
		//TODO: Direct draw runs horribly on linux. Check out why?

		if(lastRect == null)
			lastRect = bounds;

		if(!directDraw && bounds != null && globalBufferImage == null && selectBufferImage == null) {
			//We are only setting this once, since the size of bounds should not really change
			globalBufferImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
			selectBufferImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
		}

		if(spyglassBufferImage == null && config.getBool(ConfigHelper.PROFILE.enableSpyglass)) {
			spyglassBufferImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
		}

		Graphics2D globalBuffer = (Graphics2D) globalBufferImage.getGraphics();
		globalBuffer.setRenderingHints(qualityHints);

		Graphics2D selectBuffer = (Graphics2D) selectBufferImage.getGraphics();
		selectBuffer.setRenderingHints(qualityHints);

		Graphics2D spyglassBuffer = (Graphics2D) spyglassBufferImage.getGraphics();
		spyglassBuffer.setRenderingHints(qualityHints);

		if(directDraw) {
			globalBuffer = (Graphics2D) g;
			selectBuffer = (Graphics2D) g;
			spyglassBuffer = (Graphics2D) g;
		}

		Rectangle clearRect = allBounds.getBounds();
		if(clearRect != null) {
			globalBuffer.drawImage(screenshotTinted, clearRect.x, clearRect.y, clearRect.width, clearRect.height, clearRect.x, clearRect.y, clearRect.width, clearRect.height, this);
		}
		allBounds.clear();

		if(screenshot != null) {
			if((screenshotTinted != null && !hasSaved && bounds != null) || SystemUtils.IS_OS_LINUX) {
				if(SnipSniper.getConfig().getBool(ConfigHelper.MAIN.debug)) {
					LogManager.log(sniperInstance.getID(), "About to render image: " + screenshotTinted, Level.INFO);
					LogManager.log(sniperInstance.getID(), "Frame Visible: " + isVisible(), Level.INFO);
				}

				globalBuffer.drawImage(screenshotTinted, 0,0, bounds.width,bounds.height, this);
				allBounds.addRectangle(bounds);

				if(SnipSniper.getConfig().getBool(ConfigHelper.MAIN.debug)) {
					LogManager.log(sniperInstance.getID(), "Rendered tinted background. More Info: ", Level.INFO);
					LogManager.log(sniperInstance.getID(), "Image rendered:        " + screenshotTinted.toString(), Level.INFO);
					LogManager.log(sniperInstance.getID(), "Frame Visible: " + isVisible(), Level.INFO);
				}
				hasSaved = true;
			}

			Point cPoint = listener.getCurrentPoint(PointType.NORMAL);
			Point cPointLive = listener.getCurrentPoint(PointType.LIVE);
			Point startPoint = listener.getStartPoint(PointType.NORMAL);

			if(selectArea != null && cPoint != null && startedCapture) {
				selectBuffer.drawImage(screenshot, startPoint.x, startPoint.y, cPoint.x, cPoint.y,startPoint.x, startPoint.y, cPoint.x, cPoint.y, this);
			}

			if(cPoint != null && startPoint != null) {
				selectArea = new Rectangle(startPoint.x, startPoint.y, cPoint.x, cPoint.y);
				allBounds.addRectangle(Utils.fixRectangle(selectArea));
			}

			if(cPoint != null && selectArea != null) {
				globalBuffer.drawImage(selectBufferImage, selectArea.x, selectArea.y, selectArea.width, selectArea.height, selectArea.x, selectArea.y, selectArea.width, selectArea.height, this);
			}

			if(cPointLive != null && config.getBool(ConfigHelper.PROFILE.enableSpyglass)) {
				boolean displaySpyglass = true;
				switch(config.getString(ConfigHelper.PROFILE.spyglassMode)) {
					case "hold":
						if(!listener.isPressed(config.getInt(ConfigHelper.PROFILE.spyglassHotkey))) displaySpyglass = false;
						break;
					case "toggle":
						if(listener.isPressedOnce(config.getInt(ConfigHelper.PROFILE.spyglassHotkey))) spyglassToggle = !spyglassToggle;
						displaySpyglass = spyglassToggle;
						break;
				}

				if(displaySpyglass) {
					Rectangle spyglassRectangle = null;
					GraphicsEnvironment localGE = GraphicsEnvironment.getLocalGraphicsEnvironment();
					for (GraphicsDevice gd : localGE.getScreenDevices()) {
						for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
							Rectangle rect = graphicsConfiguration.getBounds();
							Point point = MouseInfo.getPointerInfo().getLocation();
							if (rect.contains(point)) {
								if (point.x - spyglassBufferImage.getWidth() < rect.x) {
									spyglassRectangle = new Rectangle(cPointLive.x, cPointLive.y - spyglassBufferImage.getHeight(), cPointLive.x + spyglassBufferImage.getWidth(), cPointLive.y);
								} else {
									spyglassRectangle = new Rectangle(cPointLive.x - spyglassBufferImage.getWidth(), cPointLive.y - spyglassBufferImage.getHeight(), cPointLive.x, cPointLive.y);
								}

								if (point.y - spyglassBufferImage.getHeight() < rect.y) {
									spyglassRectangle = new Rectangle(spyglassRectangle.x, cPointLive.y, spyglassRectangle.width, cPointLive.y + spyglassBufferImage.getHeight());
								}
							}
						}
					}

					if(spyglassRectangle != null) {
						generateSpyglass(spyglassBufferImage);
						Shape oldClip = globalBuffer.getClip();
						Ellipse2D.Double shape = new Ellipse2D.Double(spyglassRectangle.x, spyglassRectangle.y, spyglassBufferImage.getWidth(), spyglassBufferImage.getHeight());
						globalBuffer.setClip(shape);
						globalBuffer.drawImage(spyglassBufferImage, spyglassRectangle.x, spyglassRectangle.y, this);
						globalBuffer.setClip(oldClip);
						allBounds.addRectangle(spyglassRectangle);
					}
				}
			}

			if(lastRect != null) {
				g.drawImage(globalBufferImage, lastRect.x, lastRect.y, lastRect.width, lastRect.height, lastRect.x, lastRect.y, lastRect.width, lastRect.height, this);
				lastRect = allBounds.getBounds();
			}
		} else {
			LogManager.log(sniperInstance.getID(), "WARNING: Screenshot is null when trying to render. Trying again.", Level.WARNING);
			repaint();
		}

		globalBuffer.dispose();
		selectBuffer.dispose();
		spyglassBuffer.dispose();
	}

	private void generateSpyglass(BufferedImage image) {
		final int ROWS = config.getInt(ConfigHelper.PROFILE.spyglassZoom);
		final int THICKNESS = config.getInt(ConfigHelper.PROFILE.spyglassThickness);

		if(config.getBool(ConfigHelper.PROFILE.spyglassPixelByPixel))
			generateSpyglassPixelByPixel(image, ROWS, THICKNESS);
		else
			generateSpyglassDirect(image, ROWS, THICKNESS);
	}

	private void generateSpyglassDirect(BufferedImage image, int rows, int thickness) {
		Graphics2D g = (Graphics2D) image.getGraphics();

		Point cPointLive = listener.getCurrentPoint(PointType.LIVE);

		g.drawImage(globalBufferImage, 0, 0, image.getWidth(), image.getHeight(), cPointLive.x - rows/2, cPointLive.y - rows/2, cPointLive.x + rows/2, cPointLive.y + rows/2, this);

		g.setColor(Color.BLACK);
		int space = image.getWidth() / rows;
		for(int i = 0; i < rows; i++) {
			g.drawLine(i * space, 0, i * space, image.getHeight());
			g.drawLine(0, i * space, image.getWidth(), i * space);
		}

		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(thickness));
		g.drawLine(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight());
		g.drawLine(0, image.getHeight()/2, image.getWidth(), image.getHeight()/2);
		g.setStroke(new BasicStroke(thickness*2));
		g.drawOval(0,0,image.getWidth(),image.getHeight());
		g.setStroke(oldStroke);
	}

	private void generateSpyglassPixelByPixel(BufferedImage image, int rows, int thickness) {
		Graphics2D g = (Graphics2D) image.getGraphics();

		final int ROW_SIZE = image.getWidth() / rows;

		Point cPointLive = listener.getCurrentPoint(PointType.LIVE);

		g.setRenderingHints(qualityHints);

		g.fillRect(0, 0, image.getWidth(), image.getHeight());

		for(int y = 0; y < rows; y++) {
			for(int x = 0; x < rows; x++) {
				Rectangle rect = new Rectangle(x * ROW_SIZE, y * ROW_SIZE, ROW_SIZE, ROW_SIZE);
				int pixelX = cPointLive.x + x - rows / 2;
				int pixelY = cPointLive.y + y - rows / 2;
				if(pixelX < globalBufferImage.getWidth() && pixelY < globalBufferImage.getHeight() && pixelX >= 0 && pixelY >= 0) {
					g.setColor(new Color(globalBufferImage.getRGB(pixelX, pixelY)));
					g.fillRect(rect.x, rect.y, rect.width, rect.height);
				}
				g.setColor(Color.BLACK);
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
			}
		}

		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(thickness));
		g.drawLine(image.getWidth()/2, 0, image.getWidth()/2, image.getHeight());
		g.drawLine(0, image.getHeight()/2, image.getWidth(), image.getHeight()/2);
		g.setStroke(new BasicStroke(thickness*2));
		g.drawOval(0,0,image.getWidth(),image.getHeight());
		g.setStroke(oldStroke);

		g.dispose();
	}

	public Sniper getSniperInstance() {
		return sniperInstance;
	}

	@Override
	public void windowActivated(WindowEvent windowEvent) { }

	@Override
	public void windowClosed(WindowEvent windowEvent) { }

	@Override
	public void windowClosing(WindowEvent windowEvent) {
		sniperInstance.killCaptureWindow();
	}

	@Override
	public void windowDeactivated(WindowEvent windowEvent) { }

	@Override
	public void windowDeiconified(WindowEvent windowEvent) { }

	@Override
	public void windowIconified(WindowEvent windowEvent) { }

	@Override
	public void windowOpened(WindowEvent windowEvent) { }
}

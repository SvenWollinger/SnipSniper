package io.wollinger.snipsniper.capturewindow;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JFrame;

import io.wollinger.snipsniper.systray.Sniper;
import io.wollinger.snipsniper.utils.DebugType;
import io.wollinger.snipsniper.utils.Icons;
import io.wollinger.snipsniper.utils.Utils;
import io.wollinger.snipsniper.editorwindow.EditorWindow;

public class CaptureWindow extends JFrame implements WindowListener{
	private static final long serialVersionUID = 3129624729137795417L;
	private final RenderingHints qualityHints;

	Sniper sniperInstance;
	CaptureWindow instance;
	CaptureWindowListener listener;

	Point startPoint;
	Point startPointTotal;
	Point cPoint;
	Point cPointAlt;
	Rectangle bounds = null;
	
	public BufferedImage screenshot = null;
	public BufferedImage screenshotTinted = null;
	
	boolean startedCapture = false;
	boolean finishedCapture = false;
	boolean imageSaved = false;
	public boolean isRunning = true;
	
	public Thread thread = null;
	
	public CaptureWindow(Sniper _sniperInstance) {
		qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		instance = this;
		sniperInstance = _sniperInstance;
		sniperInstance.trayIcon.setImage(Icons.alt_icons[_sniperInstance.profileID]);
		if(sniperInstance.cfg.getInt("snipeDelay") != 0) {
			try {
				Thread.sleep(sniperInstance.cfg.getInt("snipeDelay") * 1000L);
			} catch (InterruptedException e) {
				sniperInstance.debug("There was an error with the delay! Message: " + e.getMessage(), DebugType.ERROR);
				sniperInstance.debug("More info: " + Arrays.toString(e.getStackTrace()), DebugType.ERROR);
			}
		}
		
		screenshot();
		
		this.setUndecorated(true);		
		this.setIconImage(Icons.icon_taskbar);
		
		listener = new CaptureWindowListener(this);
		this.addWindowListener(this);
		this.addMouseListener(listener);
		this.addMouseMotionListener(listener);
		this.addKeyListener(listener);
		this.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				setSize();
			}

			@Override
			public void focusLost(FocusEvent e) {
				setSize();
			}
		});
     
	   loop(); 
	}
	
	
	public void loop() {
		thread = new Thread(() -> {
			final double nsPerTick = 1000000000D / sniperInstance.cfg.getInt("maxFPS");
			long lastTime = System.nanoTime();
			long lastTimer = System.currentTimeMillis();
			double delta = 0;
			boolean screenshotDone = false;

			while (isRunning) {
				if(screenshotDone) {
					setVisible(true);
					setSize();
					specialRepaint();
				}
				if(screenshot != null && screenshotTinted != null && !screenshotDone) screenshotDone = true;

				long now = System.nanoTime();
				delta += (now - lastTime) / nsPerTick;
				lastTime = now;

				while (delta >= 1) {
					delta -= 1;
					if(screenshotDone) specialRepaint();
				}

				if (System.currentTimeMillis() - lastTimer >= 1000)
					lastTimer += 1000;
			}
		});
		thread.start();
		
	}
	
	public void specialRepaint() {
		if(area != null) {
			final Rectangle rect = area;
			
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
			sniperInstance.debug("Couldn't take screenshot. Message: " + e.getMessage(), DebugType.ERROR);
			e.printStackTrace();
		}
		screenshotTinted = Utils.copyImage(screenshot);
		Graphics g2 = screenshotTinted.getGraphics();
		g2.setColor(new Color(100,100,100,100));
		g2.fillRect(0, 0, screenshotTinted.getTileWidth(), screenshotTinted.getHeight());
	    g2.dispose();
	}
	
	public void setSize() {
		this.setLocation((int)bounds.getX(),(int)bounds.getY());
		this.setSize(bounds.width, bounds.height);
		this.requestFocus();
		this.setAlwaysOnTop(true);	
		this.repaint();
	}
	
	Rectangle calcRectangle() {
		int minX = 0, maxX = 0, minY = 0, maxY = 0;
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
		if(!imageSaved) {
			BufferedImage finalImg;
			isRunning = false;
			this.dispose();
			finishedCapture = true;

			int borderSize = sniperInstance.cfg.getInt("borderSize");
			Rectangle captureArea = calcRectangle();

			if (captureArea.width == 0 || captureArea.height == 0) {
				sniperInstance.trayIcon.displayMessage("Error: Screenshot width or height is 0!", "ERROR", MessageType.ERROR);
				sniperInstance.killCaptureWindow();
				return;
			}

			BufferedImage croppedBuffer = screenshot.getSubimage(captureArea.x, captureArea.y, captureArea.width, captureArea.height);
			finalImg = new BufferedImage(croppedBuffer.getWidth() + borderSize *2, croppedBuffer.getHeight() + borderSize *2, BufferedImage.TYPE_INT_RGB);
			Graphics g = finalImg.getGraphics();
			g.setColor(sniperInstance.cfg.getColor("borderColor"));
			g.fillRect(0, 0, finalImg.getWidth(),finalImg.getHeight());
			g.drawImage(croppedBuffer, borderSize, borderSize, croppedBuffer.getWidth(), croppedBuffer.getHeight(), this);
			g.dispose();

			String finalLocation = null;
			boolean inClipboard = false;

			if(sniperInstance.cfg.getBool("saveToDisk")) {
				finalLocation = sniperInstance.saveImage(finalImg, "");
			}

			if(sniperInstance.cfg.getBool("copyToClipboard")) {
				sniperInstance.copyToClipboard(finalImg);
				inClipboard = true;
			}

			if(finalImg != null) {
				int posX = (int) cPointAlt.getX();
				int posY = (int) cPointAlt.getY();
				boolean leftToRight = false;

				if (!(startPointTotal.getX() > cPointAlt.getX())) {
					posX -= finalImg.getWidth();
					leftToRight = true;
				}
				if (!(startPointTotal.getY() > cPointAlt.getY())) {
					posY -= finalImg.getHeight();
					leftToRight = true;
				}
				if (sniperInstance.cfg.getBool("openEditor")) {
					sniperInstance.debug("Taking screenshot. Position info:", DebugType.INFO);
					sniperInstance.debug("Captured area: " + captureArea.toString(), DebugType.INFO);
					sniperInstance.debug("Area requested by JFrame.setLocation(): " + "X: " + cPointAlt.getX() + " Y: " + cPointAlt.getY(), DebugType.INFO);
					new EditorWindow(finalImg, posX, posY, "SnipSniper Editor", sniperInstance, leftToRight, finalLocation, inClipboard);
				}
			}

			sniperInstance.killCaptureWindow();
		}
	}
	
	public Rectangle area;
	Point lastPoint = null;
	boolean hasSaved = false;
	BufferedImage bufferImage;

	public void paint(Graphics g) {
		if(bounds != null) {
			bufferImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHints(qualityHints);

		Graphics2D gBuffer = (Graphics2D)bufferImage.getGraphics();
		gBuffer.setRenderingHints(qualityHints);

		if(screenshot != null && bufferImage != null) {
			if(screenshotTinted != null && !hasSaved && bounds != null) {
				g2.drawImage(screenshotTinted, 0,0, bounds.width, bounds.height, this);
				hasSaved = true;
			}

			if(area != null && startedCapture) {
				Graphics use = gBuffer;

				boolean directDraw = sniperInstance.cfg.getBool("directDraw");
				if(directDraw)
					use = g2;

				use.drawImage(screenshotTinted, area.x, area.y, area.width, area.height,area.x, area.y, area.width, area.height, this);
				use.drawImage(screenshot, startPoint.x, startPoint.y, cPoint.x, cPoint.y,startPoint.x, startPoint.y, cPoint.x, cPoint.y, this);

				if(!directDraw)
					g2.drawImage(bufferImage, area.x, area.y, area.width, area.height,area.x, area.y, area.width, area.height, this);
			}


			if(cPoint != null && startPoint != null)
				area = new Rectangle(startPoint.x, startPoint.y, cPoint.x, cPoint.y);
	
			lastPoint = cPoint;
		} else {
			sniperInstance.debug("WARNING: Screenshot is null when trying to render. Trying again.", DebugType.WARNING);
			this.repaint();
		}
		g2.dispose();
		gBuffer.dispose();
	}

	@Override
	public void windowActivated(WindowEvent arg0) { }

	@Override
	public void windowClosed(WindowEvent arg0) { }

	@Override
	public void windowClosing(WindowEvent arg0) {
		this.sniperInstance.killCaptureWindow();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) { }

	@Override
	public void windowDeiconified(WindowEvent arg0) { }

	@Override
	public void windowIconified(WindowEvent arg0) { }

	@Override
	public void windowOpened(WindowEvent arg0) { }
}

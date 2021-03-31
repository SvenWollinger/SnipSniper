package io.wollinger.snipsniper.editorwindow;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.*;

import io.wollinger.snipsniper.systray.Sniper;
import io.wollinger.snipsniper.utils.Icons;

public class EditorWindow extends JFrame{
	private static final long serialVersionUID = -7363672331227971815L;
	
	BufferedImage img;
	BufferedImage overdraw;
	Sniper sniperInstance;
	EditorWindowRender renderer;
	EditorListener listener;
	
	Color currentColor = new Color(255,255,0,150);
	Color censorColor = Color.BLACK;
	
	final int X_OFFSET = 8; // This is the offset for X, since the window moves too far to the right otherwise.



	public EditorWindow(BufferedImage _img, int _x, int _y, String _title, Sniper _sInstance, boolean _leftToRight) {
		img = _img;
		sniperInstance = _sInstance;
		overdraw = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);

		this.setTitle(_title);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setResizable(false);
		this.setIconImage(Icons.icon_taskbar);
		this.setVisible(true);
		int barSize = this.getHeight() - this.getInsets().bottom;

		listener = new EditorListener(this);
		
		renderer = new EditorWindowRender(this);
		
		renderer.addMouseListener(listener);
		renderer.addMouseMotionListener(listener);
		renderer.addMouseWheelListener(listener);
		this.addKeyListener(listener);
		
		this.add(renderer);
		this.pack();
		
		int borderSize = sniperInstance.cfg.getInt("borderSize");
		if(!_leftToRight) borderSize = -borderSize;
		
		this.setLocation((_x - X_OFFSET) + borderSize, (_y - barSize) + borderSize);

		/*JMenuBar menuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem save = new JMenuItem("Save");
		JMenuItem saveAs = new JMenuItem("Save as");
		file.add(save);
		file.add(saveAs);

		JMenu color = new JMenu("Color");
		/coming soon/
		menuBar.add(file);
		menuBar.add(color);
		this.setJMenuBar(menuBar);*/
	}
	
	public void saveImage() {
		BufferedImage finalImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = finalImg.getGraphics();
		g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), this);
		g.drawImage(overdraw, 0, 0, img.getWidth(), img.getHeight(), this);
		g.dispose();
		sniperInstance.saveImage(finalImg, "_edited");
		if(sniperInstance.cfg.getBool("copyToClipboard"))
			sniperInstance.copyToClipboard(finalImg);
	}
	
	public void kill() {
		img = null;
		this.dispose();
	}
	
}

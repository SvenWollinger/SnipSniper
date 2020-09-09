package snipsniper.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import snipsniper.Icons;
import snipsniper.Utils;
import snipsniper.systray.Sniper;

public class ConfigWindow extends JFrame implements WindowListener{

	private static final long serialVersionUID = -1627369445259468935L;
	
	HotKeyButton hotKeyButton = new HotKeyButton();
	JCheckBox savePictures = new JCheckBox();
	JCheckBox copyToClipboard = new JCheckBox();
	JTextField borderSize = new JTextField();
	JTextField pictureLocation = new JTextField();
	Color borderColor = Color.black;
	JTextField snipeDelay = new JTextField();
	JCheckBox openEditor = new JCheckBox();
	
	int maxBorder = 999;
	
	ConfigWindow instance = this;
	ColorWindow cWnd = null;
	Sniper sniperInstance;
	
	public JLabel createJLabel(String _title, int _horizontalAlignment, int _verticalAlignment) {
		JLabel jlabel = new JLabel(_title);
		jlabel.setHorizontalAlignment(_horizontalAlignment);
		jlabel.setVerticalAlignment(_verticalAlignment);
		return jlabel;
	}
	
	public ConfigWindow(Sniper _sniperInstance) {
		this.addWindowListener(this);
		this.setSize(512,512);
		this.setTitle("Config");
		this.setIconImage(Icons.icon_taskbar);
		
		sniperInstance = _sniperInstance;
		
		hotKeyButton.setText(NativeKeyEvent.getKeyText(_sniperInstance.cfg.hotkey));
		hotKeyButton.hotkey = sniperInstance.cfg.hotkey;
		savePictures.setSelected(_sniperInstance.cfg.savePictures);
		copyToClipboard.setSelected(_sniperInstance.cfg.copyToClipboard);
		borderSize.setText(_sniperInstance.cfg.borderSize + "");
		pictureLocation.setText(_sniperInstance.cfg.pictureFolder + "");
		borderColor = _sniperInstance.cfg.borderColor;
		snipeDelay.setText(_sniperInstance.cfg.snipeDelay + "");
		openEditor.setSelected(_sniperInstance.cfg.openEditor);
		
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ConfigSaveButton(this));
		
		JPanel options = new JPanel(new GridLayout(0,1));

		JPanel row0 = new JPanel(new GridLayout(0,2));
		row0.add(createJLabel("Capture Hotkey", JLabel.CENTER, JLabel.CENTER));
		row0.add(hotKeyButton);
		options.add(row0);
		
		JPanel row1 = new JPanel(new GridLayout(0,2));
		row1.add(createJLabel("Save Images on Capture", JLabel.CENTER, JLabel.CENTER));
		row1.add(savePictures);
		options.add(row1);
		
		JPanel row2 = new JPanel(new GridLayout(0,2));
		row2.add(createJLabel("Copy images to clipboard on capture", JLabel.CENTER, JLabel.CENTER));
		row2.add(copyToClipboard);
		options.add(row2);
		
		JPanel row3 = new JPanel(new GridLayout(0,2));
		row3.add(createJLabel("Border size (in px)", JLabel.CENTER, JLabel.CENTER));
		JPanel row3_2 = new JPanel(new GridLayout(0,2));
		row3_2.add(borderSize);
		JButton colorBtn = new JButton("Color");
		colorBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(cWnd == null)
					cWnd = new ColorWindow(instance);
				else
					cWnd.requestFocus();
			}
		});
		row3_2.add(colorBtn);
		row3.add(row3_2);
		options.add(row3);
		
		JPanel row4 = new JPanel(new GridLayout(0,2));
		row4.add(createJLabel("Picture Location", JLabel.CENTER, JLabel.CENTER));
		row4.add(pictureLocation);
		options.add(row4);

		JPanel row5 = new JPanel(new GridLayout(0,2));
		row5.add(createJLabel("Snap Delay in s:", JLabel.CENTER, JLabel.CENTER));
		JPanel row5_2 = new JPanel(new GridLayout(0,2));
		row5_2.add(snipeDelay);
		row5.add(row5_2);
		options.add(row5);
		
		JPanel row6 = new JPanel(new GridLayout(0,2));
		row6.add(createJLabel("Open Editor after Capture", JLabel.CENTER, JLabel.CENTER));
		row6.add(openEditor);
		options.add(row6);
		
		JPanel row7 = new JPanel(new GridLayout(0,5));
		row7.add(new JPanel());
		row7.add(new JPanel());
		row7.add(saveButton);
		options.add(row7);
		
		this.setMinimumSize(new Dimension(512,256));
		this.add(options);
		this.pack();
		this.setVisible(true);
		int w = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
		int h = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
		this.setLocation((w/2) - this.getWidth()/2, (h/2) - this.getHeight()/2);
	}
	
	void msgError(String _msg) {
		JOptionPane.showMessageDialog(null, _msg,"Error",1);
	}
	
	public void save() {
		//TODO: Rework sanitazion
		boolean _savePictures = savePictures.isSelected();
		boolean _copyToClipboard = copyToClipboard.isSelected();
		boolean _openEditor = openEditor.isSelected();
		
		String _saveLocation = pictureLocation.getText();
		int _borderSize = 0;
		int _snipeDelay = 0;
		if(Utils.isInteger(borderSize.getText())) {
			_borderSize = Integer.parseInt(borderSize.getText());
		} else {
			msgError("Border size must be a number between 0-999!");
			return;
		}
		
		if(Utils.isInteger(snipeDelay.getText())) {
			_snipeDelay = Integer.parseInt(snipeDelay.getText());
			if(_snipeDelay < 0) {
				msgError("Snipe delay can not be less then 0!");
				return;
			}
		}
		
		//ERROR CHECKING
		if(_borderSize < 0) {
			msgError("Border size can not be less then 0!");
			return;
		} else if (_borderSize > maxBorder) {
			msgError("Border size can not be more then 999!");
			return;
		}
		File saveLocationCheck = new File(_saveLocation);
		if(!saveLocationCheck.exists()) {
			Object[] options = {"Okay" , "Create Directory" };
			int msgBox = JOptionPane.showOptionDialog(null,"Directory does not exist!", "Error", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if(msgBox == 1) {
				File f = new File(_saveLocation);
				if(!f.mkdirs()) {
					msgError("Directory could not be created!");
				}
			}
			return;
		}
		
		sniperInstance.cfg.hotkey = hotKeyButton.hotkey;
		sniperInstance.cfg.pictureFolder = _saveLocation;
		sniperInstance.cfg.savePictures = _savePictures;
		sniperInstance.cfg.borderSize = _borderSize;
		sniperInstance.cfg.copyToClipboard = _copyToClipboard;
		sniperInstance.cfg.borderColor = borderColor;
		sniperInstance.cfg.snipeDelay = _snipeDelay;
		sniperInstance.cfg.openEditor = _openEditor;
		sniperInstance.cfg.saveFile();
		
		close();
	}
	
	private boolean closed = false;
	void close() {
		if(!closed) {
			GlobalScreen.removeNativeKeyListener(hotKeyButton);
			closed = true;
			if(cWnd != null) {
				cWnd.dispose();
				cWnd = null;
			}
			this.dispose();
			sniperInstance.cfgWnd = null;
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) { }

	@Override
	public void windowClosed(WindowEvent arg0) {
		close();
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		close();
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
package io.wollinger.snipsniper.configwindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConfigSaveButton implements ActionListener{
	ConfigWindow cfgWnd;
	
	public ConfigSaveButton(ConfigWindow cfgWnd) {
		this.cfgWnd = cfgWnd;
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		cfgWnd.save();
	}
	
}

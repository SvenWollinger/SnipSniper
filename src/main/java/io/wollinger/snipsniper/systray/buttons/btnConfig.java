package io.wollinger.snipsniper.systray.buttons;

import java.awt.MenuItem;

import io.wollinger.snipsniper.configwindow.ConfigWindow;
import io.wollinger.snipsniper.systray.Sniper;
import io.wollinger.snipsniper.utils.LangManager;

public class btnConfig extends MenuItem{

	public btnConfig(Sniper sniperInstance) {
		setLabel(LangManager.getItem("menu_config"));
		addActionListener(listener -> sniperInstance.openConfigWindow());
		
	}
}

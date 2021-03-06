package io.wollinger.snipsniper.systray.buttons;

import java.awt.Desktop;
import java.awt.MenuItem;
import java.io.File;
import java.io.IOException;

import io.wollinger.snipsniper.systray.Sniper;
import io.wollinger.snipsniper.utils.ConfigHelper;
import io.wollinger.snipsniper.utils.LangManager;

public class btnOpenImgFolder extends MenuItem{

	public btnOpenImgFolder(Sniper sniperInstance) {
		setLabel(LangManager.getItem("menu_open_image_folder"));
		addActionListener(listener -> {
			try {
				Desktop.getDesktop().open(new File(sniperInstance.getConfig().getString(ConfigHelper.PROFILE.pictureFolder)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
	}
	
}

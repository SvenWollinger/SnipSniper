package snipsniper;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import snipsniper.systray.Sniper;

public class Config {

	private HashMap <String, String> settings = new HashMap<String, String>();
	private HashMap <String, String> defaults = new HashMap<String, String>();
	
	Sniper sniperInstance;
	
	public Config (Sniper _sniperInstance) {
		sniperInstance = _sniperInstance;
		
		String filename = getFilename(sniperInstance.profileID);
		try {
			if(new File(Main.profilesFolder + filename).exists())
				loadFile(Main.profilesFolder + filename, settings, false);
			
			loadFile("/defaults.txt", defaults, true);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	void loadFile(String filename, HashMap<String, String> _map, boolean inJar) throws IOException, NumberFormatException {
			BufferedReader reader = null;
			if(!inJar)
				reader = new BufferedReader(new FileReader(filename));
			else if(inJar)
				reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			
			String line = reader.readLine();
			
			while (line != null) {
				if(line.contains("=")) {
					String[] args = line.split("=");
					_map.put(args[0], args[1]);
				}
				line = reader.readLine();
			}
			reader.close();

	}
	
	public String getString(String _key) {
		if(settings.containsKey(_key))
			return settings.get(_key);
		else if(defaults.containsKey(_key))
			return defaults.get(_key);
		else
			sniperInstance.debug("No value found for <" + _key + ">.", DebugType.ERROR);
		return null;
	}
	
	public int getInt(String _key) {
		if(getString(_key) != null)
			return Integer.parseInt(getString(_key));
		return -1;
	}
	
	public boolean getBool(String _key) {
		if(getString(_key) != null)
			return Boolean.parseBoolean(getString(_key));
		return false;
	}
	
	public Color getColor(String _key) {
		if(getString(_key) != null)
			return Utils.hex2rgb(getString(_key));
		return null;
	}
	
	public void set(String _key, String _value) {
		if(!settings.containsKey(_key))
			settings.put(_key, _value);
		else
			settings.replace(_key, _value);
	}
	
	String getFilename(int _profileID) {
		String filename = "";
		if(_profileID == 0)
			filename = "default.txt";
		else
			filename = "profile" + _profileID + ".txt";
		return filename;
	}
	
	public void deleteFile() {
		String filename = getFilename(sniperInstance.profileID);
		File file = new File(Main.profilesFolder + "\\" + filename);
		file.delete();
	}
	
	private void saveFile(HashMap<String, String> _map) {
		String filename = getFilename(sniperInstance.profileID);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Main.profilesFolder + "\\" + filename));
			for (String key : _map.keySet()) {
				String value = _map.get(key);
				writer.write(key + "=" + value + "\n");
			}
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void save() {
		if(settings.isEmpty())
			saveFile(defaults);
		else
			saveFile(settings);
	}
	
}
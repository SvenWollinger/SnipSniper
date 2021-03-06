package io.wollinger.snipsniper;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;

import io.wollinger.snipsniper.utils.LogManager;
import io.wollinger.snipsniper.utils.Utils;

public class Config {
	private final HashMap <String, String> settings = new HashMap<>();
	private final HashMap <String, String> defaults = new HashMap<>();

	private String id;
	private String filename;

	public static final String EXTENSION = "cfg";
	public static final String DOT_EXTENSION = ".cfg";

	public Config (Config config) {
		//Copies config
		LogManager.log(config.id, "Copying config for <" + config.filename + ">", Level.INFO);
		loadFromConfig(config);
	}

	public void loadFromConfig(Config config) {
		this.filename = config.filename;
		this.id = config.id;

		settings.putAll(copyMap(config.settings));
		defaults.putAll(copyMap(config.defaults));
	}

	public Config (String filename, String id, String defaultFile) {
		this.filename = filename;
		this.id = id;
		LogManager.log(id, "Creating config object for <" + filename + ">.", Level.INFO);
		try {
			if(new File(SnipSniper.getProfilesFolder() + filename).exists())
				loadFile(SnipSniper.getProfilesFolder() + filename, settings, false);
			
			loadFile("/cfg/" + defaultFile, defaults, true);
		} catch (NumberFormatException | IOException e) {
			LogManager.log(id, "There was an error loading the config. Message: " + e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
	}

	private HashMap<String, String> copyMap(HashMap<String, String> mapToCopy) {
		HashMap<String, String> newMap = new HashMap<>();
		for (String key : mapToCopy.keySet()) {
			newMap.put(key, mapToCopy.get(key));
		}
		return newMap;
	}
	
	void loadFile(String filename, HashMap<String, String> map, boolean inJar) throws IOException, NumberFormatException {
			BufferedReader reader;
			if(!inJar)
				reader = new BufferedReader(new FileReader(filename));
			else
				reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			
			String line = reader.readLine();
			
			while (line != null) {
				if(line.contains("=")) {
					String[] args = line.split("=");
					map.put(args[0], args[1]);
				}
				line = reader.readLine();
			}
			reader.close();

	}

	public String getRawString(Enum key) {
		return getRawString(key.toString());
	}

	public String getRawString(String key) {
		String returnVal = null;
		if(settings.containsKey(key))
			returnVal = settings.get(key);
		else if(defaults.containsKey(key))
			returnVal = defaults.get(key);
		else
			LogManager.log(id, "No value found for <" + key + "> in Config <" + SnipSniper.getProfilesFolder() + filename + ">.", Level.SEVERE);
		return returnVal;
	}

	public String getString(Enum key) {
		return getString(key.toString());
	}

	public String getString(String key) {
		String str = getRawString(key);
		if(str != null) {
			str = Utils.replaceVars(str);
		}
		return str;
	}

	public int getInt(Enum key) {
		return getInt(key.toString());
	}

	public int getInt(String key) {
		if(getString(key) != null) {
			String value = getString(key);
			if(Utils.isDouble(value))
				return (int) Double.parseDouble(value);
			else Integer.parseInt(getString(key));
		}
		return -1;
	}

	public float getFloat(Enum key) {
		return getFloat(key.toString());
	}

	public float getFloat(String key) {
		if(getString(key) != null)
			return Float.parseFloat(getString(key));
		return -1F;
	}

	public boolean getBool(Enum key) {
		return getBool(key.toString());
	}

	public boolean getBool(String key) {
		if(getString(key) != null)
			return Boolean.parseBoolean(getString(key));
		return false;
	}

	public Color getColor(Enum key) {
		return getColor(key.toString());
	}

	public Color getColor(String key) {
		if(getString(key) != null)
			return Utils.hex2rgb(getString(key));
		return null;
	}

	public void set(Enum key, int value) {
		set(key.toString(), value + "");
	}

	public void set(Enum key, boolean value) {
		set(key.toString(), value + "");
	}

	public void set(Enum key, String value) {
		set(key.toString(), value);
	}

	public void set(String key, String value) {
		if(!settings.containsKey(key))
			settings.put(key, value);
		else
			settings.replace(key, value);
	}
	
	public void deleteFile() {
		File file = new File(SnipSniper.getProfilesFolder() + "/" + filename);
		if(!file.delete())
			LogManager.log(id, "Could not delete profile config!", Level.WARNING);
	}
	
	private void saveFile(HashMap<String, String> map) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(SnipSniper.getProfilesFolder() + "/" + filename));
			for (String key : map.keySet()) {
				String value = map.get(key);
				writer.write(key + "=" + value + "\n");
			}
			writer.close();
		} catch (IOException e) {
			LogManager.log(id, "There was an error saving the config! Message: " + e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
	}
	
	public void save() {
		if(!SnipSniper.isDemo()) {
			if (settings.isEmpty())
				saveFile(defaults);
			else
				saveFile(settings);
		}
	}

	public String getFilename() {
		return filename;
	}
	
}

package resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public final class Fonts {
	
	public static Font DEFAULT_FONT;
	
	protected static ArrayList<Font> fonts = new ArrayList<Font>(1);
	
	public static void loadAll(File directory) {
		File[] files = directory.listFiles();
		
		for(File f : files) {
			if (f.isDirectory()) {
				try {
					Font font = new Font(f);
					fonts.add(font);
					
					if(font.getName().equals("Default Font")) {
						DEFAULT_FONT = font;
					}
					
				} catch(IOException e) {
					continue;
				}
			}
		}
	}
	
	public static ArrayList<Font> getFonts() {
		return fonts;
	}
	
	public static void deleteAll() {
		DEFAULT_FONT.delete();
	}
}

package ritzow.sandbox.client.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Fonts {
	private static Font DEFAULT_FONT;
	private static List<Font> fonts = new ArrayList<>(1);
	private static List<Font> fontsImmutable = Collections.unmodifiableList(fonts);
	
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
	
	public static List<Font> getFonts() {
		return fontsImmutable;
	}
	
	public static Font getDefaultFont() {
		return DEFAULT_FONT;
	}
	
	public static void deleteAll() {
		DEFAULT_FONT.delete();
	}
}

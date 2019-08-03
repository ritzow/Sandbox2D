package ritzow.sandbox.client.data;

import java.nio.file.Path;

public class StandardClientProperties {
	public static final Path RESOURCES_PATH = Path.of("resources");
	public static final Path ASSETS_PATH = RESOURCES_PATH.resolve("assets");
	public static final Path TEXTURES_PATH = ASSETS_PATH.resolve("textures");
	public static final Path CURSORS_PATH = TEXTURES_PATH.resolve("cursors");
	public static final Path AUDIO_PATH = ASSETS_PATH.resolve("audio");
	public static final Path SHADERS_PATH = RESOURCES_PATH.resolve("shaders");
	
	public static final Path OPTIONS_PATH = Path.of("options.txt");
}

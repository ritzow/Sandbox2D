package ritzow.sandbox.client;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import ritzow.sandbox.client.graphics.TextureAtlas;
import ritzow.sandbox.client.graphics.TextureData;

public class TestAtlas {
	public static void main(String[] args) throws IOException {
		List<TextureData> textures = Files.list(Paths.get("resources/assets/textures"))
				.filter(f -> !Files.isDirectory(f)).map(f -> {
					try(InputStream s = Files.newInputStream(f)) {
						PNGDecoder decoder = new PNGDecoder(s);
						decoder.decideTextureFormat(Format.RGBA);
						ByteBuffer pixels = ByteBuffer.wrap(new byte[decoder.getWidth() * decoder.getHeight() * 4]);
						decoder.decodeFlipped(pixels, decoder.getWidth() * 4, Format.RGBA);
						pixels.flip();
						return new TextureData(decoder.getWidth(), decoder.getWidth()/4, pixels.array());
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}).collect(Collectors.toList());
		
		System.out.println(textures);
		TextureAtlas atlas = new TextureAtlas(textures.toArray(new TextureData[textures.size()]));
		System.out.println(atlas);
	}
}

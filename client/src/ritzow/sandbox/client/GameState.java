package ritzow.sandbox.client;

import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.LightRenderProgram;
import ritzow.sandbox.client.graphics.ModelRenderer;
import ritzow.sandbox.client.ui.StandardGuiRenderer;

class GameState {
	private GameState() {}

	private static ModelRenderer modelRenderer;
	private static Display display;
	private static long cursorPick, cursorMallet;
	private static MainMenuContext menuContext;
	private static StandardGuiRenderer guiRenderer;
	private static LightRenderProgram lightRenderer;

	static Display display() {
		return display;
	}

	static ModelRenderer modelRenderer() {
		return modelRenderer;
	}

	static void modelRenderer(ModelRenderer program) {
		GameState.modelRenderer = program;
	}

	static void setDisplay(Display display) {
		GameState.display = display;
	}

	static long cursorPick() {
		return cursorPick;
	}

	static void setCursorPick(long cursorPick) {
		GameState.cursorPick = cursorPick;
	}

	static long cursorMallet() {
		return cursorMallet;
	}

	static void setCursorMallet(long cursorMallet) {
		GameState.cursorMallet = cursorMallet;
	}

	static MainMenuContext menuContext() {
		return menuContext;
	}

	static void setMenuContext(MainMenuContext menuContext) {
		GameState.menuContext = menuContext;
	}

	static StandardGuiRenderer guiRenderer() {
		return guiRenderer;
	}

	static void setGuiRenderer(StandardGuiRenderer guiRenderer) {
		GameState.guiRenderer = guiRenderer;
	}

	static void setLightRenderer(LightRenderProgram lightRenderer) {
		GameState.lightRenderer = lightRenderer;
	}

	static LightRenderProgram lightRenderer() {
		return lightRenderer;
	}
}

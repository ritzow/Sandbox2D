package ritzow.sandbox.client;

import ritzow.sandbox.client.graphics.Display;
import ritzow.sandbox.client.graphics.ModelRenderProgram;

class GameState {
	private GameState() {}

	private static ModelRenderProgram shader;
	private static Display display;
	private static long cursorPick, cursorMallet;
	private static MainMenuContext menuContext;

	static Display display() {
		return display;
	}

	static ModelRenderProgram shader() {
		return shader;
	}

	static void setShader(ModelRenderProgram shader) {
		GameState.shader = shader;
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
}

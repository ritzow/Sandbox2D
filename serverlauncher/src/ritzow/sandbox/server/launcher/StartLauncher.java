package ritzow.sandbox.server.launcher;

import javafx.application.Platform;

public final class StartLauncher {
	public static void main(String[] args) {
		Platform.startup(LauncherController::new);
	}
}

module ritzow.sandbox.server.launcher {
	requires java.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	requires ritzow.sandbox.server;
	opens ritzow.sandbox.server.launcher to javafx.fxml;
	exports ritzow.sandbox.server.launcher;
}
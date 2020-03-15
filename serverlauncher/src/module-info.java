module ritzow.sandbox.server.launcher {
	requires javafx.controls;
	requires javafx.fxml;
	requires commons.validator;
	requires ritzow.sandbox.server;
	requires ritzow.sandbox.shared;
	opens ritzow.sandbox.server.launcher to javafx.fxml;
}
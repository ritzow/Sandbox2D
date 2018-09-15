package ritzow.sandbox.server.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class StartLauncher extends Application {
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		BorderPane root = FXMLLoader.load(Path.of("bin/launcher_main.fxml").toUri().toURL());
		Button startButton = (Button) root.lookup("#startButton");
		startButton.addEventHandler(ActionEvent.ACTION, this::onStartButtonPress);
		Rectangle2D screenSize = Screen.getPrimary().getBounds();
		stage.setWidth(screenSize.getWidth()/2);
		stage.setHeight(screenSize.getHeight()/2);
		try(var iconIn = Files.newInputStream(Path.of("bin/greenFace.png"))) {
			stage.getIcons().add(new Image(iconIn));
		}
		stage.setScene(new Scene(root));
		stage.setTitle("Sandbox2D Dedicated Server");
		stage.show();
	}

	private void onStartButtonPress(ActionEvent event) {
		System.out.println("start button pressed");
		((Button)event.getSource()).setDisable(true);
	}
}

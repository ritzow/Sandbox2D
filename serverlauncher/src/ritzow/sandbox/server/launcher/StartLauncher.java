package ritzow.sandbox.server.launcher;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import ritzow.sandbox.server.StartServer;

public final class StartLauncher {
	private static Stage stage;
	private static Control root;
	private static LauncherController controller;

	public static void main(String[] args) {
		Platform.startup(StartLauncher::start);
	}

	private static final class LauncherController {
		@FXML TextField ipField, portField;
		@FXML Button startButton, browseButton;
		@FXML Text errorMessageText;
	}

	private static void start() {
		try {
//			try(var in = Files.newInputStream(Path.of("bin/font/OpenSans-Bold.ttf"))) {
//				Font.loadFont(in, 18);
//			}

			//Font.loadFont("https://fonts.googleapis.com/css?family=Open+Sans:700", 18);
			
			var loader = new FXMLLoader();
			controller = new LauncherController();
			loader.setController(controller);
			try(var in = Files.newInputStream(Path.of("bin/launcher_main.fxml"))) {
				root = loader.load(in);
			}

			root.setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, CornerRadii.EMPTY, Insets.EMPTY)));
			//controller.startButton.setBackground(new Background(new BackgroundFill(Color.GREEN, new CornerRadii(5), Insets.EMPTY)));
			controller.startButton.addEventHandler(ActionEvent.ACTION, StartLauncher::onStartButtonPress);
			controller.browseButton.addEventHandler(ActionEvent.ACTION, StartLauncher::onBrowseButtonPress);

			stage = new Stage(StageStyle.DECORATED);

			try(var iconIn = Files.newInputStream(Path.of("bin/greenFace.png"))) {
				stage.getIcons().add(new Image(iconIn));
			}

			Rectangle2D screenSize = Screen.getPrimary().getBounds();
			stage.setWidth(screenSize.getWidth()/2);
			stage.setHeight(screenSize.getHeight()/2);
			stage.setScene(new Scene(root));
			stage.setTitle("Sandbox2D Server");
			stage.setOnCloseRequest(event -> {
				if(event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {

				}
			});
			stage.show();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private static void runServer(InetAddress ip, int port) {
		try {
			StartServer.run(new InetSocketAddress(ip, port));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void onStartButtonPress(ActionEvent event) {
		try {
			System.out.println("start button pressed");
			((Button)event.getSource()).setDisable(true);
			InetAddress ip = InetAddress.getByName(controller.ipField.getText().isEmpty() ? controller.ipField.getPromptText() : controller.ipField.getText());
			int port = Integer.parseUnsignedInt(controller.portField.getText().isEmpty() ? controller.portField.getPromptText() : controller.portField.getText());
			new Thread(() -> runServer(ip, port), "Server Update Thread").start();
		} catch(UnknownHostException e) {
			controller.errorMessageText.setText("Invalid IP address");
			//controller.ipField.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));
		} catch(NumberFormatException e) {
			controller.errorMessageText.setText("Invalid port number");
		}
	}

	private static void onBrowseButtonPress(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select World File");
		chooser.setInitialDirectory(Path.of(".").toFile());
		chooser.getExtensionFilters().add(new ExtensionFilter("World Files", List.of("*.dat","*.world")));
		File worldFile = chooser.showOpenDialog(stage);
		if(worldFile != null) {
			((TextField)root.lookup("#worldFileField")).setText(worldFile.getAbsolutePath());
		}
	}
}

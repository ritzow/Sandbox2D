package ritzow.sandbox.server.launcher;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import ritzow.sandbox.network.NetworkUtility;
import ritzow.sandbox.network.Protocol;
import ritzow.sandbox.server.SerializationProvider;
import ritzow.sandbox.server.network.Server;
import ritzow.sandbox.util.Utility;

class LauncherController {
	@FXML TextField ipField, portField, worldFileField;
	@FXML Button startButton, browseButton;
	@FXML Text errorMessageText;

	private final Stage stage;

	public LauncherController() {
		try(var in = Files.newInputStream(Path.of("bin/launcher_main.fxml"));
			var iconIn = Files.newInputStream(Path.of("bin/greenFace.png"))) {
			var loader = new FXMLLoader();
			loader.setController(this);
			BorderPane root = loader.load(in);
			root.getStylesheets().add("style.css");
			stage = new Stage();
			stage.getIcons().add(new Image(iconIn));
			ipField.setPromptText(NetworkUtility.getPrimaryAddress().getHostAddress());
			ipField.textProperty().addListener(this::onIpFieldChange);
			portField.setPromptText(Integer.toString(Protocol.DEFAULT_SERVER_PORT));
			portField.textProperty().addListener(this::onPortFieldChange);
			portField.setTextFormatter(new TextFormatter<Object>(LauncherController::filterPortText));
			worldFileField.textProperty().addListener(this::onFileFieldChange);
			startButton.setOnAction(this::onStartButtonPress);
			browseButton.setOnAction(this::onBrowseButtonPress);
			Rectangle2D screenSize = Screen.getPrimary().getBounds();
			stage.setWidth(screenSize.getWidth()/2);
			stage.setHeight(screenSize.getHeight()/2);
			stage.setScene(new Scene(root));
			stage.setTitle("Sandbox2D Server");
			stage.show();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void runServer(Server server) {
		try {
			long frameTime = Utility.frameRateToFrameTimeNanos(60);
			System.out.println("Started server on " + server.getAddress());
			while(server.isOpen()) {
				long start = System.nanoTime();
				server.update();
				Utility.limitFramerate(start, frameTime);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private static final Pattern NOT_DIGIT_MATCHER = Pattern.compile("\\D+");

	private static Change filterPortText(Change change) {
		if(NOT_DIGIT_MATCHER.matcher(change.getText()).find()
			|| change.getControlNewText().length() > 5)
			change.setText("");
		return change;
	}

	@SuppressWarnings("TypeParameterExtendsFinalClass")
	private void onPortFieldChange(ObservableValue<? extends String> value,
			String oldText, String newText) {
		errorMessageText.setText("");
		if(newText.isBlank()) {
			setBorder(ValidState.NEUTRAL, portField);
		} else {
			try {
				int port = Integer.parseUnsignedInt(newText);
				if(IntegerValidator.getInstance().isInRange(port, 0, Character.MAX_VALUE)) {
					setBorder(ValidState.VALID, portField);
					startButton.setDisable(false);
				} else {
					setBorder(ValidState.INVALID, portField);
					startButton.setDisable(true);
				}
			} catch(NumberFormatException e) {
				setBorder(ValidState.INVALID, portField);
				startButton.setDisable(true);
			}
		}
	}

	@SuppressWarnings("TypeParameterExtendsFinalClass")
	private void onFileFieldChange(ObservableValue<? extends String> value,
			 String oldText, String newText) {
		errorMessageText.setText("");
		if(newText.isBlank()) {
			setBorder(ValidState.NEUTRAL, worldFileField);
			startButton.setDisable(false);
		} else if(Files.isRegularFile(Path.of(newText))) {
			setBorder(ValidState.VALID, worldFileField);
			startButton.setDisable(false);
		} else {
			setBorder(ValidState.INVALID, worldFileField);
			startButton.setDisable(true);
		}
	}

	@SuppressWarnings("TypeParameterExtendsFinalClass")
	private void onIpFieldChange(ObservableValue<? extends String> value,
								 String oldText, String newText) {
		errorMessageText.setText("");
		if(newText.isBlank()) {
			setBorder(ValidState.NEUTRAL, ipField);
			startButton.setDisable(false);
		} else if(InetAddressValidator.getInstance().isValid(newText)
			|| DomainValidator.getInstance().isValid(newText)
			|| newText.equalsIgnoreCase("localhost")) {
			setBorder(ValidState.VALID, ipField);
			startButton.setDisable(false);
		} else {
			setBorder(ValidState.INVALID, ipField);
			startButton.setDisable(true);
		}
	}

	private enum ValidState {
		INVALID,
		VALID,
		NEUTRAL
	}

	private static void setBorder(ValidState state, Region node) {
		switch(state) {
			case INVALID -> node.setBorder(new Border(new BorderStroke(Color.RED,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
			case VALID -> node.setBorder(new Border(new BorderStroke(Color.GREEN,
				BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
			case NEUTRAL -> node.setStyle(null);
		}
	}

	private void onStartButtonPress(ActionEvent event) {
		try {
			InetAddress ip = InetAddress.getByName(ipField.getText().isEmpty() ?
				ipField.getPromptText() : ipField.getText());
			int port = Integer.parseUnsignedInt(portField.getText().isEmpty() ?
				portField.getPromptText() : portField.getText());

			if(!IntegerValidator.getInstance().isInRange(port, 0, Character.MAX_VALUE)) {
				throw new NumberFormatException("port not in proper range");
			}
			startButton.setDisable(true);
			Server server = Server.start(new InetSocketAddress(ip, port));
			server.setCurrentWorld(SerializationProvider
				.getProvider()
				.deserialize(Utility.loadCompressedFile(Path.of(worldFileField.getText().isEmpty() ?
					worldFileField.getPromptText() : worldFileField.getText()))));
			stage.setOnCloseRequest(windowEvent -> {
				System.out.println("Shutting down server");
				server.startShutdown(); //TODO this needs to happen in server update thread.
			});
			new Thread(() -> runServer(server), "Server Thread").start();
		} catch(UnknownHostException e) {
			errorMessageText.setText("Invalid IP address");
			setBorder(ValidState.INVALID, ipField);
		} catch(NumberFormatException e) {
			errorMessageText.setText("Invalid port number");
			setBorder(ValidState.INVALID, portField);
		} catch(BindException e) {
			errorMessageText.setText("Couldn't bind to provided address");
		} catch(IOException e) {
			errorMessageText.setText("Uknown error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void onBrowseButtonPress(@SuppressWarnings("unused") ActionEvent event) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select World File");
		chooser.setInitialDirectory(Path.of(".").toFile());
		chooser.getExtensionFilters().add(
			new FileChooser.ExtensionFilter("World Files", List.of("*.dat","*.world")));
		File worldFile = chooser.showOpenDialog(stage);
		if(worldFile != null) {
			worldFileField.setText(worldFile.getAbsolutePath());
		}
	}
}

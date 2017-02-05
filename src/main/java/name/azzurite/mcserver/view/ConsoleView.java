package name.azzurite.mcserver.view;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.console.TextAreaWriter;

public class ConsoleView implements NodeRepresentation {

	private final LocalConsole console;

	@FXML
	private GridPane root;
	@FXML
	private TextArea consoleTextArea;

	@FXML
	private TextField input;

	public ConsoleView(LocalConsole console) {
		this.console = console;
	}

	@Override
	public Node toNodeRepresentation() {
		return root;
	}

	@FXML
	private void initialize() {
		console.addOutRedirect(new TextAreaWriter(consoleTextArea));

		input.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				String text = input.getText();
				input.clear();
				console.send(text);
			}
		});
	}
}

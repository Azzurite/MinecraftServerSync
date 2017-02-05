package name.azzurite.mcserver.console;

import java.io.IOException;
import java.io.Writer;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class TextAreaWriter extends Writer {

	private final TextArea textArea;

	public TextAreaWriter(TextArea textArea) {
		this.textArea = textArea;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		Platform.runLater(createTextAreaAppendRunnable(cbuf, off, len));
	}

	private Runnable createTextAreaAppendRunnable(char[] cbuf, int off, int len) {
		String toAppend = new String(cbuf, off, len);
		return () -> textArea.appendText(toAppend);
	}

	@Override
	public void flush() throws IOException {
		// nothing to do
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}
}

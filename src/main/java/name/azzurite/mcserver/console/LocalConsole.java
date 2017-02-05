package name.azzurite.mcserver.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"UseOfSystemOutOrSystemErr", "ImplicitDefaultCharsetUsage"})
public final class LocalConsole {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalConsole.class);

	private final PrintStream systemOut;

	private final List<PrintWriter> inRedirects = new ArrayList<>();

	private List<PrintWriter> outRedirects = new ArrayList<>();

	public LocalConsole() throws IOException {
		systemOut = System.out;
		redirectOut();

		addInRedirect(redirectIn());
	}

	private static OutputStream redirectIn() throws IOException {
		PipedInputStream pipedInputStream = new PipedInputStream();
		System.setIn(pipedInputStream);

		PipedOutputStream pipedOutputStream = new PipedOutputStream();
		pipedInputStream.connect(pipedOutputStream);

		return pipedOutputStream;
	}

	public void addInRedirect(OutputStream out) {
		inRedirects.add(new PrintWriter(out));
	}

	private void redirectOut() throws IOException {
		PipedOutputStream pipedOutputStream = new PipedOutputStream();
		PrintStream printStream = new PrintStream(pipedOutputStream);
		System.setOut(printStream);

		PipedInputStream pipedInputStream = new PipedInputStream();
		pipedOutputStream.connect(pipedInputStream);

		addOutput(pipedInputStream);
	}

	public void send(String message) {
		inRedirects.forEach(printer -> {
			printer.println(message);
			printer.flush();
		});
	}

	public void addOutput(InputStream inputStream) {
		Thread outRedirectThread = new Thread(() -> {
			try (Scanner scanner = new Scanner(inputStream)) {
				while (scanner.hasNext()) {
					String nextLine = scanner.nextLine();

					outRedirects.forEach((printer) -> {
						printer.println(nextLine);
					});

					systemOut.println(nextLine);
				}
			}
		});
		outRedirectThread.setDaemon(true);
		outRedirectThread.start();
	}

	public void addOutRedirect(Writer writer) {
		outRedirects.add(new PrintWriter(writer));
	}
}

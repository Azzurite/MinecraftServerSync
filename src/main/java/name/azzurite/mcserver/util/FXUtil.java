package name.azzurite.mcserver.util;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.util.Callback;
import name.azzurite.mcserver.view.MainWindow;

public final class FXUtil {

	private static final Package ROOT_VIEW_PACKAGE = MainWindow.class.getPackage();
	private static final ClassLoader CLASS_LOADER = FXUtil.class.getClassLoader();

	private FXUtil() {
	}

	public static <T> T createFXMLController(Class<T> controllerClass) throws IOException {
		return createFXMLController(controllerClass, null);
	}

	public static <T> T createFXMLController(Class<T> controllerClass, Callback<Class<?>, Object> controllerFactory) throws IOException {
		URL fxmlClasspathURL = createControllerFXMLClasspathURL(controllerClass);
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlClasspathURL);
		fxmlLoader.setControllerFactory(controllerFactory);
		fxmlLoader.load();
		return fxmlLoader.getController();
	}

	private static URL createControllerFXMLClasspathURL(Class<?> controllerClass) {
		String cleanedControllerPath = controllerClass.getName().replace(ROOT_VIEW_PACKAGE.getName() + '.', "");

		String controllerFXMLPath = "fxml/" + cleanedControllerPath + ".fxml";
		URL resource = CLASS_LOADER.getResource(controllerFXMLPath);
		if (resource == null) {
			throw new IllegalStateException("Could not find fxml " + controllerFXMLPath);
		}
		return resource;
	}

	public static Image retrieveClassPathImage(String name) {
		return new Image(CLASS_LOADER.getResourceAsStream(name));
	}

}

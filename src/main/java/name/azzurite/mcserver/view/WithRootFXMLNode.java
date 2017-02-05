package name.azzurite.mcserver.view;

import javafx.fxml.FXML;
import javafx.scene.Node;

public class WithRootFXMLNode implements NodeRepresentation {

	@FXML
	protected Node root;

	public WithRootFXMLNode() {
	}

	public WithRootFXMLNode(Node root) {
		this.root = root;
	}

	@Override
	public Node toNodeRepresentation() {
		return root;
	}
}

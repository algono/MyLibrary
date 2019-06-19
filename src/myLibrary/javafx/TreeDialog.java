package myLibrary.javafx;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

public class TreeDialog<T> extends Dialog<List<TreeItem<T>>> {
	
	private final TreeView<T> treeView;
	
	public TreeDialog(TreeItem<T> treeRoot) {
		super();
		
		treeView = new TreeView<T>(treeRoot);
		
		DialogPane dialogPane = this.getDialogPane();
		dialogPane.setContent(treeView);
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? new ArrayList<>(treeView.getSelectionModel().getSelectedItems()) : null;
        });
	}
	
	public TreeView<T> getTree() {
		return treeView;
	}
	
	public void refresh() {
		treeView.refresh();
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 *
 * @author Alejandro
 */
public class ErrorAlert extends Alert {
    
    public ErrorAlert(Throwable ex) {
        super(Alert.AlertType.ERROR);
        init(ex);
    }
    
    public ErrorAlert(Throwable ex, String contentText, ButtonType... buttons) {
        super(Alert.AlertType.ERROR, contentText, buttons);
        init(ex);
    }
    
    private void init(Throwable ex) {
        setHeaderText(null);
        if (ex != null) {
            String errorOutput = ex.getMessage();
            if (!errorOutput.isEmpty()) {
                TextArea errorContent = new TextArea(errorOutput);
                errorContent.setEditable(false); errorContent.setWrapText(true);
                getDialogPane().setExpandableContent(new VBox(errorContent));
            }
        }
    }
    
}

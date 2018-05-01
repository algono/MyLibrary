/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx.loadingScreen;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;

/**
 *
 * @author Alejandro
 * @param <V> The result type returned
 */
public abstract class LoadingTask<V> extends Task<V> {
    
    private String errorMsg;
    private boolean exitOnFailed;
    
    public LoadingTask(String errMsg) {
        this(errMsg, false);
    }
    public LoadingTask(String errMsg, boolean exitOF) {
        super();
        errorMsg = errMsg;
        exitOnFailed = exitOF;
        setOnFailed((e) -> {
            getOutputAlert().showAndWait();
            if (exitOnFailed) System.exit(-1);
        });
    }
    //Getters
    public String getOutputMessage() { return errorMsg; }
    public Alert getOutputAlert() {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR, errorMsg);
        errorAlert.setHeaderText(null);
        return errorAlert;
    }
    public boolean getExitOnFailed() { return exitOnFailed; }
}
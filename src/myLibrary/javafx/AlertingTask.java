/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

/**
 *
 * @author Alejandro
 * @param <T>
 */
public abstract class AlertingTask<T> extends Task<T> {
      
    private String errorMsg;
    protected boolean showError;
    
    public AlertingTask() { this(null); }
    public AlertingTask(String errMsg) {
        super();
        if (errMsg != null) {
            showError = true;
            errorMsg = errMsg;
        }
    }
    
    @Override
    protected final T call() throws Exception {
        T res;
        try {
            res = doTask();
        } catch (Exception ex) {
            //Antes de que la task termine y se considere como 'failed', muestra el mensaje de error si su flag (showError) lo habilita,
            //y después vuelve a lanzar la excepción para que los demás la traten debidamente
            if (showError) {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    getErrorAlert(ex).showAndWait();
                    latch.countDown();
                });
                try {
                    latch.await();
                //Evitamos que la InterruptedException del await tape la excepción que lanzó la propia task
                } catch (InterruptedException iEx) {}
            }
            throw ex;
        }
        return res;
    }
    
    protected abstract T doTask() throws Exception;
            
    //Getters
    public String getErrorMessage() { return errorMsg; }   
    
    protected Alert getErrorAlert(Throwable ex) {
        return new ErrorAlert(ex, errorMsg);
    }
    
    //Setters
    public void showErrorMessage(boolean show) { showError = show; } 
    protected void updateErrorMsg(String errMsg) { errorMsg = errMsg; }
          
}
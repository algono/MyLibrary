/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 *
 * @author Alejandro
 */
public class LoadingQueue extends LinkedBlockingQueue<Task> {

    //Main service
    final LoadingService main;
    
    public LoadingQueue(Task... tasks) {
        super(Arrays.asList(tasks));
        main = new LoadingService(this);
        //When the service stops running, notify the threads waiting on waitFor()
        main.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            if (!isRunning) synchronized(this) { this.notifyAll(); }
        });
    }
    
    //Properties
    public ReadOnlyStringProperty messageProperty() { return main.messageProperty(); }
    public ReadOnlyDoubleProperty progressProperty() { return main.progressProperty(); }
    
    //Ways to get if the tasks succeeded or not
    //1- Wait without timeout
    public boolean waitFor() {
        return waitFor(0); //If timeout = 0, it is not taken into account
    }
    //2- Wait with timeout (milliseconds)
    public synchronized boolean waitFor(long timeout) {
        while (main.isRunning()) {
            try {
                this.wait(timeout);
            } catch (InterruptedException ex) {}
        }
        return isSucceeded();
    }
    //3- Without waiting, if it is still running returns false
    public boolean isSucceeded() { 
        return main.getState() == Worker.State.SUCCEEDED;
    }
    
    public void start() {
        main.restart();
    }
    
    public void cancel() {
        main.cancel();
    }
    
}

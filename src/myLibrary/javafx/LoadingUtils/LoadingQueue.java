/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 *
 * @author Alejandro
 */
public class LoadingQueue {

    //Queue containing the tasks the loading screen will be doing
    protected final BlockingQueue<Task> queue;
    //The currently running task, and the current message and progress properties
    private Task currentTask;
    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper();
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();
    //The thread which runs the tasks from the queue secuentially
    protected final Task<Void> mainTask = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
            while (!isCancelled() && !queue.isEmpty()) {
                currentTask = queue.poll();
                Platform.runLater(() -> {
                    message.bind(currentTask.messageProperty());
                    progress.bind(currentTask.progressProperty());
                });
                currentTask.run(); currentTask.get();
                Platform.runLater(() -> {
                    message.unbind(); progress.unbind();
                    if (currentTask.getState() != Worker.State.SUCCEEDED) { cancel(); }
                });
            }
            return null;
        }
    };
    protected final Thread thread = new Thread(mainTask);
    
    public LoadingQueue(Task... tasks) {
        queue = new LinkedBlockingQueue<>(Arrays.asList(tasks));
    }
    //Getters
    public BlockingQueue<Task> getQueue() { return queue; }
    //Properties
    public ReadOnlyStringProperty messageProperty() { return message.getReadOnlyProperty(); }
    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    //Ways to get if the tasks succeeded or not
    //1- Wait without timeout
    public boolean waitFor() throws InterruptedException {
        return waitFor(0); //If timeout = 0, it is not taken into account
    }
    //2- Wait with timeout (milliseconds)
    public synchronized boolean waitFor(long timeout) throws InterruptedException {
        thread.join(timeout);
        return isSucceeded();
    }
    //3- Without waiting, if it is still running returns false
    public boolean isSucceeded() { 
        return mainTask.getState() == Worker.State.SUCCEEDED; }
    
    public void start() { thread.start(); }
    
    public void cancel() {
        if (currentTask == null) mainTask.cancel();
        else currentTask.cancel();
    }
    
}

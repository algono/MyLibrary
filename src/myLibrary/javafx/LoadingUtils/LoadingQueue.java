/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    protected final Task<Boolean> mainTask = new Task<Boolean>() {
        @Override
        protected Boolean call() throws Exception {
            while (!queue.isEmpty()) {
                currentTask = queue.poll();
                Platform.runLater(() -> {
                    message.bind(currentTask.messageProperty());
                    progress.bind(currentTask.progressProperty());
                });
                currentTask.run();
                Platform.runLater(() -> {
                    message.unbind(); progress.unbind();
                });
                if (currentTask.getState() != Worker.State.SUCCEEDED) { return false; }
            }
            return true;
        }
    };
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
    public boolean waitFor() throws InterruptedException, ExecutionException, TimeoutException {
        return waitFor(0); //If timeout = 0, it is not taken into account
    }
    //2- Wait with timeout (milliseconds)
    public synchronized boolean waitFor(long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        return mainTask.get(timeout, TimeUnit.MILLISECONDS);
    }
    //3- Without waiting, if it is still running returns false
    public boolean isSucceeded() {
        return mainTask.getValue() == null ? false : mainTask.getValue();
    }
    //Starts the loading process
    public void start() { new Thread(mainTask).start(); }
    //Cancels the loading process (by cancelling the main task)
    public void cancel() { currentTask.cancel(); }
    
}

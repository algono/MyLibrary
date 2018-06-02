/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 *
 * @author Alejandro
 */
public class LoadingQueue {

    //Queue containing the tasks the loading screen will be doing
    protected final BlockingQueue<Task> queue;
    //The main task runs the tasks from the queue secuentially
    protected final Task<Void> mainTask = new Task<Void>() {
        private Task currentTask;
        //If the main task is cancelled, it cancels any currently running task as well
        @Override
        protected void cancelled() {
            if (currentTask != null) {
                currentTask.cancel();
            }
        }
        @Override
        protected Void call() throws Exception {
            while (!isCancelled() && !queue.isEmpty()) {
                currentTask = queue.poll();
                currentTask.messageProperty().addListener((obs, oldMsg, newMsg) -> updateMessage(newMsg));
                currentTask.run();
                if (currentTask.isCancelled()) cancel(); //If the current task was cancelled, cancel all
            }
            return null;
        }
    };
    public LoadingQueue(Task... tasks) {
        queue = new LinkedBlockingQueue<>(Arrays.asList(tasks));
        //When all tasks end, notify the threads waiting for its completion
        mainTask.runningProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) synchronized(this) { notifyAll(); }
        });
    }
    //Getters
    public BlockingQueue<Task> getQueue() { return queue; }
    //Ways to get if the tasks succeeded or not
    //1- Wait without timeout
    public boolean waitFor() {
        return waitFor(0); //If timeout = 0, it is not taken into account
    }
    //2- Wait with timeout (milliseconds)
    public synchronized boolean waitFor(long timeout) {
        try {
            while (mainTask.isRunning()) { wait(timeout); }
        } catch (InterruptedException ex) {}
        
        return mainTask.getState() == Worker.State.SUCCEEDED;
    }
    //3- Without waiting, if it is still running returns false
    public boolean isSucceeded() {
        return mainTask.getState() == Worker.State.SUCCEEDED;
    }
    //Start the loading process
    public void start() { new Thread(mainTask).start(); }
    //Cancels the loading process (by cancelling the main task)
    public void cancel() { mainTask.cancel(); }
}

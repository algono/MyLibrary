/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 *
 * @author Alejandro
 */
class LoadingService extends Service<Void> {

    private final LoadingQueue queue;
    private Task currentTask;

    public LoadingService(LoadingQueue lQueue) {
        queue = lQueue;
    }

    public Task getCurrentTask() {
        return currentTask;
    }   
    
    @Override
    public boolean cancel() {
        boolean cancelled = super.cancel();
        if (currentTask != null) currentTask.cancel();
        return cancelled;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            //Listeners for updating the properties
            private final ChangeListener<String> msgListener = (obs, oldMsg, newMsg) -> updateMessage(newMsg);
            private final ChangeListener<Number> progressListener = (obs, oldProg, newProg) -> {
                updateProgress(currentTask.getWorkDone(), currentTask.getTotalWork());
            };
            @Override
            protected Void call() throws Exception {
                while (!isCancelled() && !queue.isEmpty()) {
                    //Gets a task from the queue
                    currentTask = queue.poll();
                    
                    //Adds the updater listeners to the current task properties
                    currentTask.messageProperty().addListener(msgListener);
                    currentTask.progressProperty().addListener(progressListener);
                    
                    //Runs the task
                    currentTask.run();
                    
                    CountDownLatch latch = new CountDownLatch(1);
                    Platform.runLater(() -> {
                        //Removes the updater listeners from the task properties (as it has ended)
                        currentTask.messageProperty().removeListener(msgListener);
                        currentTask.progressProperty().removeListener(progressListener);
                        
                        //Checks if the task succeeded. If it didn't, the whole process is cancelled
                        if (currentTask.getState() != Worker.State.SUCCEEDED) {
                            this.cancel();
                        }
                        
                        latch.countDown();
                    });
                    latch.await();
                }
                return null;
            }
        };
    }

}

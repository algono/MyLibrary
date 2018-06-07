/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 *
 * @author Alejandro
 */
class LoadingService extends Service<Void> {

    private final BlockingQueue<Task> queue;
    private Task currentTask;

    public LoadingService(LoadingQueue loadingQueue) {
        queue = loadingQueue.queue;
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
            @Override
            protected Void call() throws Exception {
                
                double workCount = 0.0;
                for (Task t : queue) {
                    double tWork = t.getTotalWork();
                    if (tWork > 0) workCount += tWork;
                }
                updateProgress(0, workCount); //Initial progress
                
                while (!isCancelled() && !queue.isEmpty()) {
                    currentTask = queue.poll();

                    currentTask.messageProperty().addListener((obs, oldMsg, newMsg) -> updateMessage(newMsg));
                    currentTask.progressProperty().addListener((obs, oldProg, newProg) -> {
                        updateProgress(getWorkDone() + currentTask.getWorkDone(), getTotalWork());
                    });

                    currentTask.run();

                    CountDownLatch latch = new CountDownLatch(1);
                    Platform.runLater(() -> {
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

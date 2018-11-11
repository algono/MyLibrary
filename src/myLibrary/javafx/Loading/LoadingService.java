/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

/**
 *
 * @author Alejandro
 */
public class LoadingService extends Service<Void> {

    //List of the workers being loaded each time the service starts
    protected final List<Worker> workers;
    
    private final LinkedBlockingQueue<Worker> queue = new LinkedBlockingQueue<>();
    
    private Worker currentWorker;

    public LoadingService(Worker... workers) {
        this(new ArrayList<Worker>(Arrays.asList(workers)));
    }
    public LoadingService(List<Worker> workers) {
        this.workers = workers;
        queue.addAll(workers);
    }
    
    public Worker getCurrentWorker() {
        return currentWorker;
    }   
    
    public List<Worker> getWorkerList() { return workers; }
    
    private static void runAndWait(Runnable runnable) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            runnable.run();
            latch.countDown();
        });
        latch.await();
    }
    
    @Override
    public boolean cancel() {
        boolean cancelled = super.cancel();
        if (currentWorker != null && currentWorker.isRunning()) currentWorker.cancel();
        return cancelled;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            //Listeners for updating the properties
            private final ChangeListener<String> msgListener = (obs, oldMsg, newMsg) -> updateMessage(newMsg);
            private final ChangeListener<Number> progressListener = (obs, oldProg, newProg) -> {
                updateProgress(currentWorker.getWorkDone(), currentWorker.getTotalWork());
            };
            @Override
            protected Void call() throws Exception {
                queue.clear(); //It clears the queue and populates it with the assigned workers
                queue.addAll(workers);
                while (!isCancelled() && !queue.isEmpty()) {
                    //Gets a worker from the queue
                    currentWorker = queue.poll();
                    
                    //Adds the updater listeners to the current worker properties
                    runAndWait(() -> {
                        currentWorker.messageProperty().addListener(msgListener);
                        currentWorker.progressProperty().addListener(progressListener);
                    });
                    
                    final CountDownLatch doneLatch = new CountDownLatch(1);
                    final ChangeListener<Boolean> doneListener = (obs, oldVal, newVal) -> {
                        if (!newVal) { doneLatch.countDown(); }
                    };
                    
                    //Runs the worker and waits for its completion
                    //(If the Worker is a Task, it just runs it, but if it is a Service, it restarts the Service)
                    if (currentWorker instanceof Task) {
                        ((Task) currentWorker).run();
                    } else if (currentWorker instanceof Service) {
                        Service currentService = ((Service) currentWorker);
                        Platform.runLater(() -> {
                            currentService.reset();
                            currentService.runningProperty().addListener(doneListener);
                            currentService.start();
                        });
                        doneLatch.await();
                    }
                    
                    runAndWait(() -> {
                        //Removes the updater listeners from the worker properties (as it has ended)
                        currentWorker.runningProperty().removeListener(doneListener);
                        currentWorker.messageProperty().removeListener(msgListener);
                        currentWorker.progressProperty().removeListener(progressListener);
                        
                        //Checks if the worker succeeded. If it didn't, the whole process is cancelled
                        if (currentWorker.getState() != Worker.State.SUCCEEDED) {
                            this.cancel();
                        }
                    });
                }
                return null;
            }
        };
    }

}

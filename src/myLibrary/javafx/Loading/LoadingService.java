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
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    
    private final ObjectProperty<Worker> currentWorker = new SimpleObjectProperty<>(null);

    public LoadingService(Worker... workers) {
        this(new ArrayList<Worker>(Arrays.asList(workers)));
    }
    public LoadingService(List<Worker> workers) {
        this.workers = workers;
        queue.addAll(workers);
    }
    
    public Worker getCurrentWorker() {
        return currentWorker.get();
    }   
    protected ObjectProperty<Worker> currentWorkerProperty() { return currentWorker; }
    
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
        Worker worker = currentWorker.get();
        if (worker != null && worker.isRunning()) worker.cancel();
        return cancelled;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            //Listeners for updating the properties
            private final ChangeListener<String> msgListener = (obs, oldMsg, newMsg) -> updateMessage(newMsg);
            private final ChangeListener<Number> progressListener = (obs, oldProg, newProg) -> {
                if (newProg.intValue() > 0) { //Si el progreso no es indeterminado, recalcula el progreso total
                    double workDone = currentWorker.get().getWorkDone(), totalWork = currentWorker.get().getTotalWork();
                    /** Regla de 3:
                     * workDone -- totalWork
                     *     X    -- 1 + totalWork
                     * X = (1 + totalWork) * workDone / totalWork
                     */
                    updateProgress(this.getWorkDone() + ((1 + totalWork) * workDone / totalWork), this.getTotalWork());
                }
            };
            @Override
            protected Void call() throws Exception {
                final AtomicReference<Double> totalWorkRef = new AtomicReference<>(0.0);
                //Calculates the totalWork of all Workers
                runAndWait(() -> {
                    double totalWork = 0.0;
                    for (Worker w : workers) {
                        if (w.getTotalWork() > 0) totalWork += w.getTotalWork();
                        totalWork++;
                    }
                    totalWorkRef.set(totalWork);
                });
                double totalWork = totalWorkRef.get();
                
                queue.clear(); //It clears the queue and populates it with the assigned workers
                queue.addAll(workers);
                while (!isCancelled() && !queue.isEmpty()) {
                    //Gets a worker from the queue
                    Worker worker = queue.poll();
                    currentWorker.set(worker);
                    
                    //Updates the current progress
                    if (totalWork > workers.size() || (totalWork == workers.size() && workers.size() > 1)) {
                        updateProgress(workers.size() - queue.size(), totalWork);
                    }
                    
                    //Adds the updater listeners to the current worker properties
                    runAndWait(() -> {
                        worker.messageProperty().addListener(msgListener);
                        if (this.getTotalWork() > workers.size()) worker.progressProperty().addListener(progressListener);
                    });
                    
                    final CountDownLatch doneLatch = new CountDownLatch(1);
                    final ChangeListener<Boolean> doneListener = (obs, oldVal, newVal) -> {
                        if (!newVal) { doneLatch.countDown(); }
                    };
                    
                    //Runs the worker and waits for its completion
                    //(If the Worker is a Task, it just runs it, but if it is a Service, it restarts the Service)
                    if (worker instanceof Task) {
                        ((Task) worker).run();
                    } else if (worker instanceof Service) {
                        Service currentService = ((Service) worker);
                        Platform.runLater(() -> {
                            currentService.reset();
                            currentService.runningProperty().addListener(doneListener);
                            currentService.start();
                        });
                        doneLatch.await();
                    }
                    
                    runAndWait(() -> {
                        //Removes the updater listeners from the worker properties (as it has ended)
                        worker.runningProperty().removeListener(doneListener);
                        worker.messageProperty().removeListener(msgListener);
                        worker.progressProperty().removeListener(progressListener);
                        
                        //Checks if the worker succeeded. If it didn't, the whole process is cancelled
                        if (worker.getState() != Worker.State.SUCCEEDED) {
                            this.cancel();
                        }
                    });
                }
                currentWorker.set(null);
                return null;
            }
        };
    }

}

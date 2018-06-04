/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 *
 * @author Alejandro
 */
public class LoadingScene extends Scene {
    
    //Default width and height values
    public static final double DEF_WIDTH = 300, DEF_HEIGHT = 175;
    //Stage elements
    protected final Label label = new Label();
    protected final ProgressIndicator progress = new ProgressIndicator();
    //The main task runs the tasks from the queue secuentially
    protected final LoadingQueue loadingQueue;
    protected final ChangeListener<Boolean> startListener;
    //Constructors
    public LoadingScene(Task... tasks) {
        this(DEF_WIDTH, DEF_HEIGHT, tasks);
    }
    public LoadingScene(double width, double height, Task... tasks) {
        super(new VBox(10), width, height);
        VBox root = (VBox) getRoot();
        loadingQueue = new LoadingQueue(tasks);
        //Sets the default root alignment and elements
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(label, progress);
        //Sets the text color bluish by default
        label.setTextFill(Color.web("#3d81e3"));
        //Binds the label's text and the indicator's progress to the main task's message and progress
        label.textProperty().bind(loadingQueue.messageProperty());
        progress.progressProperty().bind(loadingQueue.progressProperty());
        //Listener that starts the loading process
        startListener = (obs, oldValue, newValue) -> {
            if (newValue) { loadingQueue.start(); }
        };
        //Add the properties to any new scene's window so that the tasks start when the scene is shown
        windowProperty().addListener((obsWin, oldWindow, newWindow) -> {
            //Remove the listener from the previous window (if there's any)
            if (oldWindow != null) oldWindow.showingProperty().removeListener(startListener);
            //If the window is already showing, start the process now
            if (newWindow.isShowing()) loadingQueue.start();
            newWindow.showingProperty().addListener(startListener);
        });
    }
    
    //Getters
    public Label getLabel() { return label; }
    public ProgressIndicator getProgressIndicator() { return progress; }
    public BlockingQueue<Task> getQueue() { return loadingQueue.getQueue(); }
    
    //Getting if tasks succeeded or not
    public boolean waitFor() throws InterruptedException, ExecutionException, TimeoutException { return loadingQueue.waitFor(); }
    public boolean waitFor(long timeout) throws InterruptedException, ExecutionException, TimeoutException { return loadingQueue.waitFor(timeout); }
    public boolean isSucceeded() { return loadingQueue.isSucceeded(); }
    
}

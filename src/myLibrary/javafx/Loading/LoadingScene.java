/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
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
    protected final Label label = new Label("Loading...");
    protected final ProgressIndicator progress = new ProgressIndicator();
    
    //The main Service runs the workers from the queue secuentially
    protected final LoadingQueue loadingQueue;
    protected final ChangeListener<Boolean> startListener;
    
    //Constructors
    public LoadingScene(Worker... workers) {
        this(DEF_WIDTH, DEF_HEIGHT, workers);
    }
    public LoadingScene(double width, double height, Worker... workers) {
        super(new VBox(10), width, height);
        VBox root = (VBox) getRoot();
        loadingQueue = new LoadingQueue(workers);
        
        //Sets the default root alignment and elements
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(label, progress);
        
        //Sets the text color bluish by default
        label.setTextFill(Color.web("#3d81e3"));
        
        //Listeners/bindings to the message and progress properties
        loadingQueue.messageProperty().addListener((obs, oldMsg, newMsg) -> label.setText(newMsg));
        progress.progressProperty().bind(loadingQueue.progressProperty());
        
        //Listener that starts the loading process
        startListener = (obs, oldValue, newValue) -> {
            if (newValue) { loadingQueue.start(); }
        };
        
        //Add the properties to any new scene's window so that the workers start when the scene is shown
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
    public LoadingQueue getQueue() { return loadingQueue; }
    
    //Getting if workers succeeded or not
    public boolean waitFor() { return loadingQueue.waitFor(); }
    public boolean waitFor(long timeout) { return loadingQueue.waitFor(timeout); }
    public boolean isSucceeded() { return loadingQueue.isSucceeded(); }
    
}

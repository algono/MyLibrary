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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 *
 * @author Alejandro
 */
public class LoadingScene extends Scene {
    
    //Default width and height values (300 x 175)
    public static final double DEF_WIDTH = 300, DEF_HEIGHT = 175;
    
    //Stage elements
    private final VBox root;
    private Label label = new Label("Loading...");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ProgressBar progressBar = new ProgressBar();
    
    //If true, the progressBar is shown instead of the progressIndicator (and the other way around)
    private boolean isProgressBar = false;
    
    //The main Service runs the workers from the queue secuentially
    protected final LoadingQueue loadingQueue;
    
    //Listener for starting the loading Service when the scene is shown
    protected final ChangeListener<Boolean> startListener;
    
    //Constructors
    public LoadingScene(Worker... workers) {
        this(DEF_WIDTH, DEF_HEIGHT, workers);
    }
    public LoadingScene(double width, double height, Worker... workers) {
        super(new VBox(10), width, height);
        root = (VBox) getRoot();
        loadingQueue = new LoadingQueue(workers);
        
        //Sets the default root alignment and elements
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(label, progressIndicator);
        
        //Sets the text color bluish by default
        label.setTextFill(Color.web("#3d81e3"));
        
        //Listeners/bindings to the message and progress properties
        loadingQueue.messageProperty().addListener((obs, oldMsg, newMsg) -> label.setText(newMsg));
        progressIndicator.progressProperty().bind(loadingQueue.progressProperty());
        progressBar.progressProperty().bind(loadingQueue.progressProperty());
        
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
    public ProgressIndicator getProgressIndicator() { return isProgressBar ? progressBar : progressIndicator; }
    public LoadingQueue getQueue() { return loadingQueue; }
    
    //Setters
    public void setLabel(Label l) { label = l; }
    public void setProgressBar(boolean willBeProgressBar) {
        if (isProgressBar != willBeProgressBar) { //If the value is actually changing, do the changing based on the new value
            if (willBeProgressBar) {
                root.getChildren().remove(progressIndicator);
                root.getChildren().add(progressBar);
            } else {
                root.getChildren().remove(progressBar);
                root.getChildren().add(progressIndicator);
            }
            isProgressBar = willBeProgressBar;
        } 
    }
    //Sets the root back to the original root made by the constructor
    public void resetRoot() { setRoot(root); }
    
    //Getting if workers succeeded or not
    public boolean waitFor() { return loadingQueue.waitFor(); }
    public boolean waitFor(long timeout) { return loadingQueue.waitFor(timeout); }
    public boolean isSucceeded() { return loadingQueue.isSucceeded(); }
    
}

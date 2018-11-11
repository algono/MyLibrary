/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import javafx.application.Platform;
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
    private final ProgressBar workerProgressBar = new ProgressBar(), totalProgressBar = new ProgressBar();
    
    //If true, the progressBar is shown instead of the progressIndicator (and the other way around)
    private boolean isProgressBar = false;
    
    //The main Service runs the workers from the queue secuentially
    final LoadingService main;
    
    //Listeners
    protected final ChangeListener<Boolean> startListener;
    protected final ChangeListener<Worker> currentWorkerListener = (obs, oldWorker, newWorker) -> {
        workerProgressBar.progressProperty().unbind();
        if (newWorker != null) {
            //A Worker's progressProperty can only be used from the FX Application Thread
            Platform.runLater(() -> workerProgressBar.progressProperty().bind(newWorker.progressProperty()));
        }
    };
    private boolean showProgress;
    
    //Constructors
    public LoadingScene(Worker... theWorkers) {
        this(DEF_WIDTH, DEF_HEIGHT, theWorkers);
    }
    public LoadingScene(double width, double height, Worker... workers) {
        super(new VBox(10), width, height);
        root = (VBox) getRoot();
        main = new LoadingService(workers);
        
        //Sets the default root alignment and elements
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(label, progressIndicator);
        
        //Sets the text color bluish by default
        label.setTextFill(Color.web("#3d81e3"));
        
        //Listeners/bindings to the message and progress properties
        main.messageProperty().addListener((obs, oldMsg, newMsg) -> label.setText(newMsg));
        showProgress(true); //By default, showProgress = true.
          
        //Listener that starts (or cancels) the loading process
        startListener = (obs, oldValue, newValue) -> {
            if (newValue) {
                //If the window is showing, it restarts the Service (if it wasn't already running).
                if (!main.isRunning()) main.restart();
            } else { main.cancel(); } //If the window is hiding and the Service is still running, it is cancelled.
        };
        
        //Add the properties to any new scene's window so that the workers start when the scene is shown
        windowProperty().addListener((obsWin, oldWindow, newWindow) -> {
            
            //Remove the listener from the previous window (if there's any)
            if (oldWindow != null) oldWindow.showingProperty().removeListener(startListener);
            
            //If the window is already showing, start the process now
            if (newWindow.isShowing() && !main.isRunning()) main.restart();
            
            newWindow.showingProperty().addListener(startListener);
        });
    }
    
    //Getters
    public Label getLabel() { return label; }
    public ProgressIndicator getProgressIndicator() { return isProgressBar ? totalProgressBar : progressIndicator; }
    
    public LoadingService getLoadingService() { return main; }
    
    //Setters
    public void setLabel(Label l) { label = l; }
    public void setProgressBar(boolean willBeProgressBar) {
        if (isProgressBar != willBeProgressBar) { //If the value is actually changing, do the changing based on the new value
            if (willBeProgressBar) {
                //Adds Listener for workerProgressBar
                if (showProgress) main.currentWorkerProperty().addListener(currentWorkerListener);
                //Replaces indicator with bars
                root.getChildren().remove(progressIndicator);
                root.getChildren().addAll(workerProgressBar, totalProgressBar);
            } else {
                //Removes Listener for workerProgressBar
                main.currentWorkerProperty().removeListener(currentWorkerListener);
                //Replaces bars with indicator
                root.getChildren().removeAll(workerProgressBar, totalProgressBar);
                root.getChildren().add(progressIndicator);
            }
            isProgressBar = willBeProgressBar;
        } 
    }
    
    public final void showProgress(boolean show) {
        if (show) {
            if (isProgressBar) main.currentWorkerProperty().addListener(currentWorkerListener);
            progressIndicator.progressProperty().bind(main.progressProperty());
            totalProgressBar.progressProperty().bind(main.progressProperty());
        } else {
            main.currentWorkerProperty().removeListener(currentWorkerListener);
            progressIndicator.progressProperty().unbind();
            totalProgressBar.progressProperty().unbind();
        }
        showProgress = show;
    }
    //Sets the root back to the original root made by the constructor
    public void resetRoot() { setRoot(root); }
    
    //Getting if the loading process succeeded or not
    public boolean isSucceeded() { return main.getState() == Worker.State.SUCCEEDED; }
    
}

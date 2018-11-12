/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
    private final GridPane progressBarPane = new GridPane();
    private Label label = new Label("Loading...");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ProgressBar workerProgressBar = new ProgressBar(), totalProgressBar = new ProgressBar();
    
    //If true, the progressBar is shown instead of the progressIndicator (and the other way around)
    private final BooleanProperty isProgressBar = new SimpleBooleanProperty(false);
    
    //The main Service runs the workers from the queue secuentially
    final LoadingService main;
    
    //Listeners
    protected final ChangeListener<Boolean> startListener;
    protected final ChangeListener<Worker> progressListener = (obs, oldWorker, newWorker) -> {
        progressIndicator.progressProperty().unbind();
        workerProgressBar.progressProperty().unbind();
        if (newWorker != null) {
            //A Worker's progressProperty can only be used from the FX Application Thread
            Platform.runLater(() -> {
                progressIndicator.progressProperty().bind(newWorker.progressProperty());
                workerProgressBar.progressProperty().bind(newWorker.progressProperty());
            });
        }
    };
    
    //Constructors
    public LoadingScene(Worker... workers) {
        this(DEF_WIDTH, DEF_HEIGHT, workers);
    }
    public LoadingScene(double width, double height, Worker... workers) {
        super(new VBox(10), width, height);
        root = (VBox) getRoot();
        main = new LoadingService(workers);
        
        //Sets the default root alignment and elements
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(label, new StackPane(progressIndicator, progressBarPane));
        
        //Sets the text color bluish by default
        label.setTextFill(Color.web("#3d81e3"));
        
        //Binding for the progressBars' visibility
        progressBarPane.visibleProperty().bind(isProgressBar);
        progressIndicator.visibleProperty().bind(isProgressBar.not());
        
        //Listeners/bindings to the message and progress properties
        main.messageProperty().addListener((obs, oldMsg, newMsg) -> label.setText(newMsg));
        main.currentWorkerProperty().addListener(progressListener);
        totalProgressBar.progressProperty().bind(main.progressProperty());
          
        //Listener that starts (or cancels) the loading process
        startListener = (obs, oldValue, newValue) -> {
            if (newValue) {
                //If the window is showing, it restarts the Service (if it wasn't already running).
                if (!main.isRunning()) main.restart();
            } else { //If the window is hiding and the Service is still running, it is cancelled.
                if (main.isRunning()) main.cancel();
            }
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
    public ProgressIndicator getProgressIndicator() { return progressIndicator; }
    public ProgressBar getWorkerProgressBar() { return totalProgressBar; }
    public ProgressBar getTotalProgressBar() { return totalProgressBar; }
    
    public LoadingService getLoadingService() { return main; }
    
    //Setters
    public void setLabel(Label l) { label = l; }
    public void setProgressBar(boolean willBeProgressBar) {
        if (isProgressBar.get() != willBeProgressBar) { //If the value is actually changing, do the changing based on the new value
            if (willBeProgressBar) {
                //Replaces indicator with bars
                root.getChildren().remove(progressIndicator);
                root.getChildren().addAll(workerProgressBar, totalProgressBar);
            } else {
                //Replaces bars with indicator
                root.getChildren().removeAll(workerProgressBar, totalProgressBar);
                root.getChildren().add(progressIndicator);
            }
            isProgressBar.set(willBeProgressBar);
        } 
    }
    //Sets the root back to the original root made by the constructor
    public void resetRoot() { setRoot(root); }
    
    //Getting if the loading process succeeded or not
    public boolean isSucceeded() { return main.getState() == Worker.State.SUCCEEDED; }
    
}

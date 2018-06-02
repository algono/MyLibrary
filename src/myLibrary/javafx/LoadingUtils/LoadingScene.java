/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.concurrent.BlockingQueue;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 *
 * @author Alejandro
 */
public class LoadingScene extends Scene {
    
    //Default width and height values
    public static final double DEF_WIDTH = 300, DEF_HEIGHT = 175;
    //Stage elements
    protected final GridPane root;
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
        super(new GridPane(), width, height);
        root = (GridPane) getRoot();
        loadingQueue = new LoadingQueue(tasks);
        //Sets the GridPane's alignment and gaps
        root.setAlignment(Pos.CENTER);
        root.setVgap(10); root.setHgap(10);
        setLabelPosition(Pos.TOP_CENTER);
        setProgressPosition(Pos.CENTER);
        //Sets the text color bluish by default
        label.setTextFill(Color.web("#3d81e3"));
        //Binds the label's text to the message of the main task
        label.textProperty().bind(loadingQueue.mainTask.messageProperty());
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
    public boolean waitFor() { return loadingQueue.waitFor(); }
    public boolean waitFor(long timeout) { return loadingQueue.waitFor(timeout); }
    public boolean isSucceeded() { return loadingQueue.isSucceeded(); }
    
    //Setters
    public final void setLabelPosition(Pos p) { setPosition(label, p); }
    public final void setProgressPosition(Pos p) { setPosition(progress, p); }
    
    //Sets the position of a node based on the given position
    protected final void setPosition(Node n, Pos p) {
        int row, column;
        switch (p.getHpos()) {
            case LEFT: column = 0; break;
            case RIGHT: column = 2; break;
            default: column = 1; break;
        }
        switch (p.getVpos()) {
            case TOP: row = 0; break;
            case BOTTOM: row = 2; break;
            default: row = 1; break;
        }
        root.add(n, column, row);
    }
}

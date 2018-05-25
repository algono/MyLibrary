/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Alejandro
 */
public class LoadingStage extends Stage {
    
    //Stage elements
    private Label state = new Label();
    private ProgressIndicator progress = new ProgressIndicator();
    //Queue containing the tasks the loading screen will be doing
    private final BlockingQueue<Task> queue;
    //The main task runs the tasks from the queue secuentially
    private final Task<Void> mainTask = new Task() {
        @Override
        protected Void call() throws Exception {
            while (!isCancelled() && !queue.isEmpty()) {
                Task t = queue.poll();
                Platform.runLater(() -> state.textProperty().bind(t.messageProperty()));
                t.run(); t.get(); //Runs the task and waits for its completion
                if (t.isCancelled()) cancel();
            }
            return null;
        }
    };
    //Constructor
    public LoadingStage(Task... tasks) {
        queue = new LinkedBlockingQueue<>(Arrays.asList(tasks));
        setTitle("Loading...");
        setWidth(300); setHeight(175); //Default width and height values
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        state.setTextFill(Color.web("#3d81e3")); //Sets the text color bluish by default
        setOnCloseRequest(e -> e.consume()); //Doesn't allow the user to close the window by any means
        genScene(); //Sets the default scene
        //Whenever the stage is shown, run the tasks
        showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) new Thread(mainTask).start();
        });
        //When all tasks end, notify the threads waiting and hide the stage 
        mainTask.runningProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                hide();
                synchronized(this) { notifyAll(); }
            }
        });
    }
    //Getters
    public BlockingQueue<Task> getQueue() { return queue; }
    public Label getLabel() { return state; }
    public ProgressIndicator getProgressIndicator() { return progress; }
    //Ways to get if the tasks succeeded or not
    //1- Wait without timeout
    public boolean waitToSucceeded() {
        return waitToSucceeded(0); //If timeout = 0, it is not taken into account
    }
    //2- Wait with timeout (milliseconds)
    public synchronized boolean waitToSucceeded(long timeout) {
        try {
            while (mainTask.isRunning()) { wait(timeout); }
        } catch (InterruptedException ex) {}
        
        return mainTask.getState() == Worker.State.SUCCEEDED;
    }
    //3- Without waiting, if it is still running returns false
    public boolean isSucceeded() {
        return mainTask.getState() == Worker.State.SUCCEEDED;
    }
    //Setters
    public void setLabel(Label l) { state = l; }
    public void setProgressIndicator(ProgressIndicator p) { progress = p; }

    //PRIVATE METHODS
    
    //Creates scene and elements
    private void genScene() {
        VBox root = new VBox(state, progress);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        Scene scene = new Scene(root, getWidth(), getHeight());
        setScene(scene);
    }
}

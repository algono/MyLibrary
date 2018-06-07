/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.LoadingUtils;

import java.util.concurrent.BlockingQueue;
import javafx.concurrent.Task;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Alejandro
 */
public class LoadingStage extends Stage {
    
    private final LoadingScene scene;
    
    //Constructors
    public LoadingStage(Task... tasks) {
        this(LoadingScene.DEF_WIDTH, LoadingScene.DEF_HEIGHT, tasks);
    }
    public LoadingStage(double width, double height, Task... tasks) {
        this(new LoadingScene(width, height, tasks));
    }
    public LoadingStage(LoadingScene s) {
        super(StageStyle.UNDECORATED);
        scene = s;
        setTitle("Loading...");
        setWidth(scene.getWidth()); setHeight(scene.getHeight());
        initModality(Modality.APPLICATION_MODAL);
        setScene(scene);
        //If the user tries to close the stage, it cancels the main task
        setOnCloseRequest(e -> { 
            scene.loadingQueue.cancel();
            e.consume(); //This will prevent the window from closing until the task has been successfully cancelled
        });
        //When all tasks end, hide the stage
        showingProperty().addListener((obs, wasShowing, isShowing) -> { 
            if (isShowing) {
                scene.loadingQueue.waitFor();
                hide();
            }
        });
    }
    //Getting the queue
    public BlockingQueue<Task> getQueue() { return scene.getQueue(); }
    //Getting if tasks succeeded or not
    public boolean waitFor() { return scene.waitFor(); }
    public boolean waitFor(long timeout) { return scene.waitFor(timeout); }
    public boolean isSucceeded() { return scene.isSucceeded(); }
    
}

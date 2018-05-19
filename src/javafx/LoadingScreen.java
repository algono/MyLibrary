/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Alejandro
 */
public class LoadingScreen {
    
    //Stage elements
    private final Label state = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    //Stage properties
    private String title = "Loading...";
    private double width = 300, height = 175; //Default width and height values
    private Modality modality = Modality.APPLICATION_MODAL;
    private Paint textFill = Color.web("#3d81e3"); //Sets the text color bluish by default
    private final List<Image> icons = new ArrayList<>();
    //Queue containing the tasks the loading screen will be doing
    private final BlockingQueue<Task> queue;
    
    //Constructor
    public LoadingScreen(Task... tasks) {
        queue = new LinkedBlockingQueue<>(Arrays.asList(tasks));
    }
    //Getters
    public String getTitle() { return title; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Modality getModality() { return modality; }
    public BlockingQueue<Task> getQueue() { return queue; }
    public ProgressIndicator getProgressIndicator() { return progress; }
    //Setters
    public void setTitle(String t) { title = t; }
    public void setWidth(double w) { width = w; }
    public void setHeight(double h) { height = h; }
    public void initModality(Modality m) { modality = m; }
    public void setTextFill(Paint p) { textFill = p; }
    //Add icons to stage
    public void addIcons(Image... ics) { icons.addAll(Arrays.asList(ics)); }
    //Creates stage and elements
    private Stage genStage() {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(modality);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.initStyle(StageStyle.UNDECORATED);
        if (!icons.isEmpty()) stage.getIcons().addAll(icons);
        stage.setOnCloseRequest(e -> e.consume()); //Doesn't allow the user to close the window by any means
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        state.setTextFill(textFill); 
        root.getChildren().addAll(state, progress);
        root.setSpacing(10);
        stage.setScene(new Scene(root, width, height));
        return stage;
    }
    
    /**
     * 
     * @return If the task was completed successfully
     */
    public boolean load() {
        Stage stage = genStage();
        //The main task runs the tasks from the queue secuentially
        Task<Void> mainTask = new Task() {
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
        mainTask.runningProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) stage.hide();
        });
        //Starts the main task
        new Thread(mainTask).start();
        //Shows the loading screen
        stage.showAndWait();
        //Returns if all tasks succeeded or not
        return mainTask.getState() == Worker.State.SUCCEEDED;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx;

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
    private Image[] icons;
    
    private final Task task;
    
    //Constructor
    public LoadingScreen(Task t) { task = t; }
    //Getters
    public String getTitle() { return title; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Modality getModality() { return modality; }
    public Task getTask() { return task; }
    //Setters
    public void setTitle(String t) { title = t; }
    public void setWidth(double w) { width = w; }
    public void setHeight(double h) { height = h; }
    public void initModality(Modality m) { modality = m; }
    public void setTextFill(Paint p) { textFill = p; }
    //Add icons to stage
    public void addIcons(Image... ics) { icons = ics; }
    //Creates stage and elements
    private Stage genStage() {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(modality);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.getIcons().addAll(icons);
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
        //Sets bindings and listeners
        state.textProperty().bind(task.messageProperty());
        task.runningProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) stage.hide();
        });
        //Starts the task
        new Thread(task).start();
        //Shows the loading screen
        stage.showAndWait();
        //Returns if the task succeeded or not
        return task.getState() == Worker.State.SUCCEEDED;
    }
}

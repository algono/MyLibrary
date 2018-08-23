/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import javafx.concurrent.Worker;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Alejandro
 */
public class LoadingStage extends Stage {
    
    private final LoadingScene scene;
    //Whether the stage should hide automatically when the loading service ends
    private boolean implicitHiding = true;
    
    //Constructors
    public LoadingStage(Worker... workers) {
        this(LoadingScene.DEF_WIDTH, LoadingScene.DEF_HEIGHT, workers);
    }
    public LoadingStage(double width, double height, Worker... workers) {
        this(new LoadingScene(width, height, workers));
    }
    public LoadingStage(LoadingScene s) {
        super(StageStyle.UNDECORATED);
        scene = s;
        setTitle("Loading...");
        setWidth(scene.getWidth()); setHeight(scene.getHeight());
        initModality(Modality.APPLICATION_MODAL);
        setScene(scene);
        
        //If all workers ended and the implicitHiding value is set to true, hide the stage
        scene.main.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            if (!isRunning && implicitHiding) hide();
        });
    }
    
    public LoadingService getLoadingService() { return scene.getLoadingService(); }
    
    //Getting if the loading process succeeded or not
    public boolean isSucceeded() { return scene.isSucceeded(); }
    
    public void setImplicitHiding(boolean isHiding) {
        implicitHiding = isHiding;
        if (implicitHiding && !scene.main.isRunning()) hide();
    }
}

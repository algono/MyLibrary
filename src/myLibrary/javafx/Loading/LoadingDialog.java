/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

/**
 * 
 * @author Alejandro
 */
public class LoadingDialog extends Dialog<ButtonType> {
    
    //Default width and height values (300 x 175)
    public static final double DEF_WIDTH = 300, DEF_HEIGHT = 175;
    
    //Stage elements
    private final GridPane root = new GridPane();
    private final Label label = new Label("Loading..."), workerLabel = new Label(), numWorkersLabel = new Label(), totalLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ProgressBar workerProgressBar = new ProgressBar(), totalProgressBar = new ProgressBar();
    /**
     * isProgressBar: If true, the progressBar is shown instead of the progressIndicator (and the other way around)
     * implicitHiding: Whether the dialog should hide automatically when the loading service ends
     * showProgressNumber: Whether it should show the progress in a numerical form as well
     */
    private boolean isProgressBar = false, implicitHiding = true, showProgressNumber = false;
    //Label color (sets the text color bluish by default)
    private final Color color = Color.web("#3d81e3"); 
    
    //The main Service runs the workers from the queue secuentially
    protected final LoadingService main;
    
    public LoadingDialog(Worker... workers) {
        this(DEF_WIDTH, DEF_HEIGHT, workers);
    }
    public LoadingDialog(double width, double height, Worker... workers) {
        super();
        initStyle(StageStyle.UNDECORATED);
        setTitle("Loading...");
        main = new LoadingService(workers);
        
        setWidth(width); setHeight(height);
        root.setPrefSize(width, height);
        getDialogPane().setContent(root);
        
        //Sets the default root alignment and elements
        root.setAlignment(Pos.CENTER);
        root.setVgap(5); root.setHgap(10);
        GridPane.setConstraints(label, 1, 0, 1, 1, HPos.CENTER, VPos.CENTER);
        StackPane stack = new StackPane(progressIndicator, workerProgressBar);
        GridPane.setConstraints(stack, 1, 1, 1, 1, HPos.CENTER, VPos.CENTER);
        GridPane.setConstraints(totalProgressBar, 1, 2, 1, 1, HPos.CENTER, VPos.CENTER);
        GridPane.setConstraints(numWorkersLabel, 0, 2, 1, 1, HPos.RIGHT, VPos.CENTER);
        GridPane.setConstraints(workerLabel, 2, 1, 1, 1, HPos.LEFT, VPos.CENTER);
        GridPane.setConstraints(totalLabel, 2, 2, 1, 1, HPos.LEFT, VPos.CENTER);
        root.getChildren().addAll(label, stack, totalProgressBar, numWorkersLabel, workerLabel, totalLabel);
        
        label.setTextFill(color);
        workerLabel.setTextFill(color);
        numWorkersLabel.setTextFill(color);
        totalLabel.setTextFill(color);
        
        workerLabel.textProperty().bind(workerProgressBar.progressProperty().multiply(100).asString().concat(" %"));
        totalLabel.textProperty().bind(totalProgressBar.progressProperty().multiply(100).asString().concat(" %"));
        numWorkersLabel.textProperty().bind(Bindings.format("%.0f/%.0f", main.workDoneProperty(), main.totalWorkProperty()));
        
        //Listeners/bindings to the message and progress properties
        main.messageProperty().addListener((obs, oldMsg, newMsg) -> label.setText(newMsg));
        
        main.currentWorkerProperty().addListener((obs, oldWorker, newWorker) -> {
            progressIndicator.progressProperty().unbind();
            workerProgressBar.progressProperty().unbind();
            if (newWorker != null) {
                //A Worker's progressProperty can only be used from the FX Application Thread
                Platform.runLater(() -> {
                    progressIndicator.progressProperty().bind(newWorker.progressProperty());
                    workerProgressBar.progressProperty().bind(newWorker.progressProperty());
                });
            }
        });
        //Sets the Cancel/Close button
        main.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            if (isRunning) {
                getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
                //Visibility for Progress Bars/Indicator
                progressIndicator.setVisible(!isProgressBar);
                workerProgressBar.setVisible(isProgressBar);
                workerLabel.setVisible(isProgressBar && showProgressNumber);
                if (isProgressBar && main.workers.size() > 1) {
                    totalProgressBar.setVisible(true);
                    totalLabel.setVisible(showProgressNumber);
                    numWorkersLabel.setVisible(showProgressNumber);
                    root.setPadding(new Insets(10, 0, 0, 0));
                } else {
                    totalLabel.setVisible(false);
                    numWorkersLabel.setVisible(false);
                    totalProgressBar.setVisible(false);
                    root.setPadding(new Insets(40, 0, 0, 0));
                }
            }
            else if (!implicitHiding) {
                label.setText("");
                getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
            }
        });
        totalProgressBar.progressProperty().bind(main.progressProperty());
          
        //Listener that starts (or cancels) the loading process
        showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                //If the window is showing, it restarts the Service (if it wasn't already running).
                if (!main.isRunning()) main.restart();
            } else { //If the window is hiding and the Service is still running, it is cancelled.
                if (main.isRunning()) main.cancel();
            }
        });
        //If all workers ended and the implicitHiding value is set to true, hide the stage
        main.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            if (!isRunning && implicitHiding) hide();
        });
    }
    
    //Getters
    public Label getLabel() { return label; }
    public ProgressIndicator getProgressIndicator() { return progressIndicator; }
    public ProgressBar getWorkerProgressBar() { return totalProgressBar; }
    public ProgressBar getTotalProgressBar() { return totalProgressBar; }
    public boolean isProgressBar() { return isProgressBar; }
    public boolean getImplicitHiding() { return implicitHiding; }
    public boolean getShowProgressNumber() { return showProgressNumber; }
    
    public LoadingService getLoadingService() { return main; }
    
    //Getting if the loading process succeeded or not
    public boolean isSucceeded() { return main.getState() == Worker.State.SUCCEEDED; }
    
    //Setters
    public void setProgressBar(boolean b) { isProgressBar = b; }
    public void showProgressNumber(boolean show) { showProgressNumber = show; }
    
    public void setImplicitHiding(boolean isHiding) {
        implicitHiding = isHiding;
        if (implicitHiding && !main.isRunning()) hide();
    }
}

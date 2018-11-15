/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myLibrary.javafx.Loading;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

/**
 * 
 * @author Alejandro
 */
public class LoadingDialog extends Dialog<Worker.State> {
    
    //Default width and height values (300 x 175)
    public static final double DEF_WIDTH = 300, DEF_HEIGHT = 175;
    
    //Stage elements
    private final VBox root = new VBox(10);
    private final GridPane progressPane = new GridPane();
    private final Label label = new Label("Loading..."), workerLabel = new Label(), numWorkersLabel = new Label(), totalLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ProgressBar workerProgressBar = new ProgressBar(), totalProgressBar = new ProgressBar();
    /**
     * isProgressBar: If true, the progressBar is shown instead of the progressIndicator (and the other way around)
     * implicitHiding: Whether the dialog should hide automatically when the loading service ends
     * showProgressNumber: Whether it should show the progress in a numerical form as well
     * showButton: Whether it should show the cancel/close button
     */
    private boolean isProgressBar = false, implicitHiding = true, showProgressNumber = false, showButton = false;
    //Label color (sets the text color bluish by default)
    private final Color color = Color.web("#3d81e3"); 
    
    //Listeners
    private final ChangeListener<Number> workerProgressListener;
    
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
        root.setPadding(new Insets(20, 0, 0, 0));
        
        progressPane.setAlignment(Pos.CENTER);
        progressPane.setVgap(5); progressPane.setHgap(10);
        
        GridPane.setConstraints(progressIndicator, 1, 0, 1, 1, HPos.CENTER, VPos.CENTER);
        GridPane.setConstraints(workerProgressBar, 1, 0, 1, 1, HPos.CENTER, VPos.CENTER);
        GridPane.setConstraints(workerLabel, 2, 0, 1, 1, HPos.LEFT, VPos.CENTER);
        GridPane.setConstraints(totalProgressBar, 1, 1, 1, 1, HPos.CENTER, VPos.CENTER);
        GridPane.setConstraints(numWorkersLabel, 0, 1, 1, 1, HPos.RIGHT, VPos.CENTER);
        GridPane.setConstraints(totalLabel, 2, 1, 1, 1, HPos.LEFT, VPos.CENTER);
        progressPane.getChildren().addAll(progressIndicator, workerProgressBar, totalProgressBar, numWorkersLabel, workerLabel, totalLabel);
        
        root.getChildren().addAll(label, progressPane);
        
        label.setTextFill(color);
        workerLabel.setTextFill(color);
        numWorkersLabel.setTextFill(color);
        totalLabel.setTextFill(color);
        
        //Listeners/bindings to the message and progress properties
        main.messageProperty().addListener((obs, oldMsg, newMsg) -> label.setText(newMsg)); 
        main.currentWorkerProperty().addListener((obs, oldWorker, newWorker) -> {
            progressIndicator.progressProperty().unbind();
            workerProgressBar.progressProperty().unbind();
            if (newWorker != null) {
                //A Worker's progressProperty can only be used from the FX Application Thread
                Platform.runLater(() -> {
                    if (isProgressBar) workerProgressBar.progressProperty().bind(newWorker.progressProperty());
                    else progressIndicator.progressProperty().bind(newWorker.progressProperty());
                });
            }
        });
        workerProgressListener = (obs, oldProg, newProg) -> {
            if (oldProg.doubleValue() < 0 && newProg.doubleValue() >= 0) {
                if (GridPane.getRowIndex(workerProgressBar) == 1) swapProgressRows();
                workerProgressBar.setVisible(true);
                workerLabel.setVisible(showProgressNumber);
                if (totalProgressBar.isVisible()) root.setPadding(new Insets(20, 0, 0, 0));
            } else if (totalProgressBar.isVisible() && showProgressNumber && oldProg.doubleValue() >= 0 && newProg.doubleValue() < 0) {
                if (GridPane.getRowIndex(workerProgressBar) == 0) swapProgressRows();
                workerProgressBar.setVisible(false);
                workerLabel.setVisible(false);
                root.setPadding(new Insets(45, 0, 0, 0));
            }
        };
        //Sets some properties when main starts/stops running
        main.runningProperty().addListener((obs, wasRunning, isRunning) -> {     
            if (!isRunning) {
                workerProgressBar.progressProperty().removeListener(workerProgressListener);
                //If all workers ended and implicitHiding is true (or main wasn't succeeded), hide the stage
                if (implicitHiding || !isSucceeded()) {
                    setResult(main.getState());
                    this.hide();
                } else if (!implicitHiding) {
                    label.setText("");
                    progressIndicator.progressProperty().unbind(); 
                    if (progressIndicator.isVisible()) progressIndicator.setProgress(1);
                    workerProgressBar.progressProperty().unbind(); 
                    if (workerProgressBar.isVisible()) workerProgressBar.setProgress(1);
                    if (showButton) getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
                }
            }
        });
        totalProgressBar.progressProperty().bind(main.progressProperty());
          
        //Listener that starts (or cancels) the loading process
        showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                //Sets the Cancel/Close button
                if (showButton) getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
                //Visibility for Progress Indicator
                progressIndicator.setVisible(!isProgressBar);
                workerProgressBar.setVisible(isProgressBar);
                totalProgressBar.setVisible(isProgressBar && main.workers.size() > 1);
                //Progress Labels bindings (It unbinds them by default, will bind them if conditions are met)
                workerLabel.textProperty().unbind();
                totalLabel.textProperty().unbind();
                numWorkersLabel.textProperty().unbind();
                //Total progress visibility + bindings
                if (isProgressBar) {
                    //Visibility Progress Labels
                    totalLabel.setVisible(showProgressNumber);
                    numWorkersLabel.setVisible(showProgressNumber);
                    workerLabel.setVisible(showProgressNumber);
                    //Bindings
                    workerProgressBar.progressProperty().addListener(workerProgressListener);
                    if (showProgressNumber) {
                        //It initializes as progress being indeterminate
                        workerLabel.setVisible(false);
                        if (totalProgressBar.isVisible()) {
                            workerProgressBar.setVisible(false);
                            if (GridPane.getRowIndex(workerProgressBar) == 0) swapProgressRows();
                            workerProgressBar.setVisible(false);
                            workerLabel.setVisible(false);
                            root.setPadding(new Insets(45, 0, 0, 0));
                        }
                        //Label bindings
                        workerLabel.textProperty().bind(Bindings.format("%.0f %%", workerProgressBar.progressProperty().multiply(100)));                        
                        if (totalProgressBar.isVisible()) {
                            totalLabel.textProperty().bind(Bindings.format("%.0f %%", totalProgressBar.progressProperty().multiply(100)));
                            numWorkersLabel.textProperty().bind(Bindings.format("%.0f/%.0f", main.workDoneProperty(), main.totalWorkProperty()));
                        }
                    }
                }
                //If the window is showing, it restarts the Service (if it wasn't already running).
                if (!main.isRunning()) main.restart();
            } else { //If the window is hiding and the Service is still running, it is cancelled.
                if (main.isRunning()) main.cancel();
            }
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
    public boolean getShowButton() { return showButton; }
    
    public LoadingService getLoadingService() { return main; }
    
    //Getting if the loading process succeeded or not
    public boolean isSucceeded() { return main.getState() == Worker.State.SUCCEEDED; }
    
    //Setters
    public void setProgressBar(boolean b) { isProgressBar = b; }
    public void showProgressNumber(boolean show) { showProgressNumber = show; }
    public void showButton(boolean show) { showButton = show; }
    
    public void setImplicitHiding(boolean isHiding) {
        implicitHiding = isHiding;
        if (implicitHiding && !main.isRunning()) {
            setResult(main.getState());
            this.hide();
        }
    }
    
    protected void swapProgressRows() { swapRows(progressPane, 0, 1); }
    //Static methods
    protected static final void swapRows(GridPane p, int x, int y) {
        for (Node node : p.getChildren()) {
            if (GridPane.getRowIndex(node) == x) { GridPane.setRowIndex(node, y); }
            else if (GridPane.getRowIndex(node) == y) { GridPane.setRowIndex(node, x); }
        }
    }
}

package com.simulator.gui;

import com.simulator.model.*;

import javafx.application.Platform;
/**
  * This is the controller class for the Dashboard.fxml file
  */
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class SmartHomeSimulatorController {

    @FXML private ToggleButton simulationToggle;
    @FXML private ToggleButton awayModeToggle;
    @FXML private Button editButton;
    @FXML private Button roomsControlPanelButton;
    @FXML private Button saveSecurity;
    @FXML private Button saveButton;
    @FXML private Label displayTemp;
    @FXML private Label displayDate;
    @FXML private Label userProfile;
    @FXML private Label userLocation;
    @FXML private Label displayTime;
    @FXML private TextArea outputConsole;

    @FXML private ListView allLightsListView;
    @FXML private ListView selectedLightsListView;
    @FXML private TextField startTimeSecurity;
    @FXML private TextField endTimeSecurity;
    @FXML private TextField motionDetectedTimeSecurity;

    @FXML private Label lastSaved;
    @FXML private TabPane tabPane;
    @FXML private Tab SHCTab;
    @FXML private RoomControlsController sHCTabPageController;
    

    private House house;
    private SimulationParameters simulation;
    private SecurityModule securityModule;
    private Timer timer = new Timer();
    Permission securityPermission;

    public SmartHomeSimulatorController()
    {
        this.house = House.getInstance();
        this.simulation = SimulationParameters.getInstance();
        securityPermission = new Permission(PERMISSION_TYPE.ALL, PERMISSION_TYPE.ALL, PERMISSION_TYPE.NONE, PERMISSION_TYPE.NONE);
    }

    @FXML
    public void initialize(){
        setTemperature(simulation.getTemperature());
        setDate(simulation.getDate());
        setLocation(simulation.getCurrentUser().getCurrentRoom());
        setProfile(simulation.getCurrentUser());
        setTime(simulation.getTime());
        setLights(house);
        Date currentDateTime = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lastSaved.setText(format.format(currentDateTime));
        startTimer();
        //creating a new instance of the logger with the output console so that other classes can use it
        Logger.newInstance(outputConsole);
        Logger.getInstance().resetLogFile();
        this.securityModule = new SecurityModule(simulation.getAllUsers(), awayModeToggle);
    }

    /**
     * Changes the simulation status to on or off
     * @param event Referring to a mouse activity by the user
     */
    @FXML
    void changeSimulationStatus(MouseEvent event) {
        if(simulation.getSimulationStatus()){
            this.simulationToggle.setText("On");
            simulation.setSimulationStatus(false);
            timer.cancel();
        } else {
            this.simulationToggle.setText("Off");
            simulation.setSimulationStatus(true);
            startTimer();
        }
    }

    /**
     * Opens the Edit Button from the dashboard.
     * @param event Referring to a mouse activity by the user
     * Calls in the SystemParameter controller to get the user desired values,
     * and changes the values on the dashboard.
     */
    @FXML
    void openEditor(MouseEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ParameterEditor.fxml"));
            Parent root1 = (Parent) fxmlLoader.load();
            Stage stage = new Stage();
            stage.initOwner(simulationToggle.getScene().getWindow());
            stage.setTitle("Edit Parameters");
            stage.setScene(new Scene(root1));  

            if(simulation.getSimulationStatus())
                timer.cancel();

            stage.showAndWait();
            
            if(simulation.getSimulationStatus())
                startTimer();

            setTemperature(simulation.getTemperature());
            setDate(simulation.getDate());
            setLocation(simulation.getCurrentUser().getCurrentRoom());
            setProfile(simulation.getCurrentUser());
            setTime(simulation.getTime());
        }
        catch (Exception e){
            e.printStackTrace();
        }  
    }

    @FXML
    public void changeAwayStatus() {
        securityModule.toggleAwayMode();
    }

    @FXML
    void addSecurityLight(){
        Object selectedItem = this.allLightsListView.getSelectionModel().getSelectedItem();
        this.selectedLightsListView.getItems().add((String) selectedItem);
        this.allLightsListView.getItems().remove(selectedItem);

        System.out.println((String) selectedItem);

        Light selectedLight = this.house.getLightByName((String) selectedItem);
        this.securityModule.addLight(selectedLight);
    }
    
    @FXML
    void removeSecurityLight(){

        Object selectedItem = this.selectedLightsListView.getSelectionModel().getSelectedItem();
        this.allLightsListView.getItems().add((String) selectedItem);
        this.selectedLightsListView.getItems().remove(selectedItem);

        System.out.println((String) selectedItem);

        Light selectedLight = this.house.getLightByName((String) selectedItem);
        this.securityModule.removeLight(selectedLight);
    }

    @FXML
    private void setTemperature(int temperature) {
        this.displayTemp.setText(Integer.toString(temperature) + "°C");
    }

    @FXML
    private void setDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd, YYYY");
        this.displayDate.setText(format.format(date));
    }

    @FXML
    private void setLocation(Room location) {
        this.userLocation.setText(location.getName());
    }

    @FXML
    private void setProfile(Profile profile) {
        this.userProfile.setText(profile.getName());
    }
    
    @FXML
    private void setTime(int time){
        String hours = String.format("%02d", time/60);
        String mins = String.format("%02d", time%60);
        this.displayTime.setText(hours + ":" + mins);
    }

    @FXML
    private void saveSecuritySettings(){
        if(securityPermission.checkPermission(simulation.getCurrentUser(), simulation.getCurrentUser().getCurrentRoom())) {
            String startTimeInput = this.startTimeSecurity.getText();
            String endTimeInput = this.endTimeSecurity.getText();
            String motionDetectedTimeInput = this.motionDetectedTimeSecurity.getText();
            try {
                int startTime = Integer.parseInt(startTimeInput);
                int endTime = Integer.parseInt(endTimeInput);
                int motionDetectedTime = Integer.parseInt(motionDetectedTimeInput);

                if (startTime > 1440 || startTime < 0 || endTime < 0 || endTime > 1440) {
                    throw new Exception("Time is not in the correct range");
                }

                this.securityModule.saveSettings(startTime, endTime, motionDetectedTime);
                Logger.getInstance().ouputToConsole("Security settings were successfully updated");
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid input");
                alert.setContentText("No information was saved! Please try again.");
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void setLights(House house){
        ArrayList<String> lightNameList = new ArrayList<String>();

        lightNameList = this.house.getLightsNameList();

        for (String light: lightNameList){
            allLightsListView.getItems().add(light);
        }
    }

    private void startTimer(){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    simulation.updateTime();
                    setTime(simulation.getTime());
                    setDate(simulation.getDate());
                });
            }
        }, simulation.getTimeInterval() ,simulation.getTimeInterval());
    }

    @FXML
    private void save(MouseEvent event){
        simulation.printToTxtFile();
        Date currentDateTime = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lastSaved.setText(format.format(currentDateTime));
    }

    @FXML
    protected ToggleButton getSimToggle(){
        return simulationToggle;
    }
}

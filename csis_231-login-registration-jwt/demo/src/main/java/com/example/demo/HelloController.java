package com.example.demo;

import com.example.demo.api.BackendClient;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML 
    private TableView<Customer> customerTable;
    
    @FXML
    private TableColumn<Customer, String> nameColumn;
    
    @FXML
    private TableColumn<Customer, String> emailColumn;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button editButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private Button refreshButton;

    @FXML
    private Button logoutButton;

    private BackendClient api;
    private AuthResponse authResponse;
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    public void setBackendClient(BackendClient backendClient) {
        this.api = backendClient;
    }

    public void setAuthResponse(AuthResponse authResponse) {
        this.authResponse = authResponse;
        if (welcomeText != null) {
            welcomeText.setText("Welcome, " + authResponse.getUsername() + "!");
        }
    }

    @FXML
    public void initialize(){
        // Configure table columns with custom cell value factories
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        emailColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        
        // Set table data
        customerTable.setItems(customers);
        
        // Configure column sorting
        nameColumn.setSortable(true);
        emailColumn.setSortable(true);
        
        // Initialize with default backend client if not set
        if (api == null) {
            api = new BackendClient();
        }
        
        reload();
    }

    @FXML
    protected void onAddCustomer() {
        showCustomerForm(null);
    }
    
    @FXML
    protected void onEditCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a customer to edit").showAndWait();
            return;
        }
        showCustomerForm(selected);
    }
    
    @FXML
    protected void onDeleteCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a customer to delete").showAndWait();
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Customer");
        confirmAlert.setContentText("Are you sure you want to delete " + selected.getName() + "?");
        
        if (confirmAlert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            try {
                api.deleteCustomer(selected.getId());
                reload();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Failed to delete customer: " + e.getMessage()).showAndWait();
            }
        }
    }
    
    @FXML
    protected void onRefresh() {
        reload();
    }

    @FXML
    protected void onLogout() {
        try {
            // Clear authentication token
            if (api != null) {
                api.clearAuthToken();
            }

            // Open login window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 400);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            // Close main window
            ((Stage) logoutButton.getScene().getWindow()).close();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to logout: " + e.getMessage()).showAndWait();
        }
    }

    private void showCustomerForm(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/customer-form.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            
            CustomerFormController controller = loader.getController();
            controller.setBackendClient(api);
            controller.setCustomer(customer);
            controller.setOnCustomerSaved(this::reload);
            
            Stage stage = new Stage();
            stage.setTitle(customer == null ? "Add Customer" : "Edit Customer");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open customer form: " + e.getMessage()).showAndWait();
        }
    }

    private void reload(){
        customers.clear();
        try{
            customers.addAll(api.fetchCustomers());
        }catch(Exception e){
            new Alert(Alert.AlertType.ERROR, "Failed to load customers "+e.getMessage()).showAndWait();
        }
    }
}

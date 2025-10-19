package com.example.demo;

import com.example.demo.api.BackendClient;
import com.example.demo.model.Customer;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CustomerFormController {
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    private BackendClient api;
    private Customer customer;
    private boolean isEditMode = false;
    private Runnable onCustomerSaved;
    
    public void setBackendClient(BackendClient backendClient) {
        this.api = backendClient;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
        this.isEditMode = customer != null;
        
        if (isEditMode) {
            nameField.setText(customer.getName());
            emailField.setText(customer.getEmail());
            saveButton.setText("Update");
        } else {
            nameField.clear();
            emailField.clear();
            saveButton.setText("Create");
        }
    }
    
    public void setOnCustomerSaved(Runnable callback) {
        this.onCustomerSaved = callback;
    }
    
    @FXML
    private void onSave() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        
        if (name.isEmpty() || email.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Please fill in all fields").showAndWait();
            return;
        }
        
        try {
            if (isEditMode) {
                customer.setName(name);
                customer.setEmail(email);
                api.updateCustomer(customer.getId(), customer);
            } else {
                Customer newCustomer = new Customer();
                newCustomer.setName(name);
                newCustomer.setEmail(email);
                api.createCustomer(newCustomer);
            }
            
            if (onCustomerSaved != null) {
                onCustomerSaved.run();
            }
            
            closeForm();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save customer: " + e.getMessage()).showAndWait();
        }
    }
    
    @FXML
    private void onCancel() {
        closeForm();
    }
    
    private void closeForm() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}

package com.example.demo.controllers;


import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.model.RegisterRequest;
import com.example.demo.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;


public class RegisterController {
    @FXML private TextField username;
    @FXML private TextField email;
    @FXML private PasswordField password;
   @FXML private TextField firstName;
   @FXML private TextField lastName;



    public void onRegister() {
        try {
            String u = safe(username); String e = safe(email);
            String p = safe(password); String c = safe(firstName); String f = safe(lastName);
            if (u.isEmpty() || e.isEmpty() || p.isEmpty() || c.isEmpty()) { AlertUtils.warn("Fill all fields."); return; }
            if (!p.equals(c)) { AlertUtils.warn("Passwords do not match."); return; }
            AuthApi.register(new RegisterRequest(u, e, p , f,c));
            AlertUtils.info("Account created. Please login.");
            Launcher.go("login.fxml", "Login");
        } catch (Exception ex) { AlertUtils.error("Registration failed: " + ex.getMessage()); }
    }


    public void goLogin(){ Launcher.go("login.fxml", "Login"); }


    private static String safe(TextField t){ return t.getText()==null?"":t.getText().trim(); }
    private static String safe(PasswordField t){ return t.getText()==null?"":t.getText().trim(); }
}
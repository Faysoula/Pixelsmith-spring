package com.example.pixelsmith;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class Login extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Email and Password fields
        TextField emailField = new TextField();
        emailField.setPromptText("EMAIL");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("PASSWORD");

        Button loginButton = new Button("Log In");
        loginButton.setOnAction(e -> {
            String email = emailField.getText();
            String enteredPassword = passwordField.getText();
            try {
                if (authenticate(email, enteredPassword)) {
                    Integer userId = retrieveUserId(email);
                    if (userId != null) {
                        UserSession.setCurrentUserId(userId); // Set the user ID
                        new PixelArtEditor().start(new Stage());
                        primaryStage.close();
                    } else {
                        showAlert("Login Failed", "User ID not found.");
                    }
                } else {
                    showAlert("Login Failed", "Invalid email or password.");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", ex.getMessage());
            }
        });

        Label titleLabel = new Label("SMITH IN");
        GridPane.setHalignment(titleLabel, HPos.CENTER);

        VBox layout = new VBox(10, titleLabel,emailField, passwordField, loginButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Login");
        primaryStage.show();

        emailField.getStyleClass().add("text-field");
        passwordField.getStyleClass().add("text-field");
        loginButton.getStyleClass().add("button");
        titleLabel.getStyleClass().add("title");

        // Set up the layout
        layout.getStyleClass().add("vbox");

        // Create the scene and apply the CSS
        scene.getStylesheets().add("login.css");

        primaryStage.setScene(scene);
        primaryStage.setTitle("Login");
        primaryStage.show();
    }

    private boolean authenticate(String email, String password) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/pixelsmith"
                , "root", "13102004");
        String query = "SELECT PasswordHash FROM users WHERE Email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPasswordHash = rs.getString("PasswordHash");
                    return BCrypt.checkpw(password, storedPasswordHash);
                }
            }
        }
        return false;
    }

    private Integer retrieveUserId(String email) {
        String query = "SELECT UserId FROM users WHERE Email = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/pixelsmith", "root", "13102004");
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UserId");
                }
            }
        } catch (SQLException ex) {
            showAlert("Database Error", ex.getMessage());
        }
        return null; // Return null if user ID not found or in case of an exception
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

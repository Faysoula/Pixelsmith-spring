package com.example.pixelsmith;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class SignUpApp extends Application {
    private static final String DB_URL = "jdbc:mysql://localhost:3307/pixelsmith";
    private static final String USER = "root";
    private static final String PASS = "13102004";

    @Override
    public void start(Stage primaryStage) {

        // Create the username, email and password fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("USERNAME");
        TextField emailField = new TextField();
        emailField.setPromptText("EMAIL");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("PASSWORD");

        // Create the sign-up button
        Button signUpButton = new Button("DIVE IN");
        signUpButton.setOnAction(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();

            // Hash the password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert user into the database
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String query = "INSERT INTO users (Username, Email, PasswordHash) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, email);
                    pstmt.setString(3, hashedPassword);
                    pstmt.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Sign Up Successful!", "User has been registered.");
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
                }
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Database Connection Failed", ex.getMessage());
            }
        });



        VBox formLayout = new VBox(10); // Adjust the spacing if needed
        formLayout.setAlignment(Pos.CENTER);
        formLayout.getChildren().addAll(usernameField, emailField, passwordField);

// Add a title label that spans two columns
        Label titleLabel = new Label("PIXEL SMITH");
        GridPane.setHalignment(titleLabel, HPos.CENTER); // To center the label

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.getStyleClass().add("grid"); // Apply the grid style class
        grid.add(titleLabel, 0, 0);
        grid.add(formLayout, 0, 1);
        grid.add(signUpButton, 0, 2);
        GridPane.setHalignment(titleLabel, HPos.CENTER);
        GridPane.setHalignment(signUpButton, HPos.CENTER);

        // Apply styles from the external CSS
        titleLabel.getStyleClass().add("label");
        usernameField.getStyleClass().add("text-field");
        emailField.getStyleClass().add("text-field");
        passwordField.getStyleClass().add("text-field");
        signUpButton.getStyleClass().add("button");


// Set the scene with the CSS file
        Scene scene = new Scene(grid, 300, 275);
        scene.getStylesheets().add("signup.css");


// Set the scene
        primaryStage.setScene(scene);
        primaryStage.setTitle("Sign Up");
        primaryStage.show();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

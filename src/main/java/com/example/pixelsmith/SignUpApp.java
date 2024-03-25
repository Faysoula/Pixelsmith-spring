package com.example.pixelsmith;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
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



        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)*(\\.[a-zA-Z]{2,})$")) {
                emailField.setStyle("-fx-border-color: red ; -fx-border-width: 2px ;");
            } else {
                emailField.setStyle("");
            }
        });


        Button loginButton = new Button("Already a PixelSmith?");
        loginButton.setOnAction(e -> openLoginWindow());
        loginButton.getStyleClass().add("button");

        // Create the sign-up button
        Button signUpButton = new Button("DIVE IN");
        signUpButton.setOnAction(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();

            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

            // Check if the email is valid
            if (!email.matches(emailRegex)) {
                showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return;
            }


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
                    openLoginWindow();
                    primaryStage.close();
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
                }
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Database Connection Failed", ex.getMessage());
            }
        });

        HBox buttonLayout = new HBox(10);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.getChildren().addAll(signUpButton, loginButton);

        VBox formLayout = new VBox(10);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.getChildren().addAll(usernameField, emailField, passwordField,buttonLayout);

        Label titleLabel = new Label("PIXEL SMITH");
        GridPane.setHalignment(titleLabel, HPos.CENTER);

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

        Scene scene = new Scene(grid, 300, 275);
        scene.getStylesheets().add("signup.css");

        primaryStage.setScene(scene);
        primaryStage.setTitle("Sign Up");
        Image applicationIcon = new Image("icon.png");
        primaryStage.getIcons().add(applicationIcon);
        primaryStage.show();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openLoginWindow() {
        new Login().start(new Stage());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

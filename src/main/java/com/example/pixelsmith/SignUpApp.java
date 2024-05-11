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
import javafx.stage.Stage;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class SignUpApp extends Application {
    private static final String BASE_URL = "http://localhost:8080/api/users";

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
            if (!newValue.matches("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[a-zA-Z]{2,})$")) {
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

            try {
                if (signup(username, email, password)) {
                    openLoginWindow();
                    primaryStage.close();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Signup Failed", "Signup failed. Please try again.");
                }
            } catch (Exception ex) {
                ex.printStackTrace(); // Log the exception for debugging
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        HBox buttonLayout = new HBox(10);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.getChildren().addAll(signUpButton, loginButton);

        VBox formLayout = new VBox(10);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.getChildren().addAll(usernameField, emailField, passwordField, buttonLayout);

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

        Scene scene = new Scene(grid, 400, 300);
        scene.getStylesheets().add("signup.css");

        primaryStage.setScene(scene);
        primaryStage.setTitle("Sign Up");
        Image applicationIcon = new Image("icon.png");
        primaryStage.getIcons().add(applicationIcon);
        primaryStage.show();
    }

    private boolean signup(String username, String email, String password) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String url = "http://localhost:8080/api/users/signup";
        JSONObject requestBody = new JSONObject();
        requestBody.put("username", username);
        requestBody.put("email", email);
        requestBody.put("passwordhash", password);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response JSON: " + response.body());
        if (response.statusCode() != 200) {
            System.out.println("Response Code: " + response.statusCode());  // Debugging output
            System.out.println("Response Body: " + response.body());        // Debugging output
        }
        return response.statusCode() == 200;
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

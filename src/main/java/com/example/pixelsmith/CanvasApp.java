package com.example.pixelsmith;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class CanvasApp extends Application {

    private static final int CANVAS_WIDTH = 401;
    private static final int CANVAS_HEIGHT = 400;
    private static final int GRID_SIZE = 16; // This is the size of each square in the grid

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawGrid(gc);

        // Color Picker for selecting pen color
        ColorPicker colorPicker = new ColorPicker(Color.BLACK);

        // Toolbar with drawing tools, adding just the pen tool for now
        ToolBar toolBar = new ToolBar();
        ToggleButton penTool = new ToggleButton("Pen");
        toolBar.getItems().addAll(penTool, colorPicker);

        // Add the toolbar on the left and canvas in the center
        root.setLeft(toolBar);
        root.setCenter(canvas);

        Runnable draw = () -> {
            if(penTool.isSelected()){
                double x = Math.floor(canvas.getMousePosition().getX() / GRID_SIZE) * GRID_SIZE;
                double y = Math.floor(canvas.getMousePosition().getY() / GRID_SIZE) * GRID_SIZE;
                gc.setFill(colorPicker.getValue());
                gc.fillRect(x,y,GRID_SIZE, GRID_SIZE);
            }
        };
        canvas.setOnMouseClicked(e->draw.run());
        canvas.setOnMouseDragged(e->draw.run());

        // Scene with styling
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Canvas Page");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);

        // Draw vertical lines
        for (int i = 0; i < CANVAS_WIDTH; i += GRID_SIZE) {
            gc.strokeLine(i, 0, i, CANVAS_HEIGHT);
        }

        // Draw horizontal lines
        for (int j = 0; j < CANVAS_HEIGHT; j += GRID_SIZE) {
            gc.strokeLine(0, j, CANVAS_WIDTH, j);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

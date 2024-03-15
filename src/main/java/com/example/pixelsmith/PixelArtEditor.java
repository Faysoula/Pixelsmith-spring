package com.example.pixelsmith;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PixelArtEditor extends Application {

    private static final int CANVAS_WIDTH = 400;
    private static final int CANVAS_HEIGHT = 400;
    private static final int GRID_SIZE = 16;
    private static final int ROWS = CANVAS_HEIGHT / GRID_SIZE;
    private static final int COLS = CANVAS_WIDTH / GRID_SIZE;
    private final Color[][] pixels = new Color[ROWS][COLS];
    private GraphicsContext gc;
    private ColorPicker colorPicker;
    private ColorPicker backgroundColorPicker;
    private Tool currentTool;

    // Tool interface
    interface Tool {
        void apply(int row, int col);
    }

    // Pen tool
    class PenTool implements Tool {
        public void apply(int row, int col) {
            pixels[row][col] = colorPicker.getValue();
            renderPixel(row, col);
        }
    }

    // Eraser tool
    class EraserTool implements Tool {
        public void apply(int row, int col) {
            // Determine the original checkerboard color based on the position
            Color originalColor = ((row + col) % 2 == 0) ? Color.rgb(160, 160, 160) : Color.rgb(96, 96, 96);
            pixels[row][col] = originalColor;
            renderPixel(row, col);
        }
    }
    class FillTool implements Tool {
        public void apply(int row, int col) {
            Color targetColor = pixels[row][col];
            fill(row, col, targetColor, colorPicker.getValue());
        }

        private void fill(int row, int col, Color targetColor, Color replacementColor) {
            if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
            if (!pixels[row][col].equals(targetColor) || pixels[row][col].equals(replacementColor)) return;

            pixels[row][col] = replacementColor;
            renderPixel(row, col);

            fill(row - 1, col, targetColor, replacementColor);
            fill(row + 1, col, targetColor, replacementColor);
            fill(row, col - 1, targetColor, replacementColor);
            fill(row, col + 1, targetColor, replacementColor);
        }
    }

    // Initialize the grid with the default color
    private void initializeGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if ((row + col) % 2 == 0) {
                    pixels[row][col] = Color.rgb(160, 160, 160); // Light check
                } else {
                    pixels[row][col] = Color.rgb(96, 96, 96); // Dark check
                }
            }
        }
    }

    // Render the grid based on the pixel data structure
    private void renderGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                renderPixel(row, col);
            }
        }
    }

    // Render a single pixel
    private void renderPixel(int row, int col) {
        if (pixels[row][col] == Color.TRANSPARENT) {
            gc.clearRect(col * GRID_SIZE, row * GRID_SIZE, GRID_SIZE, GRID_SIZE);
        } else {
            gc.setFill(pixels[row][col]);
            gc.fillRect(col * GRID_SIZE, row * GRID_SIZE, GRID_SIZE, GRID_SIZE);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        initializeGrid();
        BorderPane root = new BorderPane();
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        renderGrid();

        // Initialize color pickers
        colorPicker = new ColorPicker(Color.BLACK);
        backgroundColorPicker = new ColorPicker(Color.TRANSPARENT);

        // Initialize toolbar and tools
        ToolBar toolBar = new ToolBar();
        ToggleGroup toolsGroup = new ToggleGroup();

        ToggleButton penToolButton = new ToggleButton("Pen");
        ToggleButton eraserToolButton = new ToggleButton("Eraser");

        penToolButton.setToggleGroup(toolsGroup);
        eraserToolButton.setToggleGroup(toolsGroup);

        penToolButton.setSelected(true); // Pen tool is selected by default
        currentTool = new PenTool(); // Default tool

        penToolButton.setOnAction(e -> currentTool = new PenTool());
        eraserToolButton.setOnAction(e -> currentTool = new EraserTool());

        toolBar.getItems().addAll(penToolButton, eraserToolButton, colorPicker, backgroundColorPicker);

        // Add the toolbar on the left and canvas in the center
        root.setLeft(toolBar);
        root.setCenter(canvas);

        // Handle the drawing on canvas
        canvas.setOnMouseClicked(e -> applyTool(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> applyTool(e.getX(), e.getY()));

        // Scene with styling
        Scene scene = new Scene(root, 600, 450);
        primaryStage.setTitle("Pixel Art Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void applyTool(double x, double y) {
        int col = (int) x / GRID_SIZE;
        int row = (int) y / GRID_SIZE;

        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            currentTool.apply(row, col);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

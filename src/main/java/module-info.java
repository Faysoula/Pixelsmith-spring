module com.example.pixelsmith {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;


    opens com.example.pixelsmith to javafx.fxml;
    exports com.example.pixelsmith;
}
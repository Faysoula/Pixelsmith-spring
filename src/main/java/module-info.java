module com.example.pixelsmith {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires java.desktop;
    requires javafx.swing;
    requires java.net.http;
    requires org.json;


    opens com.example.pixelsmith to javafx.fxml;
    exports com.example.pixelsmith;
}
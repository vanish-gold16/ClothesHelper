module com.example.clotheshelper {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.clotheshelper to javafx.fxml;
    exports com.example.clotheshelper;
}
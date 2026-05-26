package com.example.clotheshelper;

import com.example.clotheshelper.ui.AppRoot;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClothesHelperApplication extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new AppRoot(stage), 850, 600);

        stage.setTitle("ClothesHelper");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

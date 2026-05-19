package com.example.clotheshelper;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClothesHelperApplication extends Application {
    private static final String DEFAULT_BUTTON_STYLE = "-fx-background-color: transparent;"
            + "-fx-text-fill: #4b5563;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-radius: 0;";

    private static final String ACTIVE_BUTTON_STYLE = "-fx-background-color: #2563eb;"
            + "-fx-text-fill: white;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-radius: 0;";

    private final StackPane pageContainer = new StackPane();
    private final Map<String, VBox> pages = new LinkedHashMap<>();
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setCenter(pageContainer);
        root.setBottom(createNavigation());

        addPage("Главная", "Главная", "Здесь будет основной экран приложения.");
        addPage("Одежда", "Одежда", "Здесь позже появится список вещей.");
        addPage("Образы", "Образы", "Здесь можно будет собирать комплекты.");

        selectPage("Главная");

        Scene scene = new Scene(root, 800, 600);

        stage.setTitle("ClothesHelper");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private HBox createNavigation() {
        HBox navigation = new HBox();
        navigation.setAlignment(Pos.CENTER);
        navigation.setPadding(new Insets(0));
        navigation.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #d1d5db; -fx-border-width: 1 0 0 0;");

        createNavigationButton(navigation, "Главная");
        createNavigationButton(navigation, "Одежда");
        createNavigationButton(navigation, "Образы");

        return navigation;
    }

    private void createNavigationButton(HBox navigation, String pageName) {
        Button button = new Button(pageName);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(DEFAULT_BUTTON_STYLE);
        button.setOnAction(event -> selectPage(pageName));

        HBox.setHgrow(button, Priority.ALWAYS);
        navigation.getChildren().add(button);
        navigationButtons.put(pageName, button);
    }

    private void addPage(String pageName, String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4b5563;");

        VBox page = new VBox(12, titleLabel, descriptionLabel);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(32));
        page.setStyle("-fx-background-color: white;");

        pages.put(pageName, page);
    }

    private void selectPage(String pageName) {
        VBox page = pages.get(pageName);
        if (page == null) {
            return;
        }

        pageContainer.getChildren().setAll(page);

        for (Map.Entry<String, Button> entry : navigationButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(pageName);
            entry.getValue().setStyle(isActive ? ACTIVE_BUTTON_STYLE : DEFAULT_BUTTON_STYLE);
        }
    }
}

package com.example.clotheshelper.ui;

import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.ui.pages.AddItemPage;
import com.example.clotheshelper.ui.pages.EditItemPage;
import com.example.clotheshelper.ui.pages.LibraryPage;
import com.example.clotheshelper.ui.pages.SimplePage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppRoot extends BorderPane {
    private static final double NAVIGATION_HEIGHT = 64;

    private static final String DEFAULT_BUTTON_STYLE = "-fx-background-color: transparent;"
            + "-fx-text-fill: #4b5563;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-insets: 0;"
            + "-fx-border-width: 0;"
            + "-fx-background-radius: 0;";

    private static final String ACTIVE_BUTTON_STYLE = "-fx-background-color: #2563eb;"
            + "-fx-text-fill: white;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-insets: 0;"
            + "-fx-border-width: 0;"
            + "-fx-background-radius: 0;";

    private final StackPane pageContainer = new StackPane();
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();
    private LibraryPage libraryPage;

    public AppRoot(Stage owner) {
        setCenter(pageContainer);
        setBottom(createNavigation());
        createPages(owner);
        selectPage("Home");
    }

    private void createPages(Stage owner) {
        pages.put("Home", new SimplePage("Home", "This will be the main screen of the app."));
        libraryPage = new LibraryPage(item -> showEditPage(owner, item));
        pages.put("Library", libraryPage);
        pages.put("Add", new AddItemPage(owner));
        pages.put("Settings", new SimplePage("Settings", "App settings will appear here."));
        pages.put("Profile", new SimplePage("Profile", "Your profile information will appear here."));
    }

    private void showEditPage(Stage owner, SavedClothingItem item) {
        EditItemPage editItemPage = new EditItemPage(owner, item, () -> selectPage("Library"), this::refreshLibrary);
        pageContainer.getChildren().setAll(editItemPage);
        updateNavigation("Library");
    }

    private void refreshLibrary() {
        if (libraryPage != null) {
            libraryPage.refreshItems();
        }
    }

    private HBox createNavigation() {
        HBox navigation = new HBox();
        navigation.setAlignment(Pos.CENTER);
        navigation.setFillHeight(true);
        navigation.setMinHeight(NAVIGATION_HEIGHT);
        navigation.setPrefHeight(NAVIGATION_HEIGHT);
        navigation.setPadding(new Insets(0));
        navigation.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #d1d5db; -fx-border-width: 1 0 0 0;");

        createNavigationButton(navigation, "Home", false);
        createNavigationButton(navigation, "Library", false);
        createNavigationButton(navigation, "Add", true);
        createNavigationButton(navigation, "Settings", false);
        createNavigationButton(navigation, "Profile", false);

        return navigation;
    }

    private void createNavigationButton(HBox navigation, String pageName, boolean hasPlusIcon) {
        Button button = new Button(pageName);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(NAVIGATION_HEIGHT);
        button.setPrefHeight(NAVIGATION_HEIGHT);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setStyle(DEFAULT_BUTTON_STYLE);
        button.setOnAction(event -> selectPage(pageName));

        if (hasPlusIcon) {
            Label plusIcon = new Label("+");
            plusIcon.setStyle(createPlusIconStyle(false));
            button.setGraphic(plusIcon);
            button.setContentDisplay(ContentDisplay.TOP);
            button.setGraphicTextGap(2);
        }

        HBox.setHgrow(button, Priority.ALWAYS);
        navigation.getChildren().add(button);
        navigationButtons.put(pageName, button);
    }

    private void selectPage(String pageName) {
        Node page = pages.get(pageName);
        if (page == null) {
            return;
        }

        if (page instanceof LibraryPage libraryPage) {
            libraryPage.refreshItems();
        }

        pageContainer.getChildren().setAll(page);
        updateNavigation(pageName);
    }

    private void updateNavigation(String activePageName) {
        for (Map.Entry<String, Button> entry : navigationButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(activePageName);
            Button button = entry.getValue();
            button.setStyle(isActive ? ACTIVE_BUTTON_STYLE : DEFAULT_BUTTON_STYLE);

            if (button.getGraphic() instanceof Label plusIcon) {
                plusIcon.setStyle(createPlusIconStyle(isActive));
            }
        }
    }

    private String createPlusIconStyle(boolean isActive) {
        String iconColor = isActive ? "white" : "#4b5563";
        return "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + iconColor + ";";
    }
}

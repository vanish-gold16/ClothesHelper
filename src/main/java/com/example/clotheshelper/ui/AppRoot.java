package com.example.clotheshelper.ui;

import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.ui.pages.AddItemPage;
import com.example.clotheshelper.ui.pages.EditItemPage;
import com.example.clotheshelper.ui.pages.LibraryPage;
import com.example.clotheshelper.ui.pages.SettingsPage;
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
import java.util.prefs.Preferences;

public class AppRoot extends BorderPane {
    private static final double NAVIGATION_HEIGHT = 64;
    private static final String THEME_PREFERENCE_KEY = "theme";

    private static final String DEFAULT_BUTTON_STYLE = "-fx-background-color: transparent;"
            + "-fx-text-fill: -app-muted-text;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-insets: 0;"
            + "-fx-border-width: 0;"
            + "-fx-background-radius: 0;";

    private static final String ACTIVE_BUTTON_STYLE = "-fx-background-color: -app-primary;"
            + "-fx-text-fill: -app-on-primary;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 12 16;"
            + "-fx-background-insets: 0;"
            + "-fx-border-width: 0;"
            + "-fx-background-radius: 0;";

    private final StackPane pageContainer = new StackPane();
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();
    private final Preferences preferences = Preferences.userNodeForPackage(AppRoot.class);
    private HBox navigation;
    private AppTheme currentTheme;
    private String activePageName = "Home";
    private LibraryPage libraryPage;

    public AppRoot(Stage owner) {
        currentTheme = loadTheme();
        setCenter(pageContainer);
        navigation = createNavigation();
        setBottom(navigation);
        createPages(owner);
        applyTheme(currentTheme);
        selectPage("Home");
    }

    private void createPages(Stage owner) {
        pages.put("Home", new SimplePage("Home", "This will be the main screen of the app."));
        libraryPage = new LibraryPage(item -> showEditPage(owner, item));
        pages.put("Library", libraryPage);
        pages.put("Add", new AddItemPage(owner));
        pages.put("Settings", new SettingsPage(currentTheme, this::setTheme));
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
        navigation.setStyle(createNavigationStyle());

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
        this.activePageName = activePageName;
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
        String iconColor = isActive ? "-app-on-primary" : "-app-muted-text";
        return "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + iconColor + ";";
    }

    private void setTheme(AppTheme theme) {
        currentTheme = theme;
        preferences.put(THEME_PREFERENCE_KEY, theme.name());
        applyTheme(theme);
    }

    private void applyTheme(AppTheme theme) {
        setStyle(theme.createRootStyle());
        if (navigation != null) {
            navigation.setStyle(createNavigationStyle());
        }
        updateNavigation(activePageName);
    }

    private String createNavigationStyle() {
        return "-fx-background-color: -app-muted-surface;"
                + "-fx-border-color: -app-border;"
                + "-fx-border-width: 1 0 0 0;";
    }

    private AppTheme loadTheme() {
        String themeName = preferences.get(THEME_PREFERENCE_KEY, AppTheme.LIGHT.name());
        try {
            return AppTheme.valueOf(themeName);
        } catch (IllegalArgumentException exception) {
            return AppTheme.LIGHT;
        }
    }
}

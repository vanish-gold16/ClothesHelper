package com.example.clotheshelper.ui;

import com.example.clotheshelper.storage.SavedClothingItem;
import com.example.clotheshelper.ui.pages.AddItemPage;
import com.example.clotheshelper.ui.pages.EditItemPage;
import com.example.clotheshelper.ui.pages.HomePage;
import com.example.clotheshelper.ui.pages.LibraryPage;
import com.example.clotheshelper.ui.pages.ProfilePage;
import com.example.clotheshelper.ui.pages.SettingsPage;
import com.example.clotheshelper.ui.styles.UiStyles;
import com.example.clotheshelper.ui.theme.AppTheme;
import com.example.clotheshelper.ui.theme.ThemePreferences;
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
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The application shell: holds the bottom navigation, swaps between the pages and
 * applies the current colour theme across the window and its popups.
 */
public class AppRoot extends BorderPane {
    private static final double NAVIGATION_HEIGHT = 64;

    private final StackPane pageContainer = new StackPane();
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();
    private final ThemePreferences themePreferences = new ThemePreferences(AppRoot.class);
    private HBox navigation;
    private AppTheme currentTheme;
    private String activePageName = "Home";
    private LibraryPage libraryPage;
    private String themeStylesheetUri;

    public AppRoot(Stage owner) {
        currentTheme = themePreferences.load();
        setCenter(pageContainer);
        navigation = createNavigation();
        setBottom(navigation);
        createPages(owner);
        applyTheme(currentTheme);
        sceneProperty().addListener((observable, oldScene, newScene) -> publishThemeVariables(newScene));
        selectPage("Home");
    }

    private void createPages(Stage owner) {
        HomePage homePage = new HomePage();
        pages.put("Home", homePage);
        libraryPage = new LibraryPage(item -> showEditPage(owner, item));
        pages.put("Library", libraryPage);
        pages.put("Add", new AddItemPage(owner));
        pages.put("Settings", new SettingsPage(currentTheme, this::setTheme));
        pages.put("Profile", new ProfilePage());
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
        button.setStyle(UiStyles.NAVIGATION_BUTTON);
        button.setOnAction(event -> selectPage(pageName));

        if (hasPlusIcon) {
            Label plusIcon = new Label("+");
            plusIcon.setStyle(UiStyles.navigationIcon(false));
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
        if (page instanceof HomePage homePage) {
            homePage.refreshWeather();
        }
        if (page instanceof ProfilePage profilePage) {
            profilePage.refreshStats();
        }

        pageContainer.getChildren().setAll(page);
        updateNavigation(pageName);
    }

    private void updateNavigation(String activePageName) {
        this.activePageName = activePageName;
        for (Map.Entry<String, Button> entry : navigationButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(activePageName);
            Button button = entry.getValue();
            button.setStyle(isActive ? UiStyles.ACTIVE_NAVIGATION_BUTTON : UiStyles.NAVIGATION_BUTTON);

            if (button.getGraphic() instanceof Label plusIcon) {
                plusIcon.setStyle(UiStyles.navigationIcon(isActive));
            }
        }
    }

    private void setTheme(AppTheme theme) {
        currentTheme = theme;
        themePreferences.save(theme);
        applyTheme(theme);
    }

    private void applyTheme(AppTheme theme) {
        setStyle(theme.createRootStyle());
        publishThemeVariables(getScene());
        if (navigation != null) {
            navigation.setStyle(createNavigationStyle());
        }
        updateNavigation(activePageName);
    }

    private void publishThemeVariables(Scene scene) {
        if (scene == null) {
            return;
        }

        if (themeStylesheetUri != null) {
            scene.getStylesheets().remove(themeStylesheetUri);
        }

        String css = ".root {" + currentTheme.createRootVariables() + "}";
        themeStylesheetUri = "data:text/css;base64,"
                + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        scene.getStylesheets().add(themeStylesheetUri);
    }

    private String createNavigationStyle() {
        return UiStyles.NAVIGATION_BAR;
    }

}

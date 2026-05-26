package com.example.clotheshelper.ui.pages;

import com.example.clotheshelper.ui.components.PageHeader;
import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.Locale;

public class ProfilePage extends ScrollPane {
    private static final double LAYOUT_MAX_WIDTH = 760;
    private static final double AVATAR_SIZE = 132;
    private static final String AVATAR_RESOURCE = "/com/example/clotheshelper/ui/assets/pfp.png";

    private final String displayName = createDisplayName();

    public ProfilePage() {
        VBox pageContent = new VBox(24,
                new PageHeader("Profile", "Your visible profile information.", LAYOUT_MAX_WIDTH),
                createProfileCard()
        );
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(32));
        pageContent.setStyle(UiStyles.PAGE_BACKGROUND);

        setContent(pageContent);
        UiStyles.configurePageScrollPane(this);
    }

    private VBox createProfileCard() {
        Label cardTitle = new Label("Public profile");
        cardTitle.setStyle(UiStyles.CARD_TITLE);

        Label fieldLabel = new Label("Display name");
        fieldLabel.setStyle(UiStyles.FIELD_LABEL);

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 22px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: -app-text;");

        VBox nameBlock = new VBox(6, fieldLabel, nameLabel);
        nameBlock.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, cardTitle, createAvatar(), nameBlock);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(22));
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setStyle(UiStyles.CARD);
        return card;
    }

    private Node createAvatar() {
        URL avatarUrl = ProfilePage.class.getResource(AVATAR_RESOURCE);
        if (avatarUrl == null) {
            return createAvatarPlaceholder();
        }

        ImageView imageView = new ImageView(new Image(avatarUrl.toExternalForm(), AVATAR_SIZE, AVATAR_SIZE, false, true));
        imageView.setFitWidth(AVATAR_SIZE);
        imageView.setFitHeight(AVATAR_SIZE);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setClip(new Circle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, AVATAR_SIZE / 2));
        return imageView;
    }

    private Node createAvatarPlaceholder() {
        Label initialLabel = new Label(displayName.substring(0, 1).toUpperCase(Locale.ROOT));
        initialLabel.setStyle("-fx-font-size: 46px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: -app-on-primary;");

        StackPane avatar = new StackPane(initialLabel);
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setMaxSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setStyle("-fx-background-color: -app-primary;"
                + "-fx-background-radius: 999;"
                + "-fx-border-color: -app-border;"
                + "-fx-border-radius: 999;");
        return avatar;
    }

    private String createDisplayName() {
        String userName = System.getProperty("user.name", "User").trim();
        if (userName.isBlank()) {
            return "User";
        }
        return userName.substring(0, 1).toUpperCase(Locale.ROOT) + userName.substring(1);
    }
}

package com.example.clotheshelper.ui.components;

import com.example.clotheshelper.enums.MainColor;
import com.example.clotheshelper.ui.styles.UiStyles;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class MainColorCell extends ListCell<MainColor> {
    @Override
    protected void updateItem(MainColor color, boolean empty) {
        super.updateItem(color, empty);

        if (empty || color == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        Region swatch = new Region();
        swatch.setPrefSize(14, 14);
        swatch.setStyle(UiStyles.swatch(color.getHex()));

        Label label = new Label(color.toString());
        label.setStyle(UiStyles.TEXT);

        HBox content = new HBox(8, swatch, label);
        content.setAlignment(Pos.CENTER_LEFT);
        setText(null);
        setGraphic(content);
    }
}

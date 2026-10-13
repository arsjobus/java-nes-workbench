package com.nesworkbench;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Initial JavaFX shell. ROM opening and editing workflows are planned features. */
public final class App extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("NES Asset Workbench");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("Local-first tools for inspecting and editing NES graphics");
        subtitle.setStyle("-fx-text-fill: #666666;");
        VBox heading = new VBox(6, title, subtitle);
        heading.setPadding(new Insets(18));

        Label emptyState = new Label("Open a NES ROM to begin.\n\n"
                + "The starter includes a tested iNES 1.0 parser and CHR tile decoder.\n"
                + "ROM opening, tile browsing, and editing controls are upcoming roadmap items.");
        emptyState.setWrapText(true);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setStyle("-fx-font-size: 14px;");

        StackPane workspace = new StackPane(emptyState);
        workspace.setPadding(new Insets(24));
        workspace.setStyle("-fx-background-color: #f4f4f4;");
        Label status = new Label("Ready — no ROM loaded");
        status.setPadding(new Insets(10, 14, 10, 14));

        BorderPane root = new BorderPane();
        root.setTop(new VBox(0, heading, new Separator()));
        root.setCenter(workspace);
        root.setBottom(new VBox(new Separator(), status));
        stage.setTitle("NES Asset Workbench");
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.setScene(new Scene(root, 1000, 680));
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}

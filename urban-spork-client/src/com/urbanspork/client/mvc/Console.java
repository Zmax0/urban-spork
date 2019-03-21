package com.urbanspork.client.mvc;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Console extends Application {

    private static Stage PRIMARY_STAGE;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Platform.setImplicitExit(false);
        PRIMARY_STAGE = primaryStage;
        Parent root = FXMLLoader.load(Resource.CLIENT_FXML.toURI().toURL());
        Scene scene = new Scene(root);
        PRIMARY_STAGE.setScene(scene);
        PRIMARY_STAGE.setOnCloseRequest(event -> {
            event.consume();
            PRIMARY_STAGE.hide();
        });
        PRIMARY_STAGE.setResizable(false);
        PRIMARY_STAGE.setTitle("Console");
        PRIMARY_STAGE.hide();
    }

    public static void show() {
        PRIMARY_STAGE.show();
    }

    public static void launch(String[] args) {
        Application.launch(args);
    }

}

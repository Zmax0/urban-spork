package com.urbanspork.client.mvc.component;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Resource;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Console extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Component.Console.set(this);
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);
        Parent root = FXMLLoader.load(Resource.CLIENT_FXML.toURI().toURL());
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });
        primaryStage.setResizable(false);
        primaryStage.setTitle("Console");
        primaryStage.hide();
    }

    public void show() {
        primaryStage.show();
    }

    public static void launch(String[] args) {
        Application.launch(args);
    }

}

package com.urbanspork.client.mvc.component;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Console extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Component.Console.set(this);
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);
        Parent root = FXMLLoader.load(Resource.CLIENT_FXML, Resource.bundle);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNIFIED);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });
        primaryStage.setResizable(false);
        primaryStage.setTitle(I18n.PRAGRAM_TITLE);
        primaryStage.hide();
    }

    public void show() {
        if (primaryStage.isShowing()) {
            if (!primaryStage.isFocused()) {
                primaryStage.requestFocus();
            }
        } else {
            primaryStage.show();
        }
    }

    public void hide() {
        primaryStage.hide();
    }

    public static void launch(String[] args) {
        Application.launch(args);
    }

}

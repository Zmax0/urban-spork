package com.urbanspork.client.mvc.component;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Resource;

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
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle("com.urbanspork.client.i18n.language", Locale.getDefault());
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle("com.urbanspork.client.i18n.language", new Locale("en"));
        }
        Parent root = FXMLLoader.load(Resource.CLIENT_FXML.toURI().toURL(), bundle);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNIFIED);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });
        primaryStage.setResizable(false);
        primaryStage.setTitle("Console");
        primaryStage.hide();
    }

    public void show() {
        if (primaryStage.isShowing()) {
            primaryStage.requestFocus();
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

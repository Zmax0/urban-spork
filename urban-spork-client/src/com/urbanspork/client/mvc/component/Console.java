package com.urbanspork.client.mvc.component;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Components;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Console extends Application implements Component {

    boolean started;

    private Thread thread;

    private Stage primaryStage;

    @Override
    public void init() throws Exception {
        Components.register(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
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
        primaryStage.getIcons().add(new Image(Resource.PROGRAM_ICON.toString()));
        primaryStage.setTitle(I18n.PROGRAM_TITLE);
        primaryStage.hide();
    }

    public void show() {
        if (primaryStage != null) {
            if (primaryStage.isIconified()) {
                primaryStage.setIconified(false);
            }
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
        }
    }

    public void hide() {
        if (primaryStage != null) {
            primaryStage.hide();
        }
    }

    public static void launch(String... args) {
        Application.launch(args);
    }

    @Override
    public void start(String[] args) {
        thread = new Thread(() -> {
            launch(args);
        });
        thread.setName("Console-Launcher");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public boolean started() {
        return Components.Console != null && Components.Console.started;
    }
}

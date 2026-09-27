package com.netdiag;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlLocation = getClass().getResource("/com/netdiag/view/main.fxml");
            if (fxmlLocation == null) {
                throw new IllegalStateException("Arquivo FXML não encontrado em /com/netdiag/view/main.fxml");
            }
            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root, 980, 720);

            primaryStage.setTitle("NetDiag Pro - Diagnóstico de Computador, Impressoras e Rede");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(850);
            primaryStage.setMinHeight(600);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

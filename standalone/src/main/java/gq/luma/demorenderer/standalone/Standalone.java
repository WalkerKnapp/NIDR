package gq.luma.demorenderer.standalone;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Standalone extends Application {
    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/StandaloneGUI.fxml"));
        Scene scene = new Scene(root, 405, 352);

        primaryStage.setTitle("NI Source Demo Render");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}

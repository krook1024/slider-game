package com.krook1024.game.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class that controls the application GUI.
 */
public class App extends Application {
    private int appWidth = 640;
    private int appHeight = 480;

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting slider-game...");
        long startTime = System.nanoTime();

        setAppTitleWithVersion(stage);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/launcher.fxml"));
        logger.debug("Loaded fxml resource: " + root);

        stage.setScene(new Scene(root));
        stage.setWidth(appWidth);
        stage.setHeight(appHeight);
        stage.setResizable(false);
        stage.show();

        long elapsedTime = System.nanoTime() - startTime;
        logger.info("Started application in " + elapsedTime / 1000000 + " ms");
    }

    /**
     * Sets the stage's title according to the project version in pom.xml.
     *
     * @param stage the stage whose title has to be set
     */
    private void setAppTitleWithVersion(Stage stage) {
        try {
            stage.setTitle("slider-game " + getProjectVersionFromPom());
        } catch (Exception e) {
            stage.setTitle("slider-game");
        }
    }

    /**
     * Find the the project version in pom.xml and returns it.
     *
     * @return the project version
     * @throws IOException if an error occurs during reading pom.xml
     * @throws XmlPullParserException if pom.xml cannot be parsed
     */
    private String getProjectVersionFromPom() throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        return model.getVersion();
    }
}
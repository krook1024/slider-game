package com.krook1024.game.controller;

import com.google.inject.Inject;
import com.krook1024.game.results.GameResult;
import com.krook1024.game.results.GameResultDao;
import com.krook1024.game.state.Axis;
import com.krook1024.game.state.Direction;
import com.krook1024.game.state.SliderState;
import com.krook1024.game.state.Tile;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.time.Instant;
import java.util.List;

/**
 * Acts as a controller class for the game view.
 */
public class GameController extends BaseController {
    private String name;
    private Instant startTime;
    private Duration time = Duration.ZERO;
    private IntegerProperty steps = new SimpleIntegerProperty(0);
    private SliderState sliderState;
    private Timeline clock;
    private List<Image> images;
    private int activeTileIndex = -1;

    @Inject
    GameResultDao gameResultDao;

    @FXML
    Label usernameLabel;
    @FXML
    Label elapsedTimeLabel;
    @FXML
    Label stepsLabel;
    @FXML
    GridPane gameGrid;
    @FXML
    Button giveUpButton;

    private BooleanProperty gameOver = new SimpleBooleanProperty();

    /**
     * Sets the name to the one specified as the parameter.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    @FXML
    private void initialize() {
        images = List.of(
                new Image(getClass().getResource("/rectangle/1.png").toExternalForm()),
                new Image(getClass().getResource("/rectangle/2.png").toExternalForm()),
                new Image(getClass().getResource("/rectangle/3.png").toExternalForm()),
                new Image(getClass().getResource("/rectangle/4.png").toExternalForm()),
                new Image(getClass().getResource("/rectangle/5.png").toExternalForm())
        );
        stepsLabel.textProperty().bind(steps.asString());
        gameOver.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                logger.info("Game is over");
                gameOver.setValue(false);
                logger.debug("Saving result to database...");
                gameResultDao.persist(createGameResult());
                clock.stop();
            }
        });
        resetGame();
    }

    private GameResult createGameResult() {
        return GameResult.builder()
                .player(name)
                .solved(sliderState.isSolved())
                .duration(java.time.Duration.between(startTime, Instant.now()))
                .steps(steps.get())
                .build();
    }

    private void resetGame() {
        Platform.runLater(() -> {
            logger.info("Setting name to {}", name);
            usernameLabel.setText("Hello, " + name);
        });
        sliderState = new SliderState();
        steps.set(0);
        time = Duration.ZERO;
        startTime = Instant.now();
        clock = getClockTimeline();
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        gameOver.setValue(false);
        draw();
    }

    /**
     * Creates a timeline that runs the clock (counts elapsed time).
     *
     * @return the timeline for the clock
     */
    private Timeline getClockTimeline() {
        return new Timeline(
                new KeyFrame(Duration.millis(100),
                        t -> {
                            Duration duration = ((KeyFrame) t.getSource()).getTime();
                            time = time.add(duration);
                            elapsedTimeLabel.setText(formatElapsedTime((int) time.toSeconds()));
                        })
        );
    }

    /**
     * Formats seconds as H:MM:SS.
     *
     * @param seconds the seconds to format
     * @return the formatted string
     */
    private String formatElapsedTime(int seconds) {
        return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    /**
     * Draws the current {@code SliderState} to the {@code gameGrid} GridPanepublic.
     */
    private void draw() {
        gameGrid.getChildren().clear();
        for (int i = 0; i < sliderState.getTiles().size(); i++) {
            Tile t = sliderState.getTiles().get(i);
            ImageView imageView = new ImageView();

            GridPane.setRowIndex(imageView, Math.min(t.getTopLeft().getY(), t.getBotLeft().getY()));
            GridPane.setColumnIndex(imageView, Math.min(t.getTopLeft().getX(), t.getBotLeft().getX()));
            GridPane.setRowSpan(imageView, 2);
            GridPane.setColumnSpan(imageView, 2);

            imageView.setImage(images.get(t.getType().getValue() - 1));
            imageView.setOnMouseClicked(this::handleGameGridClick);
            if (i == activeTileIndex) {
                imageView.setStyle("-fx-opacity: 0.85");
            }
            gameGrid.getChildren().add(imageView);
        }
    }

    private void handleGameGridClick(Event e) {
        Node source = (Node) e.getSource();

        Integer colIndex = GridPane.getColumnIndex(source);
        Integer rowIndex = GridPane.getRowIndex(source);
        Integer colSpan = GridPane.getColumnSpan(source);
        Integer rowSpan = GridPane.getRowSpan(source);

        logger.info("Clicked on Tile [{}, {}]", colIndex, rowIndex);

        gameGrid.getChildren()
                .filtered((Node elem) -> elem != source)
                .forEach((Node elem) -> elem.setStyle("-fx-opacity: 1;"));
        source.setStyle("-fx-opacity: 0.85");

        activeTileIndex = sliderState.findTileIndexByTopLeftAtPoint(colIndex, rowIndex);
        logger.info("The corresponding index for the clicked tile is {}", activeTileIndex);
    }

    @FXML
    private void onStepClick(ActionEvent event) {
        Node source = (Node) event.getSource();
        String accessibleText = source.getAccessibleText();
        if ( ! sliderState.isSolved() && ! gameOver.getValue() && activeTileIndex != -1) {
            logger.debug("Stepping cube {} in the direction {}", activeTileIndex, accessibleText);
            steps.set(steps.get() + 1);
            switch (accessibleText) {
                case "UP":
                    sliderState.stepTileWithIndex(activeTileIndex, Direction.UP, Axis.Y);
                    break;
                case "DOWN":
                    sliderState.stepTileWithIndex(activeTileIndex, Direction.DOWN, Axis.Y);
                    break;
                case "LEFT":
                    sliderState.stepTileWithIndex(activeTileIndex, Direction.LEFT, Axis.X);
                    break;
                case "RIGHT":
                    sliderState.stepTileWithIndex(activeTileIndex, Direction.RIGHT, Axis.X);
            }
            if (sliderState.isSolved()) {
                gameOver.setValue(true);
                logger.info("Player {} has solved the game in {} steps", name, steps.get());
                giveUpButton.setText("You won!");
                giveUpButton.setDisable(true);
                createGoBackToMainMenuButton();
            }
            draw();
        }
    }

    @FXML
    private void onGiveUpButtonClicked(ActionEvent event) {
        if ( ! gameOver.getValue()) {
            gameOver.setValue(true);
            clock.stop();

            Button source = (Button) event.getSource();
            source.setDisable(true);
            source.setText("You have given up!");

            createGoBackToMainMenuButton();
        }
    }

    private void createGoBackToMainMenuButton() {
        HBox hbox = (HBox) giveUpButton.getParent();
        Button newButton = new Button("Go back to Main Menu");
        newButton.setOnAction(this::onGoBackToMainMenuButtonClicked);
        hbox.getChildren().add(newButton);
    }

    @FXML
    private void onGoBackToMainMenuButtonClicked(ActionEvent event) {
        changeSceneTo(getStageOfEvent(event), "/fxml/launcher.fxml");
    }
}

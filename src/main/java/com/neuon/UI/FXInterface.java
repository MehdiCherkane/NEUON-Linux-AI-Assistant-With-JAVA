package com.neuon.UI;

import com.neuon.agent.*;
import com.neuon.core.*;
import com.neuon.tools.*;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.concurrent.CountDownLatch;

public class FXInterface extends Interface {

    private static TextArea outputArea;
    private Stage interactiveDialog;
    private TextField interactiveInput;
    private Runner activeRunner;   // used to send input/kill
    
    public void setOutputArea(TextArea outputArea) {
        this.outputArea = outputArea;
    }

    @Override
    
    public void sendOutput(String output) {
        
        if (output == null) return;
        if (Platform.isFxApplicationThread()) {
            append(output);
        } else {
            Platform.runLater(() -> append(output));
        }
    }

    private void append(String output) {
        if (outputArea == null) {
            System.out.println(output);
            return;
        }
        outputArea.appendText("\n" + output);
        outputArea.positionCaret(outputArea.getText().length());
    }

    @Override
    public boolean validateCommand(String command) {
        if (Platform.isFxApplicationThread()) {
            return showNeuonDialog(command);
        }
        
        final boolean[] result = {false};
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            result[0] = showNeuonDialog(command);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return result[0];
    }

    private boolean showNeuonDialog(String command) {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle(
            "-fx-background-color: #020b10;" +
            "-fx-border-color: #00f2ff;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,242,255,0.25), 24, 0, 0, 0);"
        );

        Label title = new Label("⚠ SAFETY PROTOCOL");
        title.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#00f2ff"));

        Label sub = new Label("Command flagged for manual review:");
        sub.setTextFill(Color.web("#00f2ff"));
        sub.setOpacity(0.7);
        sub.setFont(Font.font("Segoe UI", 12));

        Label cmd = new Label(command);
        cmd.setTextFill(Color.web("#ff5555"));
        cmd.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        cmd.setStyle(
            "-fx-background-color: rgba(255,85,85,0.08);" +
            "-fx-padding: 10 16;" +
            "-fx-border-color: rgba(255,85,85,0.25);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;"
        );

        Label q = new Label("Authorize execution?");
        q.setTextFill(Color.web("#00f2ff"));
        q.setFont(Font.font("Segoe UI", 13));

    
        

        HBox btns = new HBox(14);
        btns.setAlignment(Pos.CENTER);

        final boolean[] ok = {false};

        Button auth = styledBtn("AUTHORIZE", "#00f2ff", "#000000");
        auth.setOnAction(e -> { ok[0] = true; dialog.close(); });

        Button deny = styledBtn("DENY", "#ff5555", "#000000");
        deny.setOnAction(e -> { ok[0] = false; dialog.close(); });

        btns.getChildren().addAll(auth, deny);
        root.getChildren().addAll(title, sub, cmd, q, btns);

        Scene sc = new Scene(root);
        sc.setFill(Color.TRANSPARENT);
        sc.setOnKeyPressed(ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                ok[0] = false;
                dialog.close();
            }
        });

        dialog.setScene(sc);
        dialog.showAndWait();
        return ok[0];
    }

    private Button styledBtn(String text, String border, String hoverBg) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + border + ";" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 8 22;" +
            "-fx-font-family: 'Orbitron';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 11px;" +
            "-fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color: " + border + ";" +
            "-fx-text-fill: " + hoverBg + ";" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 8 22;" +
            "-fx-font-family: 'Orbitron';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 11px;" +
            "-fx-cursor: hand;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + border + ";" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 4;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 8 22;" +
            "-fx-font-family: 'Orbitron';" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 11px;" +
            "-fx-cursor: hand;"
        ));
        return b;
    }

    // Interactive process support
    
    @Override
    public void startInteractive(String command, Runner runner) {
        this.activeRunner = runner;

        Platform.runLater(() -> {
            if (interactiveDialog == null) {
                interactiveDialog = new Stage();
                interactiveDialog.initModality(Modality.NONE);
                interactiveDialog.setTitle("Interactive Session");
                interactiveDialog.setResizable(true);
                interactiveDialog.initStyle(StageStyle.UTILITY);
                VBox vbox = new VBox(15);
                vbox.setPadding(new Insets(20));
                vbox.setStyle("-fx-background-color: #020b10; -fx-border-color: #00f2ff; -fx-border-width: 1;");

                Label cmdLabel = new Label("Command: " + command);
                cmdLabel.setTextFill(Color.web("#00f2ff"));
                cmdLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

                interactiveInput = new TextField();
                interactiveInput.setPromptText("Type input and press Enter");
                interactiveInput.setStyle("-fx-background-color: black; -fx-text-fill: #00f2ff; -fx-border-color: #00f2ff;");
                interactiveInput.setOnAction(e -> {
                    String input = interactiveInput.getText();
                    if (activeRunner != null && !input.isEmpty()) {
                        activeRunner.sendInput(input);
                        interactiveInput.clear();
                    }
                });

                Button killBtn = new Button("Kill Process");
                killBtn.setStyle("-fx-background-color: #ff5555; -fx-text-fill: white; -fx-font-weight: bold;");
                killBtn.setOnAction(e -> {
                    if (activeRunner != null) activeRunner.killActiveProcess();
                    interactiveDialog.close();
                });

                vbox.getChildren().addAll(cmdLabel, interactiveInput, killBtn);
                Scene scene = new Scene(vbox, 800, 400);
                scene.setFill(Color.TRANSPARENT);
                interactiveDialog.setScene(scene);
            }
            interactiveDialog.show();
            interactiveInput.requestFocus();
        });
    }

    @Override
    public void endInteractive() {
        Platform.runLater(() -> {
            if (interactiveDialog != null) {
                interactiveDialog.close();
                interactiveDialog = null;
            }
            activeRunner = null;
        });
    }

    @Override
    public String getPrompt() {
        return "";
    }
}

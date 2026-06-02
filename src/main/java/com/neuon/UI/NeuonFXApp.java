package com.neuon.UI;

import com.neuon.agent.*;
import com.neuon.core.*;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NeuonFXApp extends Application {

    private Timeline voiceRotateTimeline;
    private Timeline voiceRotateTimeline2;
    private Timeline voiceRotateTimeline3;
    private double originalRing1StrokeWidth = 1;

    private TextArea outputArea;
    private TextArea historyArea;
    private TextField inputField;
    private Label statusLabel;
    private Label clockLabel;
    private Circle ring1, ring2, ring3;
    private Timeline clockTimeline;
    private Timeline ring1Timeline, ring2Timeline, ring3Timeline;
    private Timeline voicePulseTimeline;
    private ProgressBar progressBar;
    private Button micBtn;
    private Button sendBtn;
    private boolean isListening;
    private boolean isProcessing;

    private ScheduledExecutorService hwScheduler;

    private HardwareMonitor hardwareMonitor;
    private TextArea hwMonitor;
    private TextArea workspaceFileList;
    private TextArea toolCallDisplay;
    private TextArea stdoutOutput;
    private TextArea stderrOutput;
    private Label statusIndicator;
    private Label apiKeyStatus;
    private Label modelNameLabel;
    private Label taskLabel;
    private Label projectLabel;
    private Label stepLabel;
    private Label resultLabel;
    private Button cancelBtn;
    private Button clearBtn;
    private Task<Void> currentTask;
    private Thread currentTaskThread;
    private OrchesterAgent orchesterAgent; 
    private VoiceHandler voiceHandler;
    private FXInterface fxInterface;

    @Override
    public void start(Stage stage) {
        
        voiceHandler = new VoiceHandler();

        StackPane mainRoot = buildUI();

        fxInterface = new FXInterface();

        orchesterAgent = new OrchesterAgent();

        Scene scene = new Scene(mainRoot, 1400, 900);
        scene.setFill(Color.web("#020b10"));

        stage.setTitle("NEUON | AI CORE");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            clearAgentMemory();
            stopAnimations();
        });
        scene.setOnKeyPressed(e -> {
        
        if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.M) {
            startVoiceRecording(micBtn);
        }
    });
        stage.show();

        fxInterface.setOutputArea(outputArea);
        fxInterface.setToolCallTextArea(toolCallDisplay);
        fxInterface.setStdoutTextArea(stdoutOutput);
        fxInterface.setStderrTextArea(stderrOutput);
        fxInterface.sendOutput("[SYSTEM] Ready Boss");
        hardwareMonitor = new HardwareMonitor();
        startHardwareMonitoring();
        startAnimations();
        refreshWorkspaceFileList();
    }

    private StackPane buildUI() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: transparent;");

        Pane bg = new Pane();
        bg.setStyle(
            "-fx-background-color: #020b10," +
            "linear-gradient(from 0px 0px to 0px 40px, rgba(0,242,255,0.05) 1px, transparent 1px)," +
            "linear-gradient(from 0px 0px to 40px 0px, rgba(0,242,255,0.05) 1px, transparent 1px);" +
            "-fx-background-size: 100% 100%, 40px 40px, 40px 40px;"
        );
        bg.setMouseTransparent(true);

        Region scanline = new Region();
        scanline.setStyle(
            "-fx-background-color: linear-gradient(rgba(18,16,16,0) 50%, rgba(0,0,0,0.1) 50%);" +
            "-fx-background-size: 100% 4px;"
        );
        scanline.setMouseTransparent(true);

        HBox topBar = createTopBar();
        grid.add(topBar, 0, 0, 4, 1);

        VBox leftPanel = createPanel("HARDWARE MONITOR");
        hwMonitor = new TextArea();
        hwMonitor.setEditable(false);
        hwMonitor.setWrapText(true);
        hwMonitor.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #00f2ff;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );
        hwMonitor.setPrefHeight(200);
        leftPanel.getChildren().add(hwMonitor);

        VBox wsPanel = createPanel("WORKSPACE FILES");
        workspaceFileList = new TextArea();
        workspaceFileList.setEditable(false);
        workspaceFileList.setWrapText(true);
        workspaceFileList.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #00f2ff;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );
        workspaceFileList.setPrefHeight(200);
        VBox.setVgrow(workspaceFileList, Priority.ALWAYS);
        wsPanel.getChildren().add(workspaceFileList);

        VBox leftMain = new VBox(10, leftPanel, wsPanel);
        VBox.setVgrow(wsPanel, Priority.ALWAYS);
        grid.add(leftMain, 0, 1);

        VBox centerLeftPanel = createPanel("STATUS");
        VBox statusContent = new VBox(8);
        statusContent.setPadding(new Insets(4));

        statusIndicator = new Label("IDLE");
        statusIndicator.setTextFill(Color.web("#00f2ff"));
        statusIndicator.setFont(Font.font("Consolas", FontWeight.BOLD, 14));

        modelNameLabel = new Label("Model: " + getModelName());
        modelNameLabel.setTextFill(Color.web("#00f2ff"));
        modelNameLabel.setFont(Font.font("Consolas", 11));

        apiKeyStatus = new Label(apiKeyStatusText());
        apiKeyStatus.setTextFill(Color.web("#00f2ff"));
        apiKeyStatus.setFont(Font.font("Consolas", 11));

        VBox taskBox = new VBox(4);
        taskLabel = new Label("Task: --");
        taskLabel.setTextFill(Color.web("#39ced3"));
        taskLabel.setFont(Font.font("Consolas", 11));
        projectLabel = new Label("Project: --");
        projectLabel.setTextFill(Color.web("#39ced3"));
        projectLabel.setFont(Font.font("Consolas", 11));
        stepLabel = new Label("Step: --");
        stepLabel.setTextFill(Color.web("#39ced3"));
        stepLabel.setFont(Font.font("Consolas", 11));
        resultLabel = new Label("Result: --");
        resultLabel.setTextFill(Color.web("#39ced3"));
        resultLabel.setFont(Font.font("Consolas", 11));
        taskBox.getChildren().addAll(taskLabel, projectLabel, stepLabel, resultLabel);

        Label toolCallHeader = new Label("CURRENT TOOL CALL");
        toolCallHeader.setTextFill(Color.web("#00f2ff"));
        toolCallHeader.setFont(Font.font("Consolas", FontWeight.BOLD, 11));

        toolCallDisplay = new TextArea();
        toolCallDisplay.setEditable(false);
        toolCallDisplay.setWrapText(true);
        toolCallDisplay.setPrefHeight(80);
        toolCallDisplay.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #ffaa00;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );

        statusContent.getChildren().addAll(statusIndicator, modelNameLabel, apiKeyStatus, taskBox, toolCallHeader, toolCallDisplay);
        VBox.setVgrow(toolCallDisplay, Priority.ALWAYS);
        centerLeftPanel.getChildren().add(statusContent);
        grid.add(centerLeftPanel, 1, 1);

        StackPane center = createCenterArea();
        grid.add(center, 2, 1);

        VBox rightPanel = createPanel("NEURAL OUTPUT");
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #39ced3;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );
        outputArea.setText("> Awaiting user input...");
        rightPanel.getChildren().add(outputArea);
        VBox.setVgrow(outputArea, Priority.ALWAYS);
        grid.add(rightPanel, 3, 1);

        HBox bottomBar = createBottomBar();
        grid.add(bottomBar, 0, 2, 4, 1);

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(18);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(22);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(25);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(35);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);

        RowConstraints r1 = new RowConstraints(); r1.setPercentHeight(10);
        RowConstraints r2 = new RowConstraints(); r2.setPercentHeight(70);
        RowConstraints r3 = new RowConstraints(); r3.setPercentHeight(20);
        grid.getRowConstraints().addAll(r1, r2, r3);

        StackPane root = new StackPane(bg, grid, scanline);
        return root;
    }

    private HBox createTopBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER);
        bar.setStyle(
            "-fx-background-color: rgba(0,15,25,0.7);" +
            "-fx-padding: 10;" +
            "-fx-border-color: #00f2ff;" +
            "-fx-border-width: 0 0 1 0;"
        );

        Label coreId = new Label("CORE_ID: NX-8800");
        coreId.setTextFill(Color.web("#00f2ff"));
        coreId.setFont(Font.font("monospace", 12));

        clockLabel = new Label("TIME: 00:00:00");
        clockLabel.setTextFill(Color.web("#00f2ff"));
        clockLabel.setFont(Font.font("monospace", 12));

        Label emu = new Label("LINUX EMULATION: ACTIVE");
        emu.setTextFill(Color.web("#00f2ff"));
        emu.setFont(Font.font("monospace", 12));

        String model = getModelName();
        Label modelLabel = new Label("MODEL: " + model);
        modelLabel.setTextFill(Color.web("#00f2ff"));
        modelLabel.setFont(Font.font("monospace", 12));

        String apiKey = System.getenv("GROQ_API_KEY");
        Circle apiDot = new Circle(5);
        apiDot.setFill(apiKey != null && !apiKey.isBlank() ? Color.web("#00ff88") : Color.web("#ff5555"));
        Label apiLabel = new Label("API");
        apiLabel.setTextFill(Color.web("#00f2ff"));
        apiLabel.setFont(Font.font("monospace", 12));
        HBox apiBox = new HBox(4, apiDot, apiLabel);
        apiBox.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(coreId, clockLabel, emu, modelLabel, apiBox);
        return bar;
    }

    private VBox createPanel(String title) {
        VBox panel = new VBox(10);
        panel.setStyle(
            "-fx-background-color: rgba(0,15,25,0.7);" +
            "-fx-border-color: rgba(0,242,255,0.2);" +
            "-fx-border-width: 1;" +
            "-fx-padding: 15;"
        );

        Label lbl = new Label(title);
        lbl.setStyle(
            "-fx-background-color: #00f2ff;" +
            "-fx-text-fill: black;" +
            "-fx-padding: 2 8;" +
            "-fx-font-weight: bold;"
        );
        lbl.setFont(Font.font("monospace", 10));

        panel.getChildren().add(lbl);
        return panel;
    }

    private StackPane createCenterArea() {
        StackPane center = new StackPane();
        center.setStyle(
            "-fx-background-color: rgba(0,15,25,0.7);" +
            "-fx-border-color: rgba(0,242,255,0.2);" +
            "-fx-border-width: 1;"
        );

        ring1 = createRing(200, 1, true);
        ring2 = createRing(170, 2, false);
        ring3 = createRing(140, 10, true);

        Label title = new Label("NEUON");
        title.setTextFill(Color.web("#00f2ff"));
        title.setFont(Font.font("Orbitron", FontWeight.BOLD, 48));
        title.setStyle("-fx-effect: dropshadow(gaussian, #00f2ff, 30, 0, 0, 0);");

        statusLabel = new Label("SYSTEM IDLE");
        statusLabel.setTextFill(Color.web("#00f2ff"));
        statusLabel.setFont(Font.font("monospace", 10));
        statusLabel.setOpacity(0.6);

        VBox titleBox = new VBox(10, title, statusLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setMouseTransparent(true);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(0);
        progressBar.setPrefHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setVisible(false);
        progressBar.setStyle(
            "-fx-accent: #00f2ff;" +
            "-fx-background-color: rgba(0,242,255,0.08);" +
            "-fx-background-radius: 2;" +
            "-fx-padding: 0;"
        );
        StackPane.setAlignment(progressBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(progressBar, new Insets(0, 0, 80, 0));

        center.getChildren().addAll(ring1, ring2, ring3, titleBox, progressBar);
        return center;
    }

    private Circle createRing(double radius, double strokeWidth, boolean dashed) {
        Circle c = new Circle(radius);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(Color.web("#00f2ff"));
        c.setStrokeWidth(strokeWidth);
        if (dashed) {
            c.getStrokeDashArray().addAll(10.0, 10.0);
        }
        return c;
    }

    private HBox createBottomBar() {
        HBox bar = new HBox(15);

        VBox histPanel = createPanel("COMMAND HISTORY");
        histPanel.setPrefWidth(600);
        historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setWrapText(true);
        historyArea.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #00f2ff;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );
        histPanel.getChildren().add(historyArea);
        VBox.setVgrow(historyArea, Priority.ALWAYS);

        VBox inputPanel = createPanel("INPUT");

        inputField = new TextField();
        inputField.setPromptText("Awaiting command...");
        inputField.setPrefWidth(350);
        inputField.setPrefHeight(95);

        String baseStyle =
            "-fx-background-color: rgba(0,15,25,0.9);" +
            "-fx-text-fill: #00f2ff;" +
            "-fx-prompt-text-fill: rgba(0,242,255,0.35);" +
            "-fx-border-color: rgba(0,242,255,0.25);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-family: 'Segoe UI', sans-serif;" +
            "-fx-font-size: 13px;";
        String focusStyle = baseStyle +
            "-fx-border-color: #00f2ff;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,242,255,0.25), 8, 0, 0, 0);";

        inputField.setStyle(baseStyle);
        inputField.focusedProperty().addListener((obs, old, val) ->
            inputField.setStyle(val ? focusStyle : baseStyle)
        );
        inputField.setOnAction(e -> sendPrompt());

        sendBtn = new Button("FIRE");
        String btnBase =
            "-fx-background-color: #00f2ff;" +
            "-fx-text-fill: #000;" +
            "-fx-font-weight: bold;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 18;" +
            "-fx-font-family: 'Orbitron', sans-serif;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 12px;";
        String btnHover = btnBase +
            "-fx-background-color: #4dffff;" +
            "-fx-effect: dropshadow(gaussian, #00f2ff, 10, 0, 0, 0);";

        sendBtn.setStyle(btnBase);
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(btnHover));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(btnBase));
        sendBtn.setOnAction(e -> sendPrompt());

        micBtn = new Button("SPEAK");
        String micBase =
            "-fx-background-color: #00f2ff;" +
            "-fx-text-fill: #000;" +
            "-fx-font-weight: bold;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 18;" +
            "-fx-font-family: 'Orbitron', sans-serif;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 12px;";
        String micHover = micBase +
            "-fx-background-color: #4dffff;" +
            "-fx-effect: dropshadow(gaussian, #00f2ff, 10, 0, 0, 0);";

        micBtn.setStyle(micBase);
        micBtn.setOnMouseEntered(e -> micBtn.setStyle(micHover));
        micBtn.setOnMouseExited(e -> micBtn.setStyle(micBase));
        micBtn.setOnAction(e -> startVoiceRecording(micBtn));

        cancelBtn = new Button("CANCEL");
        String cancelBase =
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #ff5555;" +
            "-fx-border-color: #ff5555;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-family: 'Orbitron', sans-serif;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 11px;";
        cancelBtn.setStyle(cancelBase);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
            "-fx-background-color: #ff5555;" +
            "-fx-text-fill: #000;" +
            "-fx-font-weight: bold;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-family: 'Orbitron', sans-serif;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 11px;"
        ));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelBase));
        cancelBtn.setOnAction(e -> cancelCurrentTask());
        cancelBtn.setDisable(true);

        clearBtn = new Button("CLEAR");
        String clearBase =
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #ffaa00;" +
            "-fx-border-color: #ffaa00;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-family: 'Orbitron', sans-serif;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 11px;";
        clearBtn.setStyle(clearBase);
        clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(
            "-fx-background-color: #ffaa00;" +
            "-fx-text-fill: #000;" +
            "-fx-font-weight: bold;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-family: 'Orbitron', sans-serif;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 11px;"
        ));
        clearBtn.setOnMouseExited(e -> clearBtn.setStyle(clearBase));
        clearBtn.setOnAction(e -> clearConversation());

        HBox inputBox = new HBox(8, inputField, sendBtn, micBtn, cancelBtn, clearBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(8, 0, 0, 0));
        inputPanel.getChildren().add(inputBox);

        VBox stdoutPanel = createPanel("STDOUT");
        stdoutOutput = new TextArea();
        stdoutOutput.setEditable(false);
        stdoutOutput.setWrapText(true);
        stdoutOutput.setPrefHeight(80);
        stdoutOutput.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #00ff88;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );
        stdoutPanel.getChildren().add(stdoutOutput);

        VBox stderrPanel = createPanel("STDERR");
        stderrOutput = new TextArea();
        stderrOutput.setEditable(false);
        stderrOutput.setWrapText(true);
        stderrOutput.setPrefHeight(80);
        stderrOutput.setStyle(
            "-fx-control-inner-background: black;" +
            "-fx-text-fill: #ff5555;" +
            "-fx-font-family: 'Consolas', monospace;" +
            "-fx-font-size: 11px;"
        );
        stderrPanel.getChildren().add(stderrOutput);

        VBox outputPanels = new VBox(8, stdoutPanel, stderrPanel);
        HBox.setHgrow(outputPanels, Priority.ALWAYS);

        bar.getChildren().addAll(histPanel, inputPanel, outputPanels);
        HBox.setHgrow(histPanel, Priority.ALWAYS);
        HBox.setHgrow(inputPanel, Priority.ALWAYS);
        return bar;
    }

    private void startVoiceRecording(Button micBtn) {
        if (isListening || isProcessing) {
            return;
        }

        micBtn.setStyle(
            "-fx-background-color: #ff5555;" +
            "-fx-text-fill: white;" +
            "-fx-border-color: #ff5555;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-size: 16px;" +
            "-fx-cursor: hand;"
        );
        setListening(true);

        voicePulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring1.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring1.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring2.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring2.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring3.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring3.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT)
            ),
            new KeyFrame(Duration.millis(600),
                new KeyValue(ring1.scaleXProperty(), 1.1, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring1.scaleYProperty(), 1.1, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring2.scaleXProperty(), 1.05, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring2.scaleYProperty(), 1.05, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring3.scaleXProperty(), 1.2, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(ring3.scaleYProperty(), 1.2, javafx.animation.Interpolator.EASE_OUT)
            )
           
        );
        voicePulseTimeline.setAutoReverse(true);
        voicePulseTimeline.setCycleCount(Timeline.INDEFINITE);
        voicePulseTimeline.play();

        if (ring1Timeline != null) ring1Timeline.stop();

        ring1.setStrokeWidth(4);

        voiceRotateTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring1.rotateProperty(), ring1.getRotate(), javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(ring1.rotateProperty(), ring1.getRotate() + 360, javafx.animation.Interpolator.LINEAR))
        );
        voiceRotateTimeline.setCycleCount(Timeline.INDEFINITE);
        voiceRotateTimeline.play();

        voiceRotateTimeline2 = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring2.rotateProperty(), ring2.getRotate(), javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(ring2.rotateProperty(), ring2.getRotate() - 360, javafx.animation.Interpolator.LINEAR))
        );
        voiceRotateTimeline2.setCycleCount(Timeline.INDEFINITE);
        voiceRotateTimeline2.play();

        voiceRotateTimeline3 = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring3.rotateProperty(), ring3.getRotate(), javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(ring3.rotateProperty(), ring3.getRotate() + 360, javafx.animation.Interpolator.LINEAR))
        );
        voiceRotateTimeline3.setCycleCount(Timeline.INDEFINITE);
        voiceRotateTimeline3.play();

        ring1.setStroke(Color.web("#00f2ff"));

        Task<String> voiceTask = new Task<>() {
            @Override

            protected String call() {

                return voiceHandler.recordAndTranscribe();
            }
            

            @Override
            protected void succeeded() {
                String transcript = getValue();
                stopVoiceAnimation(micBtn);

                if (transcript != null && !transcript.isEmpty()) {
                    inputField.setText(transcript);
                    sendPrompt();
                } else {
                    fxInterface.sendOutput("[SYSTEM] No speech detected.");
                }
            }

            @Override
            protected void failed() {
                stopVoiceAnimation(micBtn);
                fxInterface.sendOutput("[ERROR] Voice recording failed: " + getException().getMessage());
            }

            @Override
            protected void cancelled() {
                stopVoiceAnimation(micBtn);
            }
        };

        Thread t = new Thread(voiceTask);
        t.setDaemon(true);
        t.start();
    }

    private void stopVoiceAnimation(Button micBtn) {
        Timeline returnToIdle = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring1.scaleXProperty(), ring1.getScaleX()),
                new KeyValue(ring1.scaleYProperty(), ring1.getScaleY()),
                new KeyValue(ring2.scaleXProperty(), ring2.getScaleX()),
                new KeyValue(ring2.scaleYProperty(), ring2.getScaleY()),
                new KeyValue(ring3.scaleXProperty(), ring3.getScaleX()),
                new KeyValue(ring3.scaleYProperty(), ring3.getScaleY()),
                new KeyValue(ring1.strokeWidthProperty(), ring1.getStrokeWidth()),
                new KeyValue(ring1.rotateProperty(), ring1.getRotate())
            ),
            new KeyFrame(Duration.millis(500),
                new KeyValue(ring1.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring1.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring2.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring2.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring3.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring3.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring1.strokeWidthProperty(), originalRing1StrokeWidth, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring1.rotateProperty(), ring1.getRotate() + 90, javafx.animation.Interpolator.EASE_IN)
            )
        );
        returnToIdle.setOnFinished(e -> {
            if (voiceRotateTimeline != null) {
                voiceRotateTimeline.stop();
                voiceRotateTimeline = null;
            }
            if (voiceRotateTimeline2 != null) {
                voiceRotateTimeline2.stop();
                voiceRotateTimeline2 = null;
            }
            if (voiceRotateTimeline3 != null) {
                voiceRotateTimeline3.stop();
                voiceRotateTimeline3 = null;
            }
            if (voicePulseTimeline != null) {
                voicePulseTimeline.stop();
                voicePulseTimeline = null;
            }
            ring1.setStroke(Color.web("#00f2ff"));
            ring1.setStrokeWidth(originalRing1StrokeWidth);

            ring1Timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(ring1.rotateProperty(), ring1.getRotate(), javafx.animation.Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(40),
                    new KeyValue(ring1.rotateProperty(), ring1.getRotate() + 360, javafx.animation.Interpolator.LINEAR))
            );
            ring1Timeline.setCycleCount(Timeline.INDEFINITE);
            ring1Timeline.play();
        });
        returnToIdle.play();

        String micBase =
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #00f2ff;" +
            "-fx-border-color: #00f2ff;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 10 14;" +
            "-fx-font-size: 16px;" +
            "-fx-cursor: hand;";
        micBtn.setStyle(micBase);
        setListening(false);
    }

    private void startHardwareMonitoring() {
        hwScheduler = Executors.newSingleThreadScheduledExecutor();
        hwScheduler.scheduleAtFixedRate(() -> {
            String cpu  = hardwareMonitor.getCpu();
            String ram  = hardwareMonitor.getRam();
            String disk = hardwareMonitor.getDisk();
            String net  = hardwareMonitor.getNet();

            javafx.application.Platform.runLater(() -> {
                hwMonitor.setText(String.format(
                    "CPU  : %s\nRAM  : %s\nDISK : %s\nNET  : %s",
                    cpu, ram, disk, net
                ));
            });
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void sendPrompt() {
        String prompt = inputField.getText().trim();
        if (prompt.isEmpty()) return;

        historyArea.appendText("> " + prompt + "\n");
        inputField.clear();

        setProcessing(true);
        setStatus("THINKING");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    setStatus("THINKING");
                    updateTaskInfo("Processing request", "", "", "");
                    String response = orchesterAgent.getLLMResponse(prompt);
                    if (isCancelled()) {
                        return null;
                    }
                    fxInterface.sendOutput(response);
                    voiceHandler.speak(response);
                    setStatus("IDLE");
                    updateTaskInfo("--", "--", "--", "--");
                    refreshWorkspaceFileList();
                } catch (Exception e) {
                    fxInterface.sendOutput("[ERROR] " + e.getMessage());
                    setStatus("ERROR");
                }
                return null;
            }
            @Override protected void succeeded() { finishProcessing(); }
            @Override protected void cancelled() {
                finishProcessing();
                setStatus("IDLE");
            }
            @Override protected void failed() {
                finishProcessing();
                fxInterface.sendOutput("[ERROR] " + getException().getMessage());
                setStatus("ERROR");
            }
        };

        currentTask = task;
        Thread t = new Thread(task);
        t.setDaemon(true);
        currentTaskThread = t;
        t.start();
    }

    private void finishProcessing() {
        setProcessing(false);
        setStatus("IDLE");
        currentTask = null;
        currentTaskThread = null;
    }

    private void cancelCurrentTask() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel(true);
            if (currentTaskThread != null) {
                currentTaskThread.interrupt();
            }
            currentTask = null;
            finishProcessing();
            fxInterface.sendOutput("[SYSTEM] Task cancelled by user.");
        }
    }

    private void clearConversation() {
        clearAgentMemory();
        outputArea.clear();
        historyArea.clear();
        stdoutOutput.clear();
        stderrOutput.clear();
        toolCallDisplay.clear();
        fxInterface.sendOutput("[SYSTEM] Conversation cleared.");
    }

    private void clearAgentMemory() {
        if (orchesterAgent != null) {
            orchesterAgent.clearShortMemory();
        }
    }

    private void setProcessing(boolean active) {
        isProcessing = active;
        updateButtonState();
        if (active) {
            statusLabel.setText("THINKING...");
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            pulseCore();
            cancelBtn.setDisable(false);
        } else {
            progressBar.setVisible(false);
            cancelBtn.setDisable(true);
            if (!isListening) {
                statusLabel.setText("SYSTEM IDLE");
            }
        }
    }

    private void setListening(boolean active) {
        isListening = active;
        updateButtonState();
        if (active) {
            statusLabel.setText("LISTENING...");
        } else if (!isProcessing) {
            statusLabel.setText("SYSTEM IDLE");
        }
    }

    private void updateButtonState() {
        boolean disabled = isListening || isProcessing;
        if (micBtn != null) micBtn.setDisable(disabled);
        if (sendBtn != null) sendBtn.setDisable(disabled);
    }

    private void setStatus(String status) {
        javafx.application.Platform.runLater(() -> {
            if (statusIndicator != null) {
                statusIndicator.setText(status);
                switch (status) {
                    case "IDLE" -> statusIndicator.setTextFill(Color.web("#00f2ff"));
                    case "THINKING" -> statusIndicator.setTextFill(Color.web("#ffaa00"));
                    case "RUNNING TOOL" -> statusIndicator.setTextFill(Color.web("#ffaa00"));
                    case "WAITING APPROVAL" -> statusIndicator.setTextFill(Color.web("#ff5555"));
                    case "ERROR" -> statusIndicator.setTextFill(Color.web("#ff5555"));
                    default -> statusIndicator.setTextFill(Color.web("#00f2ff"));
                }
            }
        });
    }

    private void updateTaskInfo(String task, String project, String step, String result) {
        javafx.application.Platform.runLater(() -> {
            if (taskLabel != null) taskLabel.setText("Task: " + task);
            if (projectLabel != null) projectLabel.setText("Project: " + project);
            if (stepLabel != null) stepLabel.setText("Step: " + step);
            if (resultLabel != null) resultLabel.setText("Result: " + result);
        });
    }

    private void refreshWorkspaceFileList() {
        try {
            String listing = com.neuon.tools.WorkspaceManager.listFiles(".");
            javafx.application.Platform.runLater(() -> {
                if (workspaceFileList != null) {
                    workspaceFileList.setText(listing);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private String getModelName() {
        String model = System.getenv("LLM_MODEL");
        return model != null && !model.isBlank() ? model : "openai/gpt-oss-120b";
    }

    private String apiKeyStatusText() {
        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.isBlank()) return "API Key: NOT SET";
        return "API Key: CONFIGURED";
    }

    private void pulseCore() {
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring1.scaleXProperty(), 1),
                new KeyValue(ring1.scaleYProperty(), 1),
                new KeyValue(ring2.scaleXProperty(), 1),
                new KeyValue(ring2.scaleYProperty(), 1),
                new KeyValue(ring3.scaleXProperty(), 1),
                new KeyValue(ring3.scaleYProperty(), 1)
            ),
            new KeyFrame(Duration.millis(250),
                new KeyValue(ring1.scaleXProperty(), 1.15),
                new KeyValue(ring1.scaleYProperty(), 1.15),
                new KeyValue(ring2.scaleXProperty(), 1.12),
                new KeyValue(ring2.scaleYProperty(), 1.12),
                new KeyValue(ring3.scaleXProperty(), 1.1),
                new KeyValue(ring3.scaleYProperty(), 1.1)
            ),
            new KeyFrame(Duration.millis(500),
                new KeyValue(ring1.scaleXProperty(), 1),
                new KeyValue(ring1.scaleYProperty(), 1),
                new KeyValue(ring2.scaleXProperty(), 1),
                new KeyValue(ring2.scaleYProperty(), 1),
                new KeyValue(ring3.scaleXProperty(), 1),
                new KeyValue(ring3.scaleYProperty(), 1)
            )
        );
        pulse.play();
    }

    private void startAnimations() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.LocalTime now = java.time.LocalTime.now();
            clockLabel.setText(String.format("TIME: %02d:%02d:%02d",
                now.getHour(), now.getMinute(), now.getSecond()));
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        ring1Timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(ring1.rotateProperty(), 0, javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(40), new KeyValue(ring1.rotateProperty(), 360, javafx.animation.Interpolator.LINEAR))
        );
        ring1Timeline.setCycleCount(Timeline.INDEFINITE);
        ring1Timeline.play();

        ring2Timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(ring2.rotateProperty(), 0, javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(25), new KeyValue(ring2.rotateProperty(), -360, javafx.animation.Interpolator.LINEAR))
        );
        ring2Timeline.setCycleCount(Timeline.INDEFINITE);
        ring2Timeline.play();

        ring3Timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(ring3.rotateProperty(), 0, javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(15), new KeyValue(ring3.rotateProperty(), 360, javafx.animation.Interpolator.LINEAR))
        );
        ring3Timeline.setCycleCount(Timeline.INDEFINITE);
        ring3Timeline.play();
    }

    private void stopAnimations() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel(true);
        }
        if (currentTaskThread != null) {
            currentTaskThread.interrupt();
        }
        if (hwScheduler != null) hwScheduler.shutdownNow();
        if (clockTimeline != null) clockTimeline.stop();
        if (ring1Timeline != null) ring1Timeline.stop();
        if (ring2Timeline != null) ring2Timeline.stop();
        if (ring3Timeline != null) ring3Timeline.stop();
        if (voicePulseTimeline != null) voicePulseTimeline.stop();
        if (voiceRotateTimeline != null) voiceRotateTimeline.stop();
        if (voiceRotateTimeline2 != null) voiceRotateTimeline2.stop();
        if (voiceRotateTimeline3 != null) voiceRotateTimeline3.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

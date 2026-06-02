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
    private OrchesterAgent orchesterAgent; 
    private Memory memory;
    private VoiceHandler voiceHandler;
    private FXInterface fxInterface;

    @Override
    public void start(Stage stage) {
        
        memory = new Memory();
        voiceHandler = new VoiceHandler();

        StackPane mainRoot = buildUI();

        fxInterface = new FXInterface();

        orchesterAgent = new OrchesterAgent();

        Scene scene = new Scene(mainRoot, 1200, 800);
        scene.setFill(Color.web("#020b10"));

        stage.setTitle("NEUON | AI CORE");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            memory.clearShortMemory();
            stopAnimations();
        });
        scene.setOnKeyPressed(e -> {
        
        if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.M) {
            startVoiceRecording(micBtn);
        }
    });
        stage.show();

        fxInterface.setOutputArea(outputArea);
        fxInterface.sendOutput("[SYSTEM] Ready Boss");
        hardwareMonitor = new HardwareMonitor();
        startHardwareMonitoring();
        startAnimations();
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
        grid.add(topBar, 0, 0, 3, 1);

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
        leftPanel.getChildren().add(hwMonitor);
        VBox.setVgrow(hwMonitor, Priority.ALWAYS);
        grid.add(leftPanel, 0, 1);

        StackPane center = createCenterArea();
        grid.add(center, 1, 1);

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
        grid.add(rightPanel, 2, 1);
    

        HBox bottomBar = createBottomBar();
        grid.add(bottomBar, 0, 2, 3, 1);

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(25);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(25);
        grid.getColumnConstraints().addAll(c1, c2, c3);

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

        bar.getChildren().addAll(coreId, clockLabel, emu);
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

        // ── Thin progress bar, positioned below the title ──
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

        // ── SEND button ──
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

        // ── MIC button ──
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

        HBox inputBox = new HBox(8, inputField, sendBtn, micBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(8, 0, 0, 0));
        inputPanel.getChildren().add(inputBox);

        bar.getChildren().addAll(histPanel, inputPanel);
        HBox.setHgrow(histPanel, Priority.ALWAYS);
        HBox.setHgrow(inputPanel, Priority.ALWAYS);
        return bar;
    }
    private void startVoiceRecording(Button micBtn) {
        // ── Visual feedback: mic button turns red ──
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

        // ── 1. PULSE: Grow and STAY expanded ──
        // Animate from idle size to expanded, then hold
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
        voicePulseTimeline.play();

        // ── 2. OUTER RING: Rotate faster + get thicker ──
        // Stop the slow idle rotation
        if (ring1Timeline != null) ring1Timeline.stop();

        // Thicken the outer ring
        ring1.setStrokeWidth(4); // was 1, now 4px thick

        // Fast continuous rotation
        voiceRotateTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring1.rotateProperty(), ring1.getRotate(), javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(2), // 2 seconds per full rotation = fast
                new KeyValue(ring1.rotateProperty(), ring1.getRotate() + 360, javafx.animation.Interpolator.LINEAR))
        );
        voiceRotateTimeline.setCycleCount(Timeline.INDEFINITE);
        voiceRotateTimeline.play();

        // Fast rotation for ring2 (counterclockwise)
        voiceRotateTimeline2 = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring2.rotateProperty(), ring2.getRotate(), javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(ring2.rotateProperty(), ring2.getRotate() - 360, javafx.animation.Interpolator.LINEAR))
        );
        voiceRotateTimeline2.setCycleCount(Timeline.INDEFINITE);
        voiceRotateTimeline2.play();

        // Fast rotation for ring3 (clockwise, opposite to ring2)
        voiceRotateTimeline3 = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ring3.rotateProperty(), ring3.getRotate(), javafx.animation.Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(ring3.rotateProperty(), ring3.getRotate() + 360, javafx.animation.Interpolator.LINEAR))
        );
        voiceRotateTimeline3.setCycleCount(Timeline.INDEFINITE);
        voiceRotateTimeline3.play();

        // ── 3. Optional: Cyan-to-red color shift on outer ring ──
        ring1.setStroke(Color.web("#00f2ff")); // Red while recording

        // ── Start voice recording in background ──
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
        // ── Smooth return to idle ──
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
                new KeyValue(ring1.scaleXProperty(), 0.5, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring1.scaleYProperty(), 0.5, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring2.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring2.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring3.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring3.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring1.strokeWidthProperty(), originalRing1StrokeWidth, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(ring1.rotateProperty(), ring1.getRotate() + 90, javafx.animation.Interpolator.EASE_IN) // coast to stop
            )
        );
        returnToIdle.setOnFinished(e -> {
            // Restore idle rotation
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
            ring1.setStroke(Color.web("#00f2ff")); // Back to cyan
            ring1.setStrokeWidth(originalRing1StrokeWidth);

            // Restart slow idle rotation
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

        // Reset mic button
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
            // Read from /proc (background thread — file I/O is safe here)
            String cpu  = hardwareMonitor.getCpu();
            String ram  = hardwareMonitor.getRam();
            String disk = hardwareMonitor.getDisk();
            String net  = hardwareMonitor.getNet();

            // Push to UI (JavaFX thread only!)
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

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    String response = orchesterAgent.getLLMResponse(prompt);
                    fxInterface.sendOutput(response);
                    voiceHandler.speak(response);
                } catch (Exception e) {
                    fxInterface.sendOutput("[ERROR] " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
            @Override protected void succeeded() { setProcessing(false); }
            @Override protected void failed() {
                setProcessing(false);
                fxInterface.sendOutput("[ERROR] " + getException().getMessage());
            }
        };

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void setProcessing(boolean active) {
        isProcessing = active;
        updateButtonState();
        if (active) {
            statusLabel.setText("THINKING...");
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            pulseCore();
        } else {
            progressBar.setVisible(false);
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
        if (hwScheduler != null) hwScheduler.shutdown();
        if (clockTimeline != null) clockTimeline.stop();
        if (ring1Timeline != null) ring1Timeline.stop();
        if (ring2Timeline != null) ring2Timeline.stop();
        if (ring3Timeline != null) ring3Timeline.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

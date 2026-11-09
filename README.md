# NEUON — Linux AI Assistant (Java)

<p align="center">
  <img src="https://cdn.simpleicons.org/java/ED8B00" width="36" height="36" alt="Java" />
  <img src="https://cdn.simpleicons.org/openjdk/007396" width="36" height="36" alt="OpenJDK" />
  <img src="https://cdn.simpleicons.org/javafx/2196F3" width="36" height="36" alt="JavaFX" />
  <img src="https://cdn.simpleicons.org/gson/4285F4" width="36" height="36" alt="Gson" />
  <img src="https://cdn.simpleicons.org/vosk/000000" width="36" height="36" alt="Vosk" />
</p>

NEUON is a desktop AI assistant for Linux, written in Java 21 with a JavaFX frontend and an LLM backend. It runs as an agent loop that can execute shell commands, read/write files, run code, send email, search YouTube, maintain short- and long-term memory, and speak back through voice — all from a cyberpunk-style control panel.

## What it does

- **Agent loop** — `OrchesterAgent` drives an LLM (Groq, default `openai/gpt-oss-120b`) through a tool-enabled orchestration loop.
- **Tool registry** — shell, code agent, file read/write/edit, directory creation, email, YouTube search, short/long-term memory, and exit.
- **Voice** — Vosk speech-to-text input (`SPEAK` / `Ctrl+M`) and text-to-speech output.
- **Hardware monitor** — live CPU, RAM, disk, and network readout in the UI.
- **Workspace** — file listing and project directory management from inside the assistant.
- **JavaFX UI** — dark cyberpunk panel with animated core rings, clock, status indicators, command history, stdout/stderr panes, and voice pulse animations.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| UI | JavaFX 21 (base, controls, fxml, web) |
| LLM client | Groq (`GROQ_API_KEY`) |
| JSON | Gson 2.13.2 |
| STT | Vosk 0.3.45 |
| Mail | Jakarta Mail |
| PTY | pty4j 0.13.10 |
| Audio | JLayer 1.0.1 |
| Tests | JUnit 5 |

## Build & run

Requires Java 21 and Maven.

```bash
# Clone
git clone https://github.com/MehdiCherkane/NEUON-Linux-AI-Assistant-With-JAVA.git
cd NEUON-Linux-AI-Assistant-With-JAVA

# Build
mvn -q clean package

# Run (needs GROQ_API_KEY set)
export GROQ_API_KEY=your_key_here
mvn -q javafx:run
```

Or run the entry point directly:

```bash
mvn -q exec:java -Dexec.mainClass=com.neuon.Neuon
```

## UI tour

| Panel | Purpose |
|---|---|
| CORE_ID / clock | Session identity and live time |
| API dot | Green when `GROQ_API_KEY` is configured |
| Hardware Monitor | CPU / RAM / disk / network, refreshed every second |
| Workspace Files | Listing of the current working directory |
| Status | IDLE / THINKING / RUNNING TOOL / WAITING APPROVAL / ERROR |
| Neural Output | LLM responses streamed in |
| Command History | Sent prompts |
| Input | Text field (`FIRE` to send) + `SPEAK` voice button (`Ctrl+M`) |
| STDOUT / STDERR | Tool command output streams |

## Configuration

Set these environment variables before running:

| Variable | Purpose | Default |
|---|---|---|
| `GROQ_API_KEY` | Groq API key for the LLM backend | *(required)* |
| `LLM_MODEL` | Model identifier | `openai/gpt-oss-120b` |

## Project structure

```
NEUON-Linux-AI-Assistant-With-JAVA/
├── pom.xml                      # Maven build (Java 21, JavaFX, deps)
├── src/main/java/Neuon.java     # Entry point
├── src/main/java/com/neuon/UI/  # JavaFX interface (NeuonFXApp, voice, panels)
├── src/main/java/com/neuon/agent/ # Orchester agent, code agent, prompt orchestra
├── src/main/java/com/neuon/core/  # LLM client, memory, message/response building
├── src/main/java/com/neuon/tools/ # Tool registry, dispatch, runners, handlers
├── src/main/resources/          # LongTermMemory.json
├── src/test/java/com/neuon/     # JUnit 5 tests
├── docs/architecture.html       # Architecture overview
└── lib/gson-2.13.2.jar          # Vendored Gson
```

## License

MIT — see [LICENSE](LICENSE).

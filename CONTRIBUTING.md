# Contributing to NEUON

Thanks for taking the time to contribute! NEUON is a Java 21 desktop AI assistant. Here is how to get started.

## Ground rules

- Follow the existing code style and package layout (`com.neuon.ui`, `com.neuon.agent`, `com.neuon.core`, `com.neuon.tools`).
- Java 21 is the target; keep source/target compatibility at 21.
- Add JUnit 5 tests under `src/test/java/com/neuon/` for new behavior.
- Keep secrets out of commits: never commit `GROQ_API_KEY` or any credentials.
- The UI is JavaFX; avoid blocking the JavaFX Application Thread — run long work on background threads (`Task`, `ExecutorService`).

## Workflow

1. Fork the repo and create a branch from `main`.
2. Make your change, add tests, and run the build:

   ```bash
   mvn -q clean verify
   ```

3. Open a pull request against `main` with a clear description of the change.
4. A maintainer will review and merge.

## Reporting issues

Open an issue with:
- Java and Maven versions
- OS (Linux target)
- Steps to reproduce
- Expected vs. actual behavior
- Any relevant logs from the STDOUT/STDERR panes

## License

By contributing, you agree your contributions are licensed under the MIT License.

package app.galaxybypass;

interface IShellService {
    // Reserved by Shizuku: called when the service must stop.
    void destroy() = 16777114;

    // Runs one allow-listed command as the Shizuku identity (shell/ADB).
    // Returns "<exitCode>\n<output>".
    String exec(String command) = 1;
}

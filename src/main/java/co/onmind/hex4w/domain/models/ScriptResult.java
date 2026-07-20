package co.onmind.hex4w.domain.models;

public class ScriptResult {
    private final Object value;
    private final String stdout;
    private final String stderr;

    public ScriptResult(Object value, String stdout, String stderr) {
        this.value = value;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public Object value() { return value; }
    public String stdout() { return stdout; }
    public String stderr() { return stderr; }
}
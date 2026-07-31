package co.onmind.hex4w.domain.exceptions;

public class ScriptNotAllowedException extends RuntimeException {

    private final String requestedFile;
    private final String allowedFiles;

    public ScriptNotAllowedException(String requestedFile, String allowedFiles) {
        super("Script file not allowed: '" + requestedFile + "'. Allowed: " + allowedFiles);
        this.requestedFile = requestedFile;
        this.allowedFiles = allowedFiles;
    }

    public String getRequestedFile() {
        return requestedFile;
    }

    public String getAllowedFiles() {
        return allowedFiles;
    }
}

package fjwright.runreduce;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class REDUCEManual {
    private static String manualToC;
    private static Path manualDirPath;

    private static class REDUCEManualException extends IOException {
        REDUCEManualException(String message) {
            super(message);
        }
    }

    /**
     * Get the cached HTML REDUCE Manual ToC, creating it if necessary.
     */
    public static String getToC() throws IOException {
        Path manualDirPathNew = Path.of(RunREDUCE.reduceConfiguration.manualDir);
        if (manualToC == null || !manualDirPath.equals(manualDirPathNew)) {
            manualDirPath = manualDirPathNew;
            Path manualIndexFileName = manualDirPath.resolve("manual.html");
            try {
                manualToC = Files.readString(manualIndexFileName);
            } catch (IOException e) {
                RunREDUCE.alert(Alert.AlertType.ERROR,
                        "REDUCE Manual Error",
                        "Cannot read HTML index file at\n"
                                + manualIndexFileName);
                throw e;
            }
            String searchText = "<div class=\"tableofcontents\">";
            int start = manualToC.indexOf(searchText);
            if (start != -1) {
                start += searchText.length();
                int finish = manualToC.indexOf("</div>", start);
                if (finish != -1) manualToC = manualToC.substring(start, finish);
            } else {
                RunREDUCE.alert(Alert.AlertType.ERROR,
                        "REDUCE Manual Error",
                        "Cannot find table of contents in HTML index file at\n"
                                + manualIndexFileName);
                throw new REDUCEManualException("Cannot find table of contents.");
            }
        }
        return manualToC;
    }

    /**
     * Get the REDUCE Manual Directory as a Path.
     */
    public static Path getDirPath() throws IOException {
        if (manualDirPath == null)
            throw new REDUCEManualException("Null manualDirPath in class REDUCEManual.");
        return manualDirPath;
    }
}

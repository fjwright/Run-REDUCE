module Run.REDUCE.FX {
    requires java.desktop;
    requires java.prefs;
    requires javafx.controls;
    requires javafx.fxml;
    opens fjwright.runreduce to javafx.fxml;
    opens fjwright.runreduce.templates to javafx.fxml;
    exports fjwright.runreduce;
}

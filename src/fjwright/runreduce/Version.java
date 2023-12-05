package fjwright.runreduce;

/**
 * Version numbering compatible with installers.
 * Normal release versions have have revision = 0, which is omitted.
 * Release date as month and year, and copyright string.
 */
public class Version {
    private final static int MAJOR = 3;
    private final static int MINOR = 1;
    private final static int REVISION = 2;
    private final static String MONTH = "December";
    private final static int YEAR = 2023;

    // Excess format arguments are ignored.
    public static final String VERSION = String.format(REVISION == 0 ? "%d.%d" : "%d.%d.%d",
            MAJOR, MINOR, REVISION);
    static final String DATE = String.format("%s %d", MONTH, YEAR);
    static final String COPYRIGHT = String.format("© 2020‒%d, Francis Wright", YEAR);

    static final String JAVA = String.format(
            "Compiled using Java 17.0.9 & JavaFX 17.0.9.\n" +
                    "Run using Java %s & JavaFX %s.",
            System.getProperty("java.version"),
            System.getProperty("javafx.version"));

//    static {
//        System.err.println(System.getProperties());
//    }

    /**
     * This program is used to set the version in the installer builders.
     */
    public static void main(String[] args) {
        // Do not output a newline here!
        System.out.print(Version.VERSION);
    }
}

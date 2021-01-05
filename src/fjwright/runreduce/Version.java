package fjwright.runreduce;

/**
 * Version numbering compatible with installers.
 * Normal release versions have have revision = 0, which is omitted.
 * Release date as month and year, and copyright string.
 */
public class Version {
    private final static int major = 2;
    private final static int minor = 7;
    private final static int revision = 0;
    private final static String month = "January";
    private final static int year = 2021;

    // Excess format arguments are ignored.
    static String version = String.format(revision == 0 ? "%d.%d" : "%d.%d.%d",
            major, minor, revision);
    static String date = String.format("%s %d", month, year);
    static String copyright = String.format("© 2020‒%d, Francis Wright", year);
}

package com.tsoft.jenkins.plugin.rejson;

/**
 * Path is a ReJSON path, representing a valid path into an object
 */
class Path {

    public static final Path ROOT_PATH = new Path(".");

    private final String strPath;

    public Path(final String strPath) {
        this.strPath = strPath;
    }

    /**
     * Makes a root path
     * @return the root path
     * @deprecated use {@link #ROOT_PATH} instead
     */
    @Deprecated
    public static Path RootPath() {
        return new Path(".");
    }

    @Override
    public String toString() {
        return strPath;
    }
}


package gq.luma.render.engine;

public enum SrcGame {
    PORTAL2("portal2", 620);

    private String directoryName;
    private int appCode;

    SrcGame(String directoryName, int appCode) {
        this.directoryName = directoryName;
        this.appCode = appCode;
    }

    public int getAppCode() {
        return appCode;
    }

    public String getDirectoryName() {
        return directoryName;
    }
}

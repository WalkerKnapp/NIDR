package gq.luma.render.engine;

public enum SrcGame {
    PORTAL2("portal2", 620),
    PORTAL("portal", 400),
    PORTAL_STORIES_MEL("portal_stories", 317400);

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

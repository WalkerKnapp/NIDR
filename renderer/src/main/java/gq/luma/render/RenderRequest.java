package gq.luma.render;

import gq.luma.render.engine.SrcDemo;

import java.util.ArrayList;

public class RenderRequest {
    private ArrayList<SrcDemo> demos;
    private RenderSettings settings;
    private String status;

    public RenderRequest(ArrayList<SrcDemo> demos, RenderSettings settings){
        this.demos = demos;
        this.settings = settings;
    }

    public ArrayList<SrcDemo> getDemos() {
        return demos;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RenderSettings getSettings() {
        return settings;
    }
}

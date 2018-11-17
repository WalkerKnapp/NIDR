package gq.luma.render;

import gq.luma.render.engine.SrcDemo;

import java.util.ArrayList;

public class RenderRequest {
    private ArrayList<SrcDemo> demos;
    private String status;

    public ArrayList<SrcDemo> getDemos() {
        return demos;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}

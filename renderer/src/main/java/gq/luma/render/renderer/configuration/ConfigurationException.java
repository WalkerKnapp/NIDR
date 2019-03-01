package gq.luma.render.renderer.configuration;

public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String s){
         super(s);
    }

    public ConfigurationException(String s, Exception cause){
        super(s, cause);
    }
}

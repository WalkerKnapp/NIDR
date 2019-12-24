package gq.luma.render.engine;

import io.wkna.sdp.SourceDemo;
import io.wkna.sdp.messages.DemoMessage;
import io.wkna.sdp.messages.PacketMessage;
import io.wkna.sdp.messages.SignOnMessage;

public class DemoUtils {
    public static int getFirstPlaybackTick(SourceDemo demo) {
        for(DemoMessage message : demo.getMessages()) {
            if (message instanceof PacketMessage && !(message instanceof SignOnMessage) && message.getTick() != 0) {
                return message.getTick();
            }
        }

        throw new IllegalArgumentException("Demo provided with 0 ticks.");
    }
}

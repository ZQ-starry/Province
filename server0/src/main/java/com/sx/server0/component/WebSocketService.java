package com.sx.server0.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate template;

    public void sendTextMsg(String destination, Object message) {

        template.convertAndSend(destination, message);
    }

    public void sendTextMsgToUser(String user, String destination, Object message) {

        template.convertAndSendToUser(user, destination, message);

    }

}

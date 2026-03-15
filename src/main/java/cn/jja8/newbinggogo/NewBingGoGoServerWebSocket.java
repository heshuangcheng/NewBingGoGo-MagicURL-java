package cn.jja8.newbinggogo;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NewBingGoGoServerWebSocket extends NanoWSD.WebSocket {
    private static final String[] FORWARD_HEADER_KEYS = {"cookie", "user-agent", "accept-language"};
    NewBingGoGoClientWebSocket newBingGoGoClientWebSocket;
    LinkedList<String> messList = new LinkedList<>();
    ScheduledExecutorService scheduledExecutorService;
    ScheduledFuture<?> task;

    public NewBingGoGoServerWebSocket(NanoHTTPD.IHTTPSession handshakeRequest, ScheduledExecutorService scheduledExecutorService) {
        super(handshakeRequest);
        URI url;
        try {
            url = new URI("wss://sydney.bing.com/sydney/ChatHub");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);//这个异常这辈子都不会出的
        }
        this.scheduledExecutorService = scheduledExecutorService;
        Map<String, String> sessionHeaders = handshakeRequest.getHeaders();
        Map<String, String> forwardHeaders = new HashMap<>();
        for (String key : FORWARD_HEADER_KEYS) {
            String value = sessionHeaders.get(key);
            if (value != null) {
                forwardHeaders.put(key, value);
            }
        }
        newBingGoGoClientWebSocket = new NewBingGoGoClientWebSocket(url,this,messList,forwardHeaders);
    }

    @Override
    protected void onOpen() {
        task = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!isOpen()) {
                task.cancel(false);
                return;
            }
            try {
                ping(new byte[1]);
            } catch (IOException e) {
                task.cancel(false);
            }
        },2,2, TimeUnit.SECONDS);
        newBingGoGoClientWebSocket.connect();
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        newBingGoGoClientWebSocket.close();
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        if(newBingGoGoClientWebSocket.isOpen()){
            newBingGoGoClientWebSocket.send(message.getTextPayload());
        }else {
            messList.addLast(message.getTextPayload());
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {

    }

    @Override
    protected void onException(IOException exception) {
        newBingGoGoClientWebSocket.close();
    }
}

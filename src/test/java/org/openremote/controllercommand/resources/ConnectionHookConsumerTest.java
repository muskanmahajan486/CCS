package org.openremote.controllercommand.resources;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { ClientBuilder.class })
public class ConnectionHookConsumerTest
{
    private static final String BASE_URI = "mock://base";
    private static final String OPEN_PATH = "/open";
    private static final String CLOSE_PATH = "/close";
    private static final long RETRY_TIMEOUT = 3000;

    private ControllerSessionHandler.ShudownAware isShutdown;
    private Map sessions = null;

    private Response response;
    private Payload payload;

    private ConnectionHookConsumer consumer;
    private BlockingQueue<Payload> queue;
    private WebTarget webTarget;


    @Before
    public void setUp() throws Exception {

        isShutdown = new ControllerSessionHandler.ShudownAware() {
            int runTime = 1;
            @Override
            public boolean isShutdownInProgress() {
                return runTime-- <= 0;
            }
        };
        sessions = new HashMap<>();
        queue = mock(BlockingQueue.class);
        payload = new Payload("user1",0l,"127.0.0.1");
        when(queue.poll(15, TimeUnit.SECONDS)).thenReturn(payload);
        Client client = mock(Client.class);
        PowerMockito.mockStatic(ClientBuilder.class);
        when(ClientBuilder.newClient()).thenReturn(client);
        webTarget = mock(WebTarget.class);
        when(webTarget.path(CLOSE_PATH)).thenReturn(webTarget);
        when(webTarget.path(OPEN_PATH)).thenReturn(webTarget);
        when(client.target(BASE_URI)).thenReturn(webTarget);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        response = mock(Response.class);
        when(builder.post(Entity.json(payload))).thenReturn(response);
        consumer = new ConnectionHookConsumer(queue, BASE_URI, OPEN_PATH, CLOSE_PATH, sessions, isShutdown, RETRY_TIMEOUT);
    }



    @Test
    @PrepareForTest(ClientBuilder.class)
    public void openOk() throws InterruptedException {
        when(response.getStatus()).thenReturn(200);
        sessions.put("user1",new Object());
        consumer.run();
        verify(queue, never()).put(payload);
        verify(webTarget).path(OPEN_PATH);
    }


    @Test
    @PrepareForTest(ClientBuilder.class)
    public void open404() throws InterruptedException {
        when(response.getStatus()).thenReturn(404);
        sessions.put("user1",new Object());
        consumer.run();
        verify(queue, never()).put(payload);
        verify(webTarget).path(OPEN_PATH);

    }

    @Test
    @PrepareForTest(ClientBuilder.class)
    public void open300() throws InterruptedException {
        when(response.getStatus()).thenReturn(300);
        sessions.put("user1",new Object());
        consumer.run();
        verify(webTarget).path(OPEN_PATH);
        verify(queue).put(payload);
    }



    @Test
    @PrepareForTest(ClientBuilder.class)
    public void closeOk() throws InterruptedException {
        when(response.getStatus()).thenReturn(200);
        consumer.run();
        verify(webTarget).path(CLOSE_PATH);
        verify(queue, never()).put(payload);
    }




    @Test
    @PrepareForTest(ClientBuilder.class)
    public void close404() throws InterruptedException {
        when(response.getStatus()).thenReturn(404);
        consumer.run();
        verify(webTarget).path(CLOSE_PATH);
        verify(queue, never()).put(payload);
    }


    @Test
    @PrepareForTest(ClientBuilder.class)
    public void close300() throws InterruptedException {
        when(response.getStatus()).thenReturn(300);
        consumer.run();
        verify(webTarget).path(CLOSE_PATH);
        verify(queue).put(payload);
    }

    @Test
    @PrepareForTest(ClientBuilder.class)
    public void requestException() throws InterruptedException {
        consumer.run();
        verify(queue).put(payload);
    }

}

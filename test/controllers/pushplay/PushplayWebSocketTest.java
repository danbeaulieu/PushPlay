package controllers.pushplay;


import com.google.gson.*;
import org.junit.Test;

import play.modules.pushplay.Message;
import play.modules.pushplay.Payload;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushplayWebSocketTest {

	@Test
	public void testGson() {
		String data = "{\"hit\":\"one\"}";
		System.out.println(data);
		String temp = "{\"channel\":\"foo\",\"event\":\"foo_update\",\"data\":" + data + "}";
		System.out.println(temp);
		System.out.println(new JsonParser().parse(temp).getAsJsonObject().get("data"));
	}

    @Test
    public void testJSONObject() {

        Message outgoing = new Message();
        outgoing.setEvent("pusher:connection_established");
        outgoing.setData(new Payload("123456789"));
        System.out.println(new Gson().toJson(outgoing));
        String txt = new Gson().toJson(outgoing);
        System.out.println(txt);
        GsonBuilder gson_builder = new GsonBuilder();
        gson_builder.registerTypeAdapter(
                JsonObject.class,
                new JsonDeserializer<JsonElement>() {
                    @Override
                    public JsonElement deserialize(JsonElement arg0,
                                                   Type arg1,
                                                   JsonDeserializationContext arg2)
                            throws JsonParseException {

                        return arg0.getAsJsonObject();
                    }
                } );

        Message incoming = gson_builder.create().fromJson(txt, Message.class);
        System.out.println(incoming.getData().socket_id);
    }

    class MockMessage {

        public String socket_id;

        public JsonObject data;

        public String channel;
    }
}

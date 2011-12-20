package controllers.pushplay;


import org.junit.Test;

import play.modules.pushplay.Message;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class PushplayWebSocketTest {

	@Test
	public void testGson() {
		String data = "{\"hit\":\"one\"}";
		System.out.println("{\"channel\":\"foo\",\"event_name\":\"foo_update\",\"data\":" + data + "}");
		System.out.println(new JsonParser().parse("{\"channel\":\"foo\",\"event_name\":\"foo_update\",\"data\":" + data + "}"));
		Message incoming = new Gson().fromJson(new JsonParser().parse("{\"channel\":\"foo\",\"event_name\":\"foo_update\",\"data\":" + data + "}"), Message.class);
		System.out.println(incoming.getChannel());
		System.out.println(incoming.getData());
		System.out.println(new Gson().toJson(incoming));
	}
}

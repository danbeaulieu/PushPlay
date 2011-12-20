package controllers.pushplay;


import org.junit.Test;

import play.modules.pushplay.Message;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class PushplayWebSocketTest {

	@Test
	public void testGson() {
		String data = "{\"hit\":\"one\"}";
		System.out.println(data);
		String temp = "{\"channel\":\"foo\",\"event\":\"foo_update\",\"data\":" + data + "}";
		System.out.println(temp);
		System.out.println(new JsonParser().parse(temp).getAsJsonObject().get("data"));
	}
}

package play.modules.pushplay;

import java.util.HashMap;
import java.util.Map;


import play.Play;
import play.libs.F.EventStream;
import play.mvc.Http.Inbound;

public class PushPlayPlugin {

	public static EventStream<Message> stream = new EventStream<Message>();
	
	public static String appId = Play.configuration.getProperty("pusher.appId");
    
	public static String key = Play.configuration.getProperty("pusher.key");
	
	public static String secret = Play.configuration.getProperty("pusher.secret");
    
	/*
    public static Map<String, EventStream<String>> channels = new HashMap<String, EventStream<String>>();
    
	public static EventStream<String> getHitStream(String channel) {
		
		if (!channels.containsKey(channel)) {
			EventStream<String> e = new EventStream<String>();
			channels.put(channel, e);
		}
		
		return channels.get(channel);
	} */
}

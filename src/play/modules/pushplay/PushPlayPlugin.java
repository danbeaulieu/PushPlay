package play.modules.pushplay;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;


import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.F.EventStream;
import play.modules.hazelcast.HazelcastPlugin;
import play.mvc.Http.Inbound;

public class PushPlayPlugin extends PlayPlugin {

	public static Map<String, EventStream<String>> streams = new HashMap<String, EventStream<String>>();
	
	public static HazelcastInstance hazel;
	
	static {
		
		String appId = Play.configuration.getProperty("pusher.appId");
    
		String key = Play.configuration.getProperty("pusher.key");
	
		String secret = Play.configuration.getProperty("pusher.secret");	
	}
	
	@Override
	public void onApplicationStart() {
		
		Logger.info("Initializing PushPlayPlugin on %s", Play.id);
		hazel = HazelcastPlugin.getHazel();
		
		if (hazel != null) {
			
			ITopic<String> topic = hazel.getTopic("pushplay");
			topic.addMessageListener(new MessageListener<String>() {
				public void onMessage(String msg) {
					TopicMessage incoming = new Gson().fromJson(msg, TopicMessage.class);
					if (!incoming.playServerId.equals(Play.id)) {
						publishToAllEventStreams(incoming.message);
						Logger.debug("Message received via Hazelcast Topic - %s", msg);
					}
				}
			});
		}
		else {
			Logger.warn("hazel instance is not available, are you sure it isn't disabled in the conf?");
		}
	}
	private static void publishToAllEventStreams(Message msg) {
        publishToAllEventStreams(new Gson().toJson(msg));
    }

    private static void publishToAllEventStreams(String msg) {
        for (EventStream<String> es: streams.values()) {
            es.publish(msg);
        }
    }

	public static void publishMessage(Message msg) {
        publishMessage(new Gson().toJson(msg));
    }

    public static void publishMessage(String msg) {
		
		// we publish locally first even though we'll get it back when we publish to the topic
		// just in case there is a hazelcast problem this app server will still be able to handle 
		// messages
        publishToAllEventStreams(msg);

        // don't publish to cluster internal messages
		if (hazel != null && !msg.contains("pusher_internal:subscription_succeeded")) {
			ITopic<String> topic = hazel.getTopic("pushplay");
			Logger.debug("Publishing message: %s", msg);
			TopicMessage topicMessage = new TopicMessage(Play.id, msg);
			topic.publish(new Gson().toJson(topicMessage));	
		}
	}
	
	public static String getSecret() {
		
		return Play.configuration.getProperty("pusher.secret");
	}
}

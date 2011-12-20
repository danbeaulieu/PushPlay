package controllers.pushplay;

import static play.libs.F.Matcher.ClassOf;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import play.Logger;
import play.libs.F.Either;
import play.libs.F.Promise;
import play.modules.pushplay.Message;
import play.modules.pushplay.PushPlayPlugin;
import play.modules.pushplay.PushPlayUtil;
import play.modules.pushplay.TriggerEventMessage;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.WebSocketController;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class PushPlayWebSocket extends Controller {
	
	public static void apps(String appId, String channel) throws IOException {
		
		TriggerEventMessage tem = new TriggerEventMessage(params);
		String message = IOUtils.toString(request.body, "UTF-8");
		
		// first, make sure our request sha256'd locally match the signature
		if (!PushPlayUtil.isRequestValid(request, tem)) {
			badRequest();
		}
		
		// make sure md5s match
		if (!PushPlayUtil.isMD5Valid(message, tem)) {
			badRequest();
		}
		
		// not older than 10 minutes
		if ((System.currentTimeMillis() / 1000) - Long.parseLong(tem.auth_timestamp) > 600) {
			badRequest();
		}
		
		// do the damn thing
		// need to convert message, which is json key/value pairs to map
		Map<String, String> map = new Gson().fromJson(message, new TypeToken<Map<String, String>>() {}.getType());
		PushPlayPlugin.stream.publish(new Message(channel, tem.name, map, tem.socket_id));
		
		response.status=Http.StatusCode.ACCEPTED;
		renderText("");
	}
	
	public static class StreamSocket extends WebSocketController {

		public static String authToken(String socket_id, String channel, String channel_data) {
			
			String string_to_sign = socket_id + ":" + channel;
			
			if (string_to_sign != null) {
				string_to_sign = string_to_sign + ":" + channel_data;
			}
			
			return PushPlayUtil.sha256(string_to_sign, PushPlayPlugin.secret);
		}
		
		/**
		* Subscribe
		*/
		public static void app(String apiKey) {
		    
			Logger.info("Got connection %s", apiKey);
			Set<String> subscriptions = new HashSet<String>();
			Message outgoing = new Message();
			final String socket_id = UUID.randomUUID().toString();
			
			if (outbound.isOpen()) {
				Logger.info("outbound open");
				outgoing.setEvent("pusher:connection_established");
				outgoing.setData(new HashMap<String, String>() {{ put("socket_id", socket_id); }});
				
				Logger.info("sending connection established");
				outbound.sendJson(outgoing);
			}
			else {
				Logger.error("ws outbound not open %s");
			}
			
			while (inbound.isOpen()) {
				
				try {
					Either<Http.WebSocketEvent, Message> e = await(Promise.waitEither(inbound.nextEvent(), PushPlayPlugin.stream.nextEvent()));
					
					for (String txt : Http.WebSocketEvent.TextFrame.match(e._1)) {
						Message incoming = new Gson().fromJson(new JsonParser().parse(txt), Message.class);
						Logger.info("Incoming Message %s", txt);
						outgoing.clear();
						outgoing.setChannel(incoming.getChannel());
						
						if (incoming.getEvent().equals("pusher:subscribe")) {
							Logger.info("Subscribing to %s", incoming.getChannel());
							String auth = incoming.getData().get("auth");
							if (auth != null && !authToken(socket_id, incoming.getChannel(), incoming.getData().get("channel_data")).equals(auth.split(":")[1])) {
								// private channel failure
								outgoing.setEvent("pusher:error");
							} else {
								subscriptions.add(incoming.getChannel());
								outgoing.setEvent("pusher_internal:subscription_succeeded");
							}
						}
						else if (incoming.getEvent().equals("pusher:unsubscribe")) {
							subscriptions.remove(incoming.getChannel());
							//outgoing.setEvent_name("unsubscribe_succeeded");
						}
						else {
							Logger.warn("Unrecognized event [%s]", incoming.getEvent());
						}
						
						if (outgoing.getEvent() != null) {
							PushPlayPlugin.stream.publish(outgoing);
						}
					}
					 
					for (Message message : ClassOf(Message.class).match(e._2)) {
						Logger.info("Publishing Event %s to Outbound Subscribers", 1);
						// if the socket created the message, don't push it out
						if (message.getSocket_id() != null && message.getSocket_id().equals(socket_id)) continue;
						
						// only send messages if we are subscribed to it.
						if (message.getChannel() != null && subscriptions.contains(message.getChannel())) {
							//outbound.send(message.toString());
							outbound.sendJson(message);
						}
					}
					
				} catch (Throwable t) {
					Logger.error("Caught error", t);
				}
			}
		}
	}
}

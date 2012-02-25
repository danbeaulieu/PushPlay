package controllers.pushplay;

import static play.libs.F.Matcher.ClassOf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import play.Logger;
import play.Play;
import play.PlayPlugin;
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
		PushPlayPlugin.publishMessage(new Message(channel, tem.name, map, tem.socket_id));
		
		response.status=Http.StatusCode.ACCEPTED;
		renderText("");
	}
	
	public static class StreamSocket extends WebSocketController {
		
        private static Map<String, Map<String, String>> presence = PushPlayPlugin.hazel.getMap("presence");

		/**
		* Subscribe
		*/
		public static void app(String apiKey) {
			
            Logger.info("Got connection api key=[%s]", apiKey);
			Set<String> subscriptions = new HashSet<String>();
			Message outgoing = new Message();
			final String socket_id = UUID.randomUUID().toString();
			
			if (outbound.isOpen()) {
				Logger.info("outbound open, socket id=[%s]", socket_id);
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
						Logger.info("Incoming Message - %s", txt);
						Message incoming = new Gson().fromJson(txt, Message.class);
						
						outgoing.clear();
						String channel = incoming.getChannel();
						if (channel == null) {
							channel = incoming.getData().get("channel");
						}
						
						if (channel == null) {
							Logger.error("No channel provided in message %s", txt);
							continue;
						}
						outgoing.setChannel(channel);
						
						if (incoming.getEvent().equals("pusher:subscribe")) {
							Logger.info("Subscribing to %s", channel);
							String auth = incoming.getData().get("auth");
							if (auth != null && !PushPlayUtil.authToken(socket_id, channel, incoming.getData().get("channel_data"))
									.equals(auth.split(":")[1])) {
								// private/presence channel failure
								outgoing.setEvent("pusher:error");
							} else {
                                outgoing.setEvent("pusher_internal:subscription_succeeded");
								subscriptions.add(channel);
                                if (channel.startsWith("presence-")) {
                                    Map<String, String> members;
                                    if (!presence.containsKey(channel)) {
                                        presence.put(channel, new HashMap<String, String>());
                                    }
                                    members = presence.get(channel);
                                    outgoing.setData(members);
                                    // TODO if this is presence channel, need to send a member add message - http://pusher.com/docs/client_api_guide/client_presence_events
                                }
							}
						}
						else if (incoming.getEvent().equals("pusher:unsubscribe")) {
							// TODO if this is presence channel, need to send a member remove message - http://pusher.com/docs/client_api_guide/client_presence_events
							subscriptions.remove(channel);
						}
                        else if (incoming.getEvent().startsWith("client-")) {
                            // can only trigger events if this user is subscribed to the channel
                            // channel must be presence or private
                            if (subscriptions.contains(channel) && isAuthenticated(channel)) {
                                // TODO handle client- events - http://pusher.com/docs/client_api_guide/client_events#trigger-events
                                outgoing = incoming;
                            }
                        }
						else {
							Logger.warn("Unrecognized event [%s]", incoming.getEvent());

						}
						
						if (outgoing.getEvent() != null) {
							PushPlayPlugin.publishMessage(outgoing);
						}
					}
					 
					for (Message message : ClassOf(Message.class).match(e._2)) {
						// if the socket created the message, don't push it out
						if (message.getSocket_id() != null && message.getSocket_id().equals(socket_id)) continue;

						// only send messages if we are subscribed to it.
						if (message.getChannel() != null && subscriptions.contains(message.getChannel())) {
							//outbound.send(message.toString());
							Logger.info("Publishing message to subscribers %s", new Gson().toJson(message));
							outbound.sendJson(message);
						}
					}
				} catch (Throwable t) {
					Logger.error("Caught error %s", t.getMessage());
				}
			}
            // TODO Loop through subscriptions and notify presence channels that user left
		}

        private static boolean isAuthenticated(String channel) {
            return channel.startsWith("private-") || channel.startsWith("presence-");
        }
    }
}

package controllers.pushplay;

import static play.libs.F.Matcher.ClassOf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.F;
import play.libs.F.Either;
import play.libs.F.Promise;
import play.modules.pushplay.*;
import play.mvc.Controller;
import play.mvc.Http;

import static play.libs.F.Matcher.String;
import static play.mvc.Http.WebSocketEvent.*;
import play.mvc.WebSocketController;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class PushPlayWebSocket extends Controller {

    // <k=channel, v=<k = user_id, v = user_info>
    //private static Map<String, List<String>> presence = PushPlayPlugin.hazel.getMap("presence");
    // This is for http://pusher.com/docs/rest_api
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
		// need to convert message, which is json key/value pairs to map, this map needs to get passed directly to
        // the pusher client via data key. Ugghh.
		Map<String, String> map = new Gson().fromJson(message, new TypeToken<Map<String, String>>() {}.getType());
        map.put("socket_id", tem.socket_id);
        // TODO fix this
		//PushPlayPlugin.publishMessage(new Message(channel, tem.name, map, tem.socket_id));
		
		response.status=Http.StatusCode.ACCEPTED;
		renderText("");
	}
	
	public static class StreamSocket extends WebSocketController {

        // <k=channel, v=<k = user_id, v = user_info>
        private static Map<String, List<String>> presence = PushPlayPlugin.hazel.getMap("presence");

        /**
		* Subscribe
		*/
		public static void app(String apiKey) {
			
            Logger.info("Got connection api key=[%s]", apiKey);
			Map<String, String> subscriptions = new HashMap<String, String>();
			Message outgoing = new Message();
			final String socket_id = UUID.randomUUID().toString();
			
			if (outbound.isOpen()) {
                PushPlayPlugin.streams.put(socket_id, new F.EventStream<String>());
				Logger.info("Connection established, socket id=[%s]", socket_id);
				outgoing.setEvent("pusher:connection_established");
				outgoing.setData(new Payload(socket_id));
				
				outbound.sendJson(outgoing);
			}
			else {
				Logger.error("ws outbound not open %s");
			}
			
			while (inbound.isOpen()) {
				
				try {
                    Either<Http.WebSocketEvent, String> e = await(Promise.waitEither(inbound.nextEvent(), PushPlayPlugin.streams.get(socket_id).nextEvent()));

                    for(Http.WebSocketClose closed: SocketClosed.match(e._1)) {
                        break;
                    }

                    for (String txt : Http.WebSocketEvent.TextFrame.match(e._1)) {
						Logger.info("socket_id - %s Incoming Message - %s", socket_id, txt);
						Message incoming = new Gson().fromJson(txt, Message.class);
						
						outgoing.clear();
						String channel = incoming.getChannel();
						if (channel == null) {
							channel = incoming.getData().channel;
						}
						
						if (channel == null) {
							Logger.error("No channel provided in message %s", txt);
							continue;
						}
						outgoing.setChannel(channel);
						
						if (incoming.getEvent().equals("pusher:subscribe")) {
							Logger.info("Subscribing to %s", channel);
							String auth = incoming.getData().auth;
							if (auth != null && !PushPlayUtil.authToken(socket_id, channel, incoming.getData().channel_data.toJSON())
									.equals(auth.split(":")[1])) {
								// private/presence channel failure
								outgoing.setEvent("pusher:error");
							} else {
                                outgoing.setEvent("pusher_internal:subscription_succeeded");
								subscriptions.put(channel, null);
                                Logger.info("Succeeded %s", channel);
                                if (channel.startsWith("presence-")) {
                                    List<String> members;
                                    if (!presence.containsKey(channel)) {
                                        presence.put(channel, new ArrayList<String>());
                                    }
                                    ChannelData channelData = incoming.getData().channel_data;
                                    // TODO use hazelcast distributed lock here, or eventually a db
                                    members = presence.get(channel);

                                    // TODO
                                    // check if members already contains this user before sending member_add
                                    // members.put(incoming.getData().get("channel_data").getUserId(), incoming.getData().get("channel_data").getUserInfo());
                                    outgoing.setData(new Payload());
                                    outgoing.getData().updatePresence(members);
                                    // TODO if this is presence channel, need to send a member add message - http://pusher.com/docs/client_api_guide/client_presence_events
                                    if (!members.contains(channelData.toJSON())) {

                                        Logger.info("new member %s", channelData.toJSON() );
                                        Message memberAdded = new Message();
                                        memberAdded.setChannel(channel);
                                        memberAdded.setEvent("pusher_internal:member_added");
                                        Payload p = new Payload();
                                        p.user_id = channelData.user_id;
                                        p.user_info = channelData.user_info;
                                        memberAdded.setData(p);
                                        PushPlayPlugin.publishMessage(memberAdded);
                                    }
                                    subscriptions.put(channel, channelData.toJSON());
                                    members.add(channelData.toJSON());
                                    outgoing.setData(new Payload());
                                    outgoing.getData().updatePresence(members);

                                    presence.put(channel, members);
                                }
                                Logger.info("socket_id - %s Publishing message to subscribers %s", socket_id, new Gson().toJson(outgoing));

                                outbound.sendJson(outgoing);
                                outgoing.setEvent(null);
							}
						}
						else if (incoming.getEvent().equals("pusher:unsubscribe")) {
							// TODO if this is presence channel, need to send a member remove message - http://pusher.com/docs/client_api_guide/client_presence_events
							// careful sending a member_removed message if this channel has multiple connections...
                            subscriptions.remove(channel);
                            /* if (channel.startsWith("presence-")) {
                                List<String> members;
                                if (!presence.containsKey(channel)) {
                                    Logger.error("Presence channel record not found for [%s]", channel);
                                }
                                String channelData = subscriptions.get(channel);
                                // TODO use hazelcast distributed lock here, or eventually a db
                                members = presence.get(channel); 
                                members.remove(channelData);
                                presence.put(channel, members);
                                if (!members.contains(channelData)) {
                                    Logger.info("removing member %s", channelData );
                                    Message memberRemoved = new Message();
                                    memberRemoved.setChannel(channel);
                                    memberRemoved.setEvent("pusher_internal:member_removed");
                                    Payload p = new Payload();
                                    p.user_id = new Gson().fromJson(channelData, ChannelData.class).user_id;
                                    memberRemoved.setData(p);

                                    PushPlayPlugin.publishMessage(memberRemoved);
                                }
                            }*/
						}
                        else if (incoming.getEvent().startsWith("client-")) {
                            // dear lord...
                            JsonObject msg = new JsonParser().parse(txt).getAsJsonObject();
                            // can only trigger events if this user is subscribed to the channel
                            // and channel must be presence or private
                            if (subscriptions.keySet().contains(channel) && isAuthenticated(channel)) {
                                // TODO handle client- events - http://pusher.com/docs/client_api_guide/client_events#trigger-events
                                //outgoing = incoming;
                                PushPlayPlugin.publishMessage(txt);
                            }
                        }
						else {
							Logger.warn("Unrecognized event [%s]", incoming.getEvent());

						}
						
						if (outgoing.getEvent() != null) {
							PushPlayPlugin.publishMessage(outgoing);
						}
					}

                    for (String message : ClassOf(String.class).match(e._2)) {
                        Logger.info("socket_id - %s handling String %s", socket_id, message);
                        // we only need the channel and event, and it will always serialize as a message, but we send the string
                        Message m =  new Gson().fromJson(message, Message.class);
                        if (!isClientTriggered(m) && socket_id.equals(m.getData().socket_id)) {
                            Logger.info("Filtering out message " + message);
                            continue;
                        }
                        // null it out so it doesn't get sent.
                        //message.getData().socket_id = null;
                        // only send messages if we are subscribed to it.
                        if (subscriptions.keySet().contains(m.getChannel())) {
                            //outbound.send(message.toString());
                            Logger.info("socket_id - %s Publishing message to subscribers %s", socket_id, message);
                            outbound.send(message);
                        } else {
                            Logger.info("socket_id - %s isn't subscribed to this channel - %s", socket_id, m.getChannel());
                        }
                    }
					for (Message message : ClassOf(Message.class).match(e._2)) {
                        Logger.info("BAAAAAD socket_id - %s handling message", socket_id);
						// if the socket created the message, don't push it out, unless its a client trigger
                        // Do we really want this? Only other way to trigger events is from REST API, which indicates it should be broadcast to
                        // all sockets...
						if (!isClientTriggered(message) && socket_id.equals(message.getData().socket_id)) {
                            Logger.info("Filtering out message " + new Gson().toJson(message));
                            continue;
                        }
                        // null it out so it doesn't get sent.
                        message.getData().socket_id = null;
						// only send messages if we are subscribed to it.
						if (subscriptions.keySet().contains(message.getChannel())) {
							//outbound.send(message.toString());
							Logger.info("socket_id - %s Publishing message to subscribers %s", socket_id, new Gson().toJson(message));
							outbound.sendJson(message);
						} else {
                            Logger.info("socket_id - %s isn't subscribed to this channel - %s", socket_id, message.getChannel());
                        }
					}
				} catch (Throwable t) {
					Logger.error("Caught error %s %s", t.getMessage(), t.toString());
				}
			}
            Logger.debug("Reaping EventStream for socket_id=[%s]", socket_id);
            PushPlayPlugin.streams.remove(socket_id);

            // TODO Loop through subscriptions and notify presence channels that user left

		}

        private static boolean isClientTriggered(Message message) {
            return message.getEvent().startsWith("client-");
        }

        private static boolean isAuthenticated(String channel) {
            return channel.startsWith("private-") || channel.startsWith("presence-");
        }
    }
}

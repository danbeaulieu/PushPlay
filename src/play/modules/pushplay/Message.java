package play.modules.pushplay;

import java.util.Map;

import com.google.gson.JsonObject;

public class Message {
	
	String channel;
		
	String event;
	
	Map<String, String> data;

	String socket_id;
	
	public Message() { }
	
	public Message(String channel, String event, Map<String, String> data, String socket_id) {
		
		this.channel = channel;
		this.event = event;
		this.data = data;
		this.socket_id = socket_id;
	}

	public String getChannel() {
		
		return channel;
	}

	public void setChannel(String channel) {
		
		this.channel = channel;
	}

	public String getEvent() {
		
		return event;
	}

	public void setEvent(String event) {
		
		this.event = event;
	}

	public Map<String, String> getData() {
		
		return data;
	}

	public void setData(Map<String, String> data) {
		
		this.data = data;
	}

	public String getSocket_id() {
		
		return socket_id;
	}

	public void setSocket_id(String socket_id) {
		
		this.socket_id = socket_id;
	}

	public void clear() {
		
		this.channel = null;
		this.event = null;
		this.data = null;	
	}

}


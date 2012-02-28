package play.modules.pushplay;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class Message {
	
	String channel;
		
	String event;
	// This needs to be something other than Map. Maybe JsonObject?
	//JsonObject data;
    //@SerializedName("data")
    Payload data;
	
	public Message() { }
	
	public Message(String channel, String event, Payload data) {
		
		this.channel = channel;
		this.event = event;
		this.data = data;
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

	public Payload getData() {
		
		return data;
	}

	public void setData(Payload data) {
		
		this.data = data;

	}

	public void clear() {
		
		this.channel = null;
		this.event = null;
		this.data = null;	
	}
    
    public String toJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("event", this.getEvent());
        json.addProperty("channel", this.getChannel());
        //json.add("data", this.getData());

        return json.toString();
    }

}


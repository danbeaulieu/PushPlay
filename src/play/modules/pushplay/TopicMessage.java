package play.modules.pushplay;

public class TopicMessage {

	public String playServerId;
	
	public Message message;
	
	public TopicMessage(String id, Message msg) {
		
		this.playServerId = id;
		this.message = msg;
	}
	
}

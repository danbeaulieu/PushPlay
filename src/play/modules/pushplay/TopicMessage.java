package play.modules.pushplay;

public class TopicMessage {

	public String playServerId;
	
	public String message;
	
	public TopicMessage(String id, String msg) {
		
		this.playServerId = id;
		this.message = msg;
	}
	
}

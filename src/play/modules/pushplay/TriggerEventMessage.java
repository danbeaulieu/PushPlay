package play.modules.pushplay;

import play.mvc.Scope.Params;

public class TriggerEventMessage {

	public String auth_key;
	
	public String auth_timestamp;
	
	public String auth_version;
	
	public String body_md5;
	
	public String name;
	
	public String socket_id;
	
	public String auth_signature;
	
	public TriggerEventMessage(Params params) {
		
		this.auth_key = params.get("auth_key");
		this.auth_timestamp = params.get("auth_timestamp");
		this.auth_version = params.get("auth_version");
		this.body_md5 = params.get("body_md5");
		this.name = params.get("name");
		this.socket_id = params.get("socket_id");
		this.auth_signature = params.get("auth_signature");
	}
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("auth_key=" + this.auth_key);
		sb.append("&auth_timestamp=" + this.auth_timestamp);
		sb.append("&auth_version=" + this.auth_version);
	    sb.append("&body_md5=" + this.body_md5);
	    sb.append("&name=" + this.name);
	    sb.append((this.socket_id != null ? "&socket_id=" + socket_id : ""));
	    
	    return sb.toString();
	}
}

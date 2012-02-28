package controllers;

import play.*;
import play.modules.pushplay.ChannelData;
import play.modules.pushplay.PushPlayPlugin;
import play.modules.pushplay.PushPlayUtil;
import play.mvc.*;
import play.server.Server;

import java.util.*;

public class Application extends Controller {

    public static void index() {
        renderArgs.put("playPort", Server.httpPort);
        render();
    }

    public static void presence() {
        renderArgs.put("playPort", Server.httpPort);
        render();
    }
    
    public static void auth() {
        // TODO need to add in channel_data too for presence channels
        ChannelData cd = new ChannelData();
        if (params.get("channel_name").startsWith("presence-")) {
            cd.user_id = UUID.randomUUID().toString();
            cd.user_info = new HashMap<String, String>(){{ put("name", UUID.randomUUID().toString().substring(8));}};
        }
    	String token = PushPlayUtil.authToken(params.get("socket_id"), params.get("channel_name"), cd.toJSON());
    	renderText("{\"auth\":\"" + Play.configuration.getProperty("pusher.key") + ":" + token + "\",\"channel_data\":" + cd.toJSON() + "}");
    }

}
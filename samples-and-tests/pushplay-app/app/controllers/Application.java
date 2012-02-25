package controllers;

import play.*;
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
    
    public static void auth() {
    	String token = PushPlayUtil.authToken(params.get("socket_id"), params.get("channel_name"), null);
    	renderJSON("{\"auth\":\"" + Play.configuration.getProperty("pusher.key") + ":" + token + "\"}");
    }

}
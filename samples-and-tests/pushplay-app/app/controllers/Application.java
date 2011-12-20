package controllers;

import play.*;
import play.modules.pushplay.PushPlayPlugin;
import play.modules.pushplay.PushPlayUtil;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }
    
    public static void auth() {
    	String token = PushPlayUtil.authToken(params.get("socket_id"), params.get("channel_name"), null);
    	renderJSON("{\"auth\":\"" + Play.configuration.getProperty("pusher.key") + ":" + token + "\"}");
    }

}
package play.modules.pushplay;

import com.google.gson.Gson;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dbeaulieu
 * Date: 2/27/12
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class Payload {
    
    Presence presence;
    
    public String socket_id;
    
    public String channel;
    
    public String auth;

    public ChannelData channel_data;
    
    public String user_id;

    public Map<String, String> user_info;

    public Payload(String socket_id) {

        this.socket_id = socket_id;
    }

    public Payload() {

    }

    public void updatePresence(List<String> members) {
        
        Presence p = new Presence();

        Set<String> unique = new HashSet<String>();
        unique.addAll(members);
        p.count = unique.size();
        
        for (String s : unique) {
            ChannelData cd = new Gson().fromJson(s, ChannelData.class);
            p.hash.put(cd.user_id, cd.user_info);
        }
        
        this.presence = p;
    }

    private class Presence {

        int count = -1;

        Map<String, Map<String, String>> hash = new HashMap<String, Map<String, String>>();

    }
}

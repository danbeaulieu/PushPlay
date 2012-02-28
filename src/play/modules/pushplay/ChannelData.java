package play.modules.pushplay;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: dbeaulieu
 * Date: 2/27/12
 * Time: 2:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChannelData {
    
    public String user_id;
    
    public Map<String, String> user_info;
    
    public String toJSON() {
        
        return new Gson().toJson(this);
    }
}

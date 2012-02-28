import com.google.gson.*;
import org.junit.*;

import java.io.IOException;
import java.util.*;

import play.modules.pushplay.Message;
import play.test.*;

public class BasicTest extends UnitTest {

    @Test
    public void aVeryImportantThingToTest() {
        assertEquals(2, 1 + 1);
    }
    
    @Test
    public void testJSONObject() {
        MockMessage m = new MockMessage();
        m.socket_id = "12345";
        m.channel = "presence-chat";
        JsonObject data = new JsonObject();
        JsonObject user_id = new JsonObject();
        user_id.addProperty("user_id", "danb");
        data.add("channel_data", user_id);
        
        Map<String, String> foo = new HashMap<String, String>();
        foo.put("1234", "dan");
        System.out.println(new Gson().toJson(foo));
        System.out.println(new Gson().toJson(m));
        
    }
    
    class MockMessage {
        
        public String socket_id;
        
        public JsonObject data;
        
        public String channel;
    }

}

import org.json.*;

public class test {
    public static void main(String[] args) {
        JSONObject obj = new JSONObject();
        JSONObject payload = new JSONObject();
        obj.put("action", "login");
        payload.put("username", "long");
        obj.put("payload", payload);
        obj.getJSONObject("payload").put("aa", "aa");

        String jsonText = obj.toString();
        System.out.println(jsonText);

        JSONObject huhu = new JSONObject(jsonText);

    }
}
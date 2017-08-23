import com.sun.net.httpserver.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class JiRestProxyServer {

    public static final int PORT = 8765;

    public static String sendRequest(String method, String url, String body, ArrayList<String> headers) {
        String reqheaders = "";
        String respheaders = "";
        
        JSONObject obj = new JSONObject();
        obj.put("method", method);
        obj.put("url", url);
        obj.put("details", new JSONArray());
        obj.put("respheaders", "");
        obj.put("respbody", "");
        obj.put("bodySize", "0");         

        try {

            URL myURL = new URL(url);
            HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();
            myURLConnection.setRequestMethod(method);

            reqheaders += "Host: " + myURL.getHost() + "\r\n";
            for (String header : headers) {
                String[] cells = header.split(":");
                myURLConnection.setRequestProperty(cells[0].trim(), cells[1].trim());
                reqheaders += header + "\r\n";
            }
            obj.put("reqheaders", reqheaders.trim());
            obj.put("reqbody", body);
            obj.put("protocol", "HTTP/1.1");

	    // Send post request
            if (body != null) {
                if (body.length()>0) {
                    myURLConnection.setDoOutput(true);
	            DataOutputStream writer = new DataOutputStream(myURLConnection.getOutputStream());
	            writer.writeBytes(body);
	            writer.flush();
	            writer.close();
                }
            }

            int responseCode = myURLConnection.getResponseCode();

            Map<String, List<String>> map = myURLConnection.getHeaderFields();

            boolean contentLength = false;
	    for (Map.Entry<String, List<String>> entry : map.entrySet()) {              
                if (entry.getKey()!=null) {
                    for(String value : entry.getValue())
                        respheaders += entry.getKey() + ": " + value + "\r\n";
                }
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (!map.containsKey("Content-Length"))
                respheaders += "Content-Length: " + response.length() + "\r\n";	

            obj.put("respheaders", respheaders.trim());

            obj.put("respbody", response.toString());
            obj.put("bodySize", response.length());         
            obj.put("success", true);
            obj.put("message", "");            

            return obj.toJSONString();
        }
        catch(Exception ex) {
            obj.put("success", false);
            obj.put("message", ex.getMessage());
            return obj.toJSONString();
        }
    }


    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(PORT), 0);

        HttpContext context = server.createContext("/api/proxy", new JiRestProxyHandler());

        server.setExecutor(null); //it needs to be set on the http proxy. not on the request that is being relayed.
        server.start();
    }

    static class JiRestProxyHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            try {

                if (!exchange.getRequestMethod().equals("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                InputStreamReader isr =  new InputStreamReader(exchange.getRequestBody(), "utf-8");
                BufferedReader reader = new BufferedReader(isr);
                StringBuilder jsonBuilder = new StringBuilder();
                String line = null;
                while((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();

                JSONParser parser = new JSONParser();
                JSONObject obj = (JSONObject)parser.parse(jsonBuilder.toString());
                String method = (String)obj.get("method");
                String body = (String)obj.get("body");
                String url = (String)obj.get("url");

                JSONArray headers = (JSONArray) obj.get("headers");
                ArrayList<String> headersArray = new ArrayList<String>();
                Iterator<String> headersIterator = headersArray.iterator();
                while (headersIterator.hasNext()) {
                    headers.add(headersIterator.next());
                    
                }

                String responseJson = sendRequest(method, url, body, headers);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
 		exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
 		//exchange.getResponseHeaders().set("Access-Control-Allow-Headers", exchange.getResponseHeaders().get("access-control-request-headers").get(0));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseJson.length());

             
                OutputStream output = exchange.getResponseBody();
                PrintWriter printer = new PrintWriter(output);
                printer.print(responseJson);
                printer.flush();
                printer.close();
                output.flush();
                output.close();
            }
            catch(Exception ex) {
                exchange.sendResponseHeaders(400, ex.getMessage().length());
                OutputStream output = exchange.getResponseBody();
                PrintWriter printer = new PrintWriter(output);
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                printer.print(ex.getMessage());
                printer.flush();
                printer.close();
                output.flush();
                output.close();
            }            
        }
    }
}
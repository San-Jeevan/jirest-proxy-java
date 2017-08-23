import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleTest {
    public static void main(String[] args) throws Exception {

        URL myURL = new URL("http://localhost:8765/api/proxy");
        HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();
        myURLConnection.setRequestMethod("POST");

        String body = readFile("test1.json");
        myURLConnection.setDoOutput(true);
        DataOutputStream writer = new DataOutputStream(myURLConnection.getOutputStream());
        writer.writeBytes(body);
        writer.flush();
        writer.close();

        int responseCode = myURLConnection.getResponseCode();
	System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

	//print result
	System.out.println(response.toString());
    }

    private static String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader (file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        try {
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }

            return stringBuilder.toString();
        } finally {
            reader.close();
        }
    }
}
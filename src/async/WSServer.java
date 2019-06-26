
package async;

import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;


public class WSServer {


    public static Properties load() {
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream("server-config.properties");
            prop.load(input);
            return prop;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) throws Exception {


        Properties prop = load();

        if (prop == null) {
            System.out.println("Unable to load config file. Run with 'server-config' argument.");
            System.exit(1);
        }

        String ip = prop.getProperty("host");
        int port = Integer.parseInt(prop.getProperty("port"));

        Server server = new Server(ip, port, "/", null, MyApplicationConfig.class);

        try {
            server.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            //System.out.print("Please press a key to stop the server.");
            //reader.readLine();
            while (true) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

}
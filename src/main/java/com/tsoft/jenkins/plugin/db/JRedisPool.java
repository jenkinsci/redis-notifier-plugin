package com.tsoft.jenkins.plugin.db;

import com.tsoft.jenkins.plugin.rejson.JReJSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JRedisPool {

    static JReJSON client = null;
    private static final Logger log = Logger.getLogger(JRedisPool.class.getName());
    private Pattern p = Pattern.compile("^(https?\\:\\/\\/)?(\\w+\\:\\d{2,5})$");

    /**
     * @return the singleton instance of Jedis client
     */
    public static JReJSON getPool() {
        if (client == null) {
            new JRedisPool();
            log.info("Nuevo cliente de redis creado: " + client.hashCode());
        }
        return client;
    }

    private JRedisPool() {
        createRedisConection();
    }

    private void createRedisConection() {
        try {
            String serverUrl = RedisDatabasePoolConfiguration.get().getRedisServerUrl();
            if (client == null && serverUrl != null) {
                Matcher m = p.matcher(serverUrl);
                if( m.matches() ) {
                    log.config("redis data for new conection: " + serverUrl);
                    String url = m.group(2);
                    String server = url.split(":")[0];
                    int port = Integer.parseInt(url.split(":")[1]);
                    client = new JReJSON(server, port);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}

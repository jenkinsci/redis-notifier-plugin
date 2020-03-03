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

    private static JReJSON client;
    private static final Logger log = Logger.getLogger(JRedisPool.class.getName());

    /**
     * @return the singleton instance of Jedis client
     */
    public static JReJSON getPool() {
        if (client == null) {
            client = JRedisPool();
            log.info("Nuevo cliente de redis creado: " + client.hashCode());
        }
        return client;
    }

    private static JReJSON JRedisPool() {
        try {
            String serverUrl = RedisDatabasePoolConfiguration.get().getRedisServerUrl();
            if (client == null && serverUrl != null) {
                Pattern p = Pattern.compile("^(https?\\:\\/\\/)?(\\w+\\:\\d{2,5})$");
                Matcher m = p.matcher(serverUrl);
                if( m.matches() ) {
                    log.config("redis data for new conection: " + serverUrl);
                    String url = m.group(2);
                    String server = url.split(":")[0];
                    int port = Integer.parseInt(url.split(":")[1]);
                    return new JReJSON(server, port);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}

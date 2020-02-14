package com.tsoft.jenkins.plugin

import com.tsoft.jenkins.plugin.db.JRedisPool;
import com.tsoft.jenkins.plugin.db.RedisDatabasePoolConfiguration
import com.tsoft.jenkins.plugin.rejson.JReJSON
import com.tsoft.jenkins.plugin.rejson.Path
import redis.clients.jedis.Jedis;

import java.util.logging.Logger

class RedisClient implements Serializable {

    static final Logger log = Logger.getLogger(RedisClient.class.getName());
    private transient RedisDatabasePoolConfiguration config = RedisDatabasePoolConfiguration.get();
    private static JReJSON client2;

    private org.jenkinsci.plugins.workflow.cps.CpsScript script;

    RedisClient(org.jenkinsci.plugins.workflow.cps.CpsScript script) {
        this.script = script
        config.load()
        client2 = JRedisPool.getPool()
    }

    private <V> V node(Closure<V> body) {
        if (script.env.NODE_NAME != null) {
            // Already inside a node block.
            body()
        } else {
            script.node {
                body()
            }
        }
    }

    private String shell() {
        node {
            script.isUnix() ? "sh" : "bat"
        }
    }

    /**
     * Método de almacenamiento de valores: @set(key, value)
     * @param key
     * @param val
     * @return
     */
    private void redisSet(String key, Object val, Path path){
        try {
            def client = JRedisPool.getPool()
            client.set(key, val, path)
        }
        catch(NullPointerException ne){
            script.error("[REDIS:ERROR] No existe una conexion activa hacia el servidor de redis.")
        }
    }

    // metodo set con un string path
    def set(String key, Object val, Object path){
        if( path instanceof String ) {
            this.redisSet(key, val, new Path(path))
        }
        else if(path instanceof Path){
            this.redisSet(key, val, path)
        }
    }

    // metodo set sin un path asignado
    def set(String key, Object val){
        this.redisSet(key, val, Path.ROOT_PATH)
    }

    /**
     * Método de eliminacion de valores por Path: @del(key, path)
     * @param key
     * @param path
     * @return
     */
    private void redisDel(String key, Path path){
        try {
            client2.del(key, path)
        }
        catch(NullPointerException ne){
            script.echo("[REDIS:ERROR] No existe una conexion activa hacia el servidor de redis.")
        }
    }

    def del(String key, Object path){
        if( path instanceof String ) {
            this.redisDel(key, new Path(path))
        }
        else if(path instanceof Path){
            this.redisDel(key, path)
        }
    }

    /**
     * Método para obtener un set de valores indicando un Path: @redisGet(key, path)
     * @param key
     * @param path
     * @return
     */
    private Object redisGet(String key, Path path){
        def return_value = null
        try {
            return_value = client2.get(key, path)
        }
        catch(NullPointerException ne){
            script.echo("[REDIS:ERROR] No existe una conexion activa hacia el servidor de redis.")
        }
        return return_value
    }

    def get(String key){
        return this.redisGet(key, Path.ROOT_PATH)
    }
    def get(String key, String path){
        return this.redisGet(key, new Path(path))
    }
    def get(String key, Path path){
        return this.redisGet(key, path)
    }

    /**
     * Método para obtener un set de valores indicando un Path: @redisGet(key, path)
     * @param key
     * @param path
     * @return
     */
    private Class<?> redisType(String key, Path path){
        try {
            client2.type(key, path)
        }
        catch(NullPointerException ne){
            script.echo("[REDIS:ERROR] No existe una conexion activa hacia el servidor de redis.")
        }
    }
    // Usando solo la Key
    def type(String key){
        return this.redisType(key, Path.ROOT_PATH)
    }
    // Usando Key y Path
    def type(String key, Path path){
        return this.redisType(key, path)
    }

    /**
     * get all keys
     */
    def keys(){
        return client2.keys()
    }
}
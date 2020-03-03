package com.tsoft.jenkins.plugin.rejson;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.util.SafeEncoder;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * JReJSON is the main ReJSON client class, wrapping connection management and all ReJSON commands
 */
public class JReJSON {

    private static final Gson gson = new Gson();

    private enum Command implements ProtocolCommand {
        DEL("JSON.DEL"),
        GET("JSON.GET"),
        SET("JSON.SET"),
        TYPE("JSON.TYPE"),
        KEYS("KEYS");
        private final byte[] raw;

        Command(String alt) {
            raw = SafeEncoder.encode(alt);
        }

        public byte[] getRaw() {
            return raw;
        }
    }

    public enum ExistenceModifier implements ProtocolCommand {
        DEFAULT(""),
        NOT_EXISTS("NX"),
        MUST_EXIST("XX");
        private final String raw;

        ExistenceModifier(String alt) {
            this.raw = alt;
        }

        public byte[] getRaw() {
            return raw.getBytes(Charset.forName("UTF-8"));
        }
    }

    private Pool<Jedis> client;

    /**
     * Creates a client to the local machine
     */
    public JReJSON() {
        this("localhost", 6379);
    }

    /**
     * Creates a client to the specific host/post
     *
     * @param host Redis host
     * @param port Redis port
     */
    public JReJSON(String host, int port) {
        this(new JedisPool(host, port));
    }

    /**
     * Creates a client using provided Jedis pool
     *
     * @param jedis bring your own Jedis pool
     */
    public JReJSON(Pool<Jedis> jedis) {
        this.client = jedis;
    }

    /**
     *  Helper to check for errors and throw them as an exception
     * @param str the reply string to "analyze"
     * @throws RuntimeException
     */
    private static void assertReplyNotError(final String str) {
        if (str.startsWith("-ERR"))
            throw new RuntimeException(str.substring(5));
    }

    /**
     * Helper to check for an OK reply
     * @param str the reply string to "scrutinize"
     */
    private static void assertReplyOK(final String str) {
        if (!str.equals("OK"))
            throw new RuntimeException(str);
    }

    /**
     * Helper to handle single optional path argument situations
     * @param path a single optional path
     * @return the provided path or root if not
     */
    private static Path getSingleOptionalPath(Path... path) {
        // check for 0, 1 or more paths
        if (1 > path.length) {   // default to root
            return Path.ROOT_PATH;
        }
        if (1 == path.length) {  // take 1
            return path[0];
        }

        // throw out the baby with the water
        throw new RuntimeException("Only a single optional path is allowed");
    }

    /**
     * Deletes the root path
     * @param key the key name
     * @return the number of paths deleted (0 or 1)
     */
    public Long del(String key) {
        return del(key, Path.ROOT_PATH);
    }


    /**
     * Deletes a path
     * @param key the key name
     * @param path optional single path in the object, defaults to root
     * @return path deleted
     */
    public Long del(String key, Path path) {
        byte[][] args = new byte[2][];
        args[0] = SafeEncoder.encode(key);
        args[1] = SafeEncoder.encode(path.toString());

        try (Jedis conn = getConnection()) {
            conn.getClient().sendCommand(Command.DEL, args);
            return conn.getClient().getIntegerReply();
        }
    }

    /**
     * Gets an object at the root path
     * @param key the key name
     * @return the requested object
     */
    public <T> T get(String key) {
        return get(key, Path.ROOT_PATH);
    }

    /**
     * Gets an object
     * @param key the key name
     * @param paths optional one ore more paths in the object
     * @return the requested object
     */
    public <T> T get(String key, Path... paths) {
        byte[][] args = new byte[1 + paths.length][];
        int i=0;
        args[i] = SafeEncoder.encode(key);
        for (Path p :paths) {
            args[++i] = SafeEncoder.encode(p.toString());
        }

        String rep;
        try (Jedis conn = getConnection()) {
            conn.getClient().sendCommand(Command.GET, args);
            rep = conn.getClient().getBulkReply();
        }
        assertReplyNotError(rep);
        return (T)gson.fromJson(rep, Object.class);
    }

    /**
     * Sets an object at the root path
     * @param key the key name
     * @param object the Java object to store
     * @param flag an existential modifier
     */
    public void set(String key, Object object, ExistenceModifier flag) {
        set(key, object, flag, Path.ROOT_PATH);
    }

    /**
     * Sets an object in the root path
     * @param key the key name
     * @param object the Java object to store
     */
    public void set(String key, Object object) {
        set(key, object, ExistenceModifier.DEFAULT, Path.ROOT_PATH);
    }

    /**
     * Sets an object without caring about target path existing
     * @param key the key name
     * @param object the Java object to store
     * @param path in the object
     */
    public void set(String key, Object object, Path path) {
        set(key, object, ExistenceModifier.DEFAULT, path);
    }

    /**
     * Sets an object
     * @param key the key name
     * @param object the Java object to store
     * @param flag an existential modifier
     * @param path in the object
     */
    public void set(String key, Object object, ExistenceModifier flag, Path path) {

        List<byte[]> args = new ArrayList<>(4);

        args.add(SafeEncoder.encode(key));
        args.add(SafeEncoder.encode(path.toString()));
        args.add(SafeEncoder.encode(gson.toJson(object)));
        if (ExistenceModifier.DEFAULT != flag) {
            args.add(flag.getRaw());
        }

        String status;
        try (Jedis conn = getConnection()) {
            conn.getClient()
                    .sendCommand(Command.SET, args.toArray(new byte[args.size()][]));
            status = conn.getClient().getStatusCodeReply();
        }
        assertReplyOK(status);
    }

    /**
     * Gets the class of an object at the root path
     * @param key the key name
     * @return the Java class of the requested object
     */
    public Class<?> type(String key) {
        return type(key, new Path("."));
    }

    /**
     * Gets the class of an object
     * @param key the key name
     * @param path a path in the object
     * @return the Java class of the requested object
     */
    public Class<?> type(String key, Path path) {

        List<byte[]> args = new ArrayList<>(2);

        args.add(SafeEncoder.encode(key));
        args.add(SafeEncoder.encode(path.toString()));

        String rep;
        try (Jedis conn = getConnection()) {
            conn.getClient()
                    .sendCommand(Command.TYPE, args.toArray(new byte[args.size()][]));
            rep = conn.getClient().getBulkReply();
        }

        assertReplyNotError(rep);

        switch (rep) {
            case "null":
                return null;
            case "boolean":
                return boolean.class;
            case "integer":
                return int.class;
            case "number":
                return float.class;
            case "string":
                return String.class;
            case "object":
                return Object.class;
            case "array":
                return List.class;
            default:
                throw new java.lang.RuntimeException(rep);
        }
    }

    /**
     * Get all Keys
     * @return List of String keys
     */
    public List<String> keys() {
        List<String> values = null;
        try (Jedis conn = getConnection()) {
            byte[][] args = new byte[][]{SafeEncoder.encode("*")};
            conn.getClient().sendCommand(Command.KEYS, args);
            values = conn.getClient().getMultiBulkReply();
        }
        catch (Exception ne) {
            ne.printStackTrace();
        }
        return values;
    }

    /**
     * Deletes a path
     * @param conn the Jedis connection
     * @param key the key name
     * @param path optional single path in the object, defaults to root
     * @return the number of paths deleted (0 or 1)
     * @deprecated use {@link #del(String, Path)} instead
     */
    @Deprecated
    public static Long del(Jedis conn, String key, Path... path) {

        List<byte[]> args = new ArrayList<>(2);

        args.add(SafeEncoder.encode(key));
        args.add(SafeEncoder.encode(getSingleOptionalPath(path).toString()));

        conn.getClient()
                .sendCommand(Command.DEL, args.toArray(new byte[args.size()][]));
        Long rep = conn.getClient().getIntegerReply();
        conn.close();

        return rep;
    }

    /**
     * Gets an object
     * @param conn the Jedis connection
     * @param key the key name
     * @param paths optional one ore more paths in the object, defaults to root
     * @return the requested object
     * @deprecated use {@link #get(String, Path...)} instead
     */
    @Deprecated
    public static Object get(Jedis conn, String key, Path... paths) {

        List<byte[]> args = new ArrayList<>(2);

        args.add(SafeEncoder.encode(key));
        for (Path p :paths) {
            args.add(SafeEncoder.encode(p.toString()));
        }

        conn.getClient()
                .sendCommand(Command.GET, args.toArray(new byte[args.size()][]));
        String rep = conn.getClient().getBulkReply();
        conn.close();

        assertReplyNotError(rep);
        return gson.fromJson(rep, Object.class);
    }

    /**
     * Sets an object
     * @param conn the Jedis connection
     * @param key the key name
     * @param object the Java object to store
     * @param flag an existential modifier
     * @param path optional single path in the object, defaults to root
     * @deprecated use {@link #set(String, Object, ExistenceModifier, Path)} instead
     */
    @Deprecated
    public static void set(Jedis conn, String key, Object object, ExistenceModifier flag, Path... path) {

        List<byte[]> args = new ArrayList<>(4);

        args.add(SafeEncoder.encode(key));
        args.add(SafeEncoder.encode(getSingleOptionalPath(path).toString()));
        args.add(SafeEncoder.encode(gson.toJson(object)));
        if (ExistenceModifier.DEFAULT != flag) {
            args.add(flag.getRaw());
        }

        conn.getClient()
                .sendCommand(Command.SET, args.toArray(new byte[args.size()][]));
        String status = conn.getClient().getStatusCodeReply();
        conn.close();

        assertReplyOK(status);
    }

    /**
     * Sets an object without caring about target path existing
     * @param conn the Jedis connection
     * @param key the key name
     * @param object the Java object to store
     * @param path optional single path in the object, defaults to root
     * @deprecated use {@link #set(String, Object, ExistenceModifier, Path)} instead
     */
    @Deprecated
    public static void set(Jedis conn, String key, Object object, Path... path) {
        set(conn,key, object, ExistenceModifier.DEFAULT, path);
    }

    /**
     * Gets the class of an object
     * @param conn the Jedis connection
     * @param key the key name
     * @param path optional single path in the object, defaults to root
     * @return the Java class of the requested object
     * @deprecated use {@link #type(String, Path)} instead
     */
    @Deprecated
    public static Class<?> type(Jedis conn, String key, Path... path) {

        List<byte[]> args = new ArrayList<>(2);

        args.add(SafeEncoder.encode(key));
        args.add(SafeEncoder.encode(getSingleOptionalPath(path).toString()));

        conn.getClient()
                .sendCommand(Command.TYPE, args.toArray(new byte[args.size()][]));
        String rep = conn.getClient().getBulkReply();
        conn.close();

        assertReplyNotError(rep);

        switch (rep) {
            case "null":
                return null;
            case "boolean":
                return boolean.class;
            case "integer":
                return int.class;
            case "number":
                return float.class;
            case "string":
                return String.class;
            case "object":
                return Object.class;
            case "array":
                return List.class;
            default:
                throw new java.lang.RuntimeException(rep);
        }
    }

    public Jedis getConnection() {
        return this.client.getResource();
    }

}
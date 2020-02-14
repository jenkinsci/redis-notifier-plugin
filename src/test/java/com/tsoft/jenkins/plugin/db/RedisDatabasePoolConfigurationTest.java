package com.tsoft.jenkins.plugin.db;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class RedisDatabasePoolConfigurationTest {

    private Pattern p = null;
    @Before
    public void setUp() throws Exception {
        p = Pattern.compile("^(https?\\:\\/\\/)?(\\w+\\:\\d{2,5})$");
    }

    @Test
    public void doTestConnection() {
        String server = "http://localhost:6793";
        Matcher m = p.matcher(server);
        assertTrue(m.matches());
        assertEquals(2, m.groupCount());

        server = "https://localhost:6793";
        m = p.matcher(server);
        assertTrue(m.matches());
        assertEquals(2, m.groupCount());

        server = "localhost:6793";
        m = p.matcher(server);
        assertTrue(m.matches());
        assertEquals(2, m.groupCount());

        System.out.println(m.group(0));
        System.out.println(m.group(1));
        System.out.println(m.group(2));

    }
}
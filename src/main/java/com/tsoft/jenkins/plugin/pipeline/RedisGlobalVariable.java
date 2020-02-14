package com.tsoft.jenkins.plugin.pipeline;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import javax.annotation.Nonnull;

@Extension
public class RedisGlobalVariable extends GlobalVariable {

    @Nonnull
    @Override
    public String getName() {
        return "jredis";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript script) throws Exception {
        Binding binding = script.getBinding();
        Object redisdb;
        if (binding.hasVariable(getName())) {
            redisdb = binding.getVariable(getName());
        } else {
            redisdb = script.getClass().getClassLoader()
                    .loadClass("com.tsoft.jenkins.plugin.RedisClient")
                    .getConstructor(CpsScript.class).newInstance(script);
            binding.setVariable(getName(), redisdb);
        }
        return redisdb;
    }


}

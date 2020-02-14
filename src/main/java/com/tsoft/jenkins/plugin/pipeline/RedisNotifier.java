package com.tsoft.jenkins.plugin.pipeline;


import com.tsoft.jenkins.plugin.db.JRedisPool;
import com.tsoft.jenkins.plugin.rejson.JReJSON;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.IOException;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class RedisNotifier extends Notifier {
  private static final Logger logger = Logger.getLogger(RedisNotifier.class.getName());
  private String message;

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
     JReJSON redis = JRedisPool.getPool();
    redis.set("msg", "OK: "+this.message);
    return true;
  }

  @DataBoundConstructor
  public RedisNotifier(final String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    public String getDisplayName() {
      return "Message to a Redis server";
    }
  }
}

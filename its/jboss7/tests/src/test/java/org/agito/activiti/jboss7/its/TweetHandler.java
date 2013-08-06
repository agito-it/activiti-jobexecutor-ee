package org.agito.activiti.jboss7.its;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.junit.Assert;

public class TweetHandler implements JobHandler {

  List<String> messages = new ArrayList<String>();

  public String getType() {
    return "tweet";
  }
  
  public void execute(JobEntity job, String configuration, ExecutionEntity execution, CommandContext commandContext) {
    messages.add(configuration);
    Assert.assertNotNull(commandContext);
  }
  
  public List<String> getMessages() {
    return messages;
  }
}
package org.agito.activiti.jboss7.its;

import java.util.logging.Logger;

import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;


/**
 * @author Tom Baeyens
 */
public class TweetExceptionHandler implements JobHandler {
  
  private static Logger log = Logger.getLogger(TweetExceptionHandler.class.getName());
  
  protected int exceptionsRemaining = 2;

  public String getType() {
    return "tweet-exception";
  }

  public void execute(JobEntity job, String configuration, ExecutionEntity execution, CommandContext commandContext) {
    if (exceptionsRemaining>0) {
      exceptionsRemaining--;
      throw new RuntimeException("exception remaining: "+exceptionsRemaining);
    }
    log.info("no more exceptions to throw."); 
  }

  
  public int getExceptionsRemaining() {
    return exceptionsRemaining;
  }

  
  public void setExceptionsRemaining(int exceptionsRemaining) {
    this.exceptionsRemaining = exceptionsRemaining;
  }
}

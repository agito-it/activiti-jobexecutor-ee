package org.agito.activiti.jboss7.its;

import java.util.Date;

import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;

public abstract class AbstractTweetTest extends AbstractJobExecutorTestCase<TweetHandler> {

	@Override
	protected TweetHandler initJobHandler() {
		return new TweetHandler();
	}

	protected MessageEntity createTweetMessage(String msg) {
		MessageEntity message = new MessageEntity();
		message.setJobHandlerType("tweet");
		message.setJobHandlerConfiguration(msg);
		return message;
	}

	protected TimerEntity createTweetTimer(String msg, Date duedate) {
		TimerEntity timer = new TimerEntity();
		timer.setJobHandlerType("tweet");
		timer.setJobHandlerConfiguration(msg);
		timer.setDuedate(duedate);
		return timer;
	}

}

package com.paascloud.provider.config;

import com.paascloud.provider.mq.consumer.listener.UacPushMessageListener;
import com.passcloud.common.base.constant.AliyunMqTopicConstants;
import com.passcloud.common.base.constant.GlobalConstant;
import com.passcloud.common.conf.properties.PaascloudProperties;
import com.passcloud.common.util.PublicUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import javax.annotation.Resource;

/**
 * The class Aliyun mq configuration.
 *
 * @author liyuzhang
 */
@Slf4j
@Configuration
public class AliyunMqConfiguration {
	@Resource
	private UacPushMessageListener uacPushMessageListener;

	@Resource
	private PaascloudProperties paascloudProperties;

	@Resource
	private TaskExecutor taskExecutor;

	/**
	 * Default mq push consumer default mq push consumer.
	 *
	 * @return the default mq push consumer
	 *
	 * @throws MQClientException the mq client exception
	 */
	@Bean
	public DefaultMQPushConsumer defaultMQPushConsumer() throws MQClientException {
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(paascloudProperties.getAliyun().getRocketMq().getConsumerGroup());
		consumer.setNamesrvAddr(paascloudProperties.getAliyun().getRocketMq().getNamesrvAddr());
		consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

		String[] strArray = AliyunMqTopicConstants.ConsumerTopics.UAC.split(GlobalConstant.Symbol.COMMA);
		for (String aStrArray : strArray) {
			String[] topicArray = aStrArray.split(GlobalConstant.Symbol.AT);
			String topic = topicArray[0];
			String tags = topicArray[1];
			if (PublicUtil.isEmpty(tags)) {
				tags = "*";
			}
			consumer.subscribe(topic, tags);
			log.info("RocketMq UacPushConsumer topic = {}, tags={}", topic, tags);
		}

		consumer.registerMessageListener(uacPushMessageListener);
		consumer.setConsumeThreadMax(2);
		consumer.setConsumeThreadMin(2);

		taskExecutor.execute(() -> {
			try {
				Thread.sleep(5000);
				consumer.start();
				log.info("RocketMq UacPushConsumer OK.");
			} catch (InterruptedException | MQClientException e) {
				log.error("RocketMq OpcPushConsumer, 出现异常={}", e.getMessage(), e);
			}
		});
		return consumer;
	}

}

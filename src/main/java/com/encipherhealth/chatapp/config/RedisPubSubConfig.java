package com.encipherhealth.chatapp.config;

import com.encipherhealth.chatapp.pubsub.ChatMessageSubscriber;
import com.encipherhealth.chatapp.util.RedisKeyConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RedisPubSubConfig
{
    @Bean(name = "ssePubSubExecutor")
    public ThreadPoolTaskExecutor ssePubSubExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-redis-sub-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ThreadPoolTaskExecutor ssePubSubExecutor,
            ChatMessageSubscriber chatMessageSubscriber)
    {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(ssePubSubExecutor);
        container.addMessageListener(chatMessageSubscriber, new PatternTopic(RedisKeyConstants.LIVE_CHAT_CHANNEL_PATTERN));
        return container;
    }
}

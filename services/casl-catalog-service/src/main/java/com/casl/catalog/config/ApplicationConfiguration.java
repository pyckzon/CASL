package com.casl.catalog.config;

import com.casl.catalog.domain.port.ProductRepository;
import com.casl.catalog.domain.port.ProductSearchStrategy;
import com.casl.catalog.domain.port.StockPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableCaching
public class ApplicationConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	StockPolicy stockPolicy() {
		return new OptimisticStockPolicy();
	}

	@Bean
	CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new JdkSerializationRedisSerializer()));
		return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
	}

	@Bean
	ProductSearchStrategy productSearchStrategy(ProductRepository repository, ObjectProvider<CacheManager> cacheManagerProvider,
			@Value("${casl.catalog.cache.enabled:true}") boolean cacheEnabled) {
		com.casl.catalog.adapter.out.search.JpaProductSearchStrategy base =
				new com.casl.catalog.adapter.out.search.JpaProductSearchStrategy(repository);
		if (!cacheEnabled) {
			return base;
		}
		CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
		if (cacheManager == null) {
			return base;
		}
		return new com.casl.catalog.adapter.out.search.CachedProductSearchStrategy(base, cacheManager);
	}
}

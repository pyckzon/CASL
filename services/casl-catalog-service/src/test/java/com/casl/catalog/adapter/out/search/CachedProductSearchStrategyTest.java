package com.casl.catalog.adapter.out.search;

import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.port.ProductSearchStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedProductSearchStrategyTest {

	@Test
	void cachesResultAndAvoidsDelegateOnHit() {
		ProductSearchStrategy delegate = mock(ProductSearchStrategy.class);
		ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("product-search");
		CachedProductSearchStrategy strategy = new CachedProductSearchStrategy(delegate, cacheManager);

		ProductSearchCriteria criteria = new ProductSearchCriteria("widget", null, null, null, null, 0, 20);
		PageResult<Product> result = new PageResult<>(List.of(), 0, 20, 0);
		when(delegate.search(criteria)).thenReturn(result);

		PageResult<Product> first = strategy.search(criteria);
		PageResult<Product> second = strategy.search(criteria);

		assertThat(first).isEqualTo(result);
		assertThat(second).isEqualTo(result);
		verify(delegate, times(1)).search(criteria);
	}

	@Test
	void delegatesWhenNoCacheAvailable() {
		ProductSearchStrategy delegate = mock(ProductSearchStrategy.class);
		CacheManager cacheManager = mock(CacheManager.class);
		org.mockito.Mockito.when(cacheManager.getCache("product-search")).thenReturn(null);
		CachedProductSearchStrategy strategy = new CachedProductSearchStrategy(delegate, cacheManager);

		ProductSearchCriteria criteria = new ProductSearchCriteria("widget", null, null, null, null, 0, 20);
		PageResult<Product> result = new PageResult<>(List.of(), 0, 20, 0);
		when(delegate.search(criteria)).thenReturn(result);

		strategy.search(criteria);
		strategy.search(criteria);

		verify(delegate, times(2)).search(criteria);
	}
}

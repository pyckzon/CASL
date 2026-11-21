package com.casl.catalog.adapter.out.search;

import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.port.ProductSearchStrategy;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Decorator (Adapter) que cachea los resultados de búsqueda en Redis. Se
 * activa por configuración; desactivarlo no cambia el caso de uso.
 */
public class CachedProductSearchStrategy implements ProductSearchStrategy {

	private final ProductSearchStrategy delegate;
	private final Cache cache;

	public CachedProductSearchStrategy(ProductSearchStrategy delegate, CacheManager cacheManager) {
		this.delegate = delegate;
		this.cache = cacheManager.getCache("product-search");
	}

	@Override
	public PageResult<Product> search(ProductSearchCriteria criteria) {
		if (cache == null) {
			return delegate.search(criteria);
		}
		String key = criteria.toString();
		Cache.ValueWrapper wrapper = cache.get(key);
		if (wrapper != null && wrapper.get() instanceof PageResult) {
			@SuppressWarnings("unchecked")
			PageResult<Product> cached = (PageResult<Product>) wrapper.get();
			return cached;
		}
		PageResult<Product> result = delegate.search(criteria);
		cache.put(key, result);
		return result;
	}
}

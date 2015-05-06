/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.map;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.CollectionFactory;
import org.springframework.data.keyvalue.core.AbstractKeyValueAccessor;
import org.springframework.data.keyvalue.core.KeyValueAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link KeyValueAccessor} implementation for {@link Map}.
 * 
 * @author Christoph Strobl
 */
public class MapKeyValueAccessor extends AbstractKeyValueAccessor {

	@SuppressWarnings("rawtypes")//
	private final Class<? extends Map> keySpaceMapType;
	private final Map<Serializable, Map<Serializable, Object>> store;

	/**
	 * Create new {@link MapKeyValueAccessor} using {@link ConcurrentHashMap} as backing store type.
	 */
	public MapKeyValueAccessor() {
		this(ConcurrentHashMap.class);
	}

	/**
	 * Creates a new {@link MapKeyValueAccessor} using the given {@link Map} as backing store.
	 * 
	 * @param mapType must not be {@literal null}.
	 */
	@SuppressWarnings("rawtypes")
	public MapKeyValueAccessor(Class<? extends Map> mapType) {
		this(CollectionFactory.<Serializable, Map<Serializable, Object>> createMap(mapType, 100), mapType);
	}

	/**
	 * Create new instance of {@link MapKeyValueAccessor} using given dataStore for persistence.
	 * 
	 * @param store must not be {@literal null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MapKeyValueAccessor(Map<Serializable, Map<Serializable, Object>> store) {
		this(store, (Class<? extends Map>) ClassUtils.getUserClass(store));
	}

	/**
	 * Creates a new {@link MapKeyValueAccessor} with the given store and type to be used when creating key spaces.
	 * 
	 * @param store must not be {@literal null}.
	 * @param keySpaceMapType must not be {@literal null}.
	 */
	@SuppressWarnings("rawtypes")
	private MapKeyValueAccessor(Map<Serializable, Map<Serializable, Object>> store, Class<? extends Map> keySpaceMapType) {

		Assert.notNull(store, "Store must not be null.");
		Assert.notNull(keySpaceMapType, "Map type to be used for key spaces must not be null!");

		this.store = store;
		this.keySpaceMapType = keySpaceMapType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#put(java.io.Serializable, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public Object put(Serializable id, Object item, Serializable keyspace) {

		Assert.notNull(id, "Cannot add item with null id.");
		Assert.notNull(keyspace, "Cannot add item for null collection.");

		return getKeySpaceMap(keyspace).put(id, item);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#contains(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public boolean contains(Serializable id, Serializable keyspace) {
		return get(id, keyspace) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#get(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object get(Serializable id, Serializable keyspace) {

		Assert.notNull(id, "Cannot get item with null id.");
		return getKeySpaceMap(keyspace).get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#delete(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object delete(Serializable id, Serializable keyspace) {

		Assert.notNull(id, "Cannot delete item with null id.");
		return getKeySpaceMap(keyspace).remove(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#getAllOf(java.io.Serializable)
	 */
	@Override
	public Collection<?> getAllOf(Serializable keyspace) {
		return getKeySpaceMap(keyspace).values();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#deleteAllOf(java.io.Serializable)
	 */
	@Override
	public void deleteAllOf(Serializable keyspace) {
		getKeySpaceMap(keyspace).clear();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#hasKeyspace(java.io.Serializable)
	 */
	@Override
	public boolean hasKeyspace(Serializable keyspace) {

		Assert.notNull(keyspace, "Collection must not be null for lookup.");
		return store.containsKey(keyspace);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#clear()
	 */
	@Override
	public void clear() {
		store.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		clear();
	}

	/**
	 * Get map associated with given keyspace.
	 * 
	 * @param keyspace must not be {@literal null}.
	 * @return
	 */
	protected Map<Serializable, Object> getKeySpaceMap(Serializable keyspace) {

		Assert.notNull(keyspace, "Collection must not be null for lookup.");

		Map<Serializable, Object> map = store.get(keyspace);

		if (map != null) {
			return map;
		}

		addMapForKeySpace(keyspace);
		return store.get(keyspace);
	}

	private void addMapForKeySpace(Serializable keyspace) {
		store.put(keyspace, CollectionFactory.<Serializable, Object> createMap(keySpaceMapType, 1000));
	}
}
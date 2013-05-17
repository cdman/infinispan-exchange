package com.blogspot.hypefree.infinispantest;

import java.io.IOException;
import java.util.*;

import org.infinispan.Cache;
import org.infinispan.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public final class HotRodMapWrapper<K, V> implements Map<K, V> {
	private static final Log LOG = LogFactory.getLog(HotRodMapWrapper.class);

	private final Cache<byte[], byte[]> hotRodCache;
	private final GenericJBossMarshaller marshaller;

	private HotRodMapWrapper(Cache<byte[], byte[]> hotRodCache) {
		this.hotRodCache = hotRodCache;
		this.marshaller = new GenericJBossMarshaller();
	}

	public static <K, V> HotRodMapWrapper<K, V> wrap(
			Cache<byte[], byte[]> hotRodCache) {
		return new HotRodMapWrapper<>(hotRodCache);
	}

	public byte[] serialize(Object object) {
		try {
			return marshaller.objectToByteBuffer(object);
		} catch (IOException | InterruptedException e) {
			LOG.error("Failed to serialize " + object, e);
			throw new IllegalArgumentException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] bytes) {
		try {
			return (T) marshaller.objectFromByteBuffer(bytes);
		} catch (ClassNotFoundException | IOException e) {
			LOG.error("Error while deserializing object", e);
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(Object key) {
		byte[] hotRodKey = serialize(key);
		byte[] hotRodValue = hotRodCache.get(hotRodKey);
		if (hotRodValue == null) {
			for (Map.Entry<byte[], byte[]> entry : hotRodCache.entrySet()) {
				if (Arrays.equals(entry.getKey(), hotRodKey)) {
					System.out.println("Here!");
				}
			}
			
			return null;
		}
		return deserialize(hotRodValue);
	}

	@Override
	public V put(K key, V value) {
		hotRodCache.put(serialize(key), serialize(value));
		return null;
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
}

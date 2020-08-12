package org.eclipse.ecf.internal.provider.etcd.protocol;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;


public class Etcd {
	public Client client;
	
	public KV kvClient;
	
	public Etcd(String endpoint) {
		this.client = Client.builder().endpoints(endpoint).build();
		this.kvClient = client.getKVClient();
	}
	
	public void put(String k, String v) throws InterruptedException, ExecutionException  {
		ByteSequence key = ByteSequence.from(k.getBytes());
		ByteSequence value = ByteSequence.from(v.getBytes());
		kvClient.put(key, value).get();
	}
	
	//TODO add range based get request 
	
	
	@SuppressWarnings("nls")
	public Map<String, String> get(String k) throws InterruptedException, ExecutionException {
		ByteSequence key = ByteSequence.from(k.getBytes());
		GetResponse getResponse = kvClient.get(key).get();
		
		Map<String, String> keyValueMap = new HashMap<>();

		for (KeyValue kv : getResponse.getKvs()) {
		    keyValueMap.put(
		        kv.getKey().toString(Charset.forName("UTF-8")),
		        kv.getValue().toString(Charset.forName("UTF-8"))
		    );
		}
		return keyValueMap;
	}
	
	//TODO
	public void delete(String begin, String end) {
		
	}
	
	//TODO
	public void watch(String begin, String end) {
		
	}
	
}

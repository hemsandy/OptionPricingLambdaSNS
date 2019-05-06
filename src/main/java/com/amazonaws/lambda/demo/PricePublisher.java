package com.amazonaws.lambda.demo;

import redis.clients.jedis.Jedis;

public class PricePublisher {
	private static Jedis jedis = new Jedis("wshop-redis-cluster.cl6qco.0001.use2.cache.amazonaws.com"); 
	
	public static void publishOptionPrice(OptionData option) {
		try {
			jedis.set(option.getOptionName(), Double.toString(option.getOptionPrice()));
		} catch (Exception ex) {
			throw ex; 
		}		
	}
	
	public static void closePricePublisher() {
		jedis.close();
	}
}

package com.amazonaws.lambda.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;
 

public class RefDataReader {
	
	private static Jedis jedis = new Jedis("wshop-refdata-cache.cl6qco.0001.use2.cache.amazonaws.com"); 

	public static OptionData getOption(String optionName, double underlyingPrice) {
		try {
			String op = jedis.get(optionName); 
			JsonObject opData = new JsonParser().parse(op).getAsJsonObject(); 
						
			double calcPrice = 0.0;
			
			//Now populate the Option Data
			OptionData option = new OptionData(
					"AAPL",
					opData.get("contractSymbol").getAsString(), 
					opData.get("strike").getAsDouble(), 
					opData.get("impliedVolatility").getAsDouble(),					
					LocalDate.parse(opData.get("expiration").getAsString(), OptionData.formatter),
					underlyingPrice, //stockPrice to be set later, is basically underlying_price
					calcPrice, //This is what we have to calculate
					LocalDateTime.parse(opData.get("lastTradeDate").getAsString(), OptionData.formatter) 
					);
			
			return option;	
			
		} catch (Exception ex) {
			throw ex; 
		}
	}
	
	public static void closeRefDataReader() {
			jedis.close();
	}
	
	
}

package com.amazonaws.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import redis.clients.jedis.Jedis;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import org.jquantlib.Settings;
import org.jquantlib.daycounters.Actual365Fixed;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.EuropeanExercise;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.EuropeanOption;
import org.jquantlib.instruments.Option;
import org.jquantlib.instruments.Payoff;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.instruments.VanillaOption;
import org.jquantlib.pricingengines.AnalyticEuropeanEngine;
import org.jquantlib.processes.BlackScholesMertonProcess;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.SimpleQuote;
import org.jquantlib.termstructures.BlackVolTermStructure;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.termstructures.volatilities.BlackConstantVol;
import org.jquantlib.termstructures.yieldcurves.FlatForward;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Date;
import org.jquantlib.time.calendars.Target;


import com.amazonaws.lambda.demo.OptionData;
import com.amazonaws.lambda.demo.RefDataReader;

/*
 *          header = ['contractSymbol', 'strike', 'currency', 'lastPrice', 'change', 'percentChange', 
 *          'openInterest', 'bid', 'ask', 'contractSize', 'expiration', 
 *          'lastTradeDate', 'impliedVolatility', 'inTheMoney']
 * */

public class OptionsPricingSNSHandler implements RequestHandler<SNSEvent, String> {
	
    @Override
    public String handleRequest(SNSEvent event, Context context) {
    	try {
	    	double underlyingPrice = 211.75;  //Hard code for underlying price as of now   	
	    	
	    	String rawOptions = event.getRecords().get(0).getSNS().getMessage();
	    	String[] options = rawOptions.split(","); 
	    	context.getLogger().log("SNS Message string = " + Arrays.toString(options));
	    	
	    	for (int i = 0; i < options.length; i++) {
	    		//Extract the OptionName and Create OptionData
	    		//context.getLogger().log("Calculating price for option = " + options[i]);
	    		OptionData option = RefDataReader.getOption(options[i], underlyingPrice); 
	    		option.setOptionPrice(price(option, option.getStockPrice()));
	    		//context.getLogger().log(" Price for Option: " + options[i]  + " = " + option.getOptionPrice());
	    		
	    		//Publish the Price
	    		PricePublisher.publishOptionPrice(option);
	    	}     	
	        
	        String message = rawOptions; //event.getRecords().get(0).getSNS().getMessage();
	        context.getLogger().log("From SNS: " + message);
	        return message;
    	} catch (Exception ex) {
    		throw ex; 
    	}
    	finally {
    		RefDataReader.closeRefDataReader();
    		PricePublisher.closePricePublisher();
    	}
    }
    
	private double price(OptionData optionData, double underlying) {

		final Option.Type type = Option.Type.Call;
		//String optionName = optionData.getOptionName();
		/* @Rate */final double riskFreeRate = 0.0256;
		double impVol = optionData.getVolatility();
		if (impVol == 0.0)
			impVol = 1;
		final double volatility = impVol;
		final double dividendYield = 0.00;
		// set up dates
		final Calendar calendar = new Target();
		final Date todaysDate = new Date(new java.util.Date());
		new Settings().setEvaluationDate(todaysDate);
		LocalDate localDate = optionData.getExpiryDate();
		Date expiryDate = new Date(java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));

		final DayCounter dayCounter = new Actual365Fixed();
		final Exercise europeanExercise = new EuropeanExercise(expiryDate);

		// bootstrap the yield/dividend/volatility curves
		final Handle<Quote> underlyingH = new Handle<Quote>(new SimpleQuote(
				underlying));
		final Handle<YieldTermStructure> flatDividendTS = new Handle<YieldTermStructure>(
				new FlatForward(todaysDate, dividendYield, dayCounter));
		final Handle<YieldTermStructure> flatTermStructure = new Handle<YieldTermStructure>(
				new FlatForward(todaysDate, riskFreeRate, dayCounter));
		final Handle<BlackVolTermStructure> flatVolTS = new Handle<BlackVolTermStructure>(
				new BlackConstantVol(todaysDate, calendar, volatility,
						dayCounter));
		final Payoff payoff = new PlainVanillaPayoff(type, optionData.getStrike());

		final BlackScholesMertonProcess bsmProcess = new BlackScholesMertonProcess(
				underlyingH, flatDividendTS, flatTermStructure, flatVolTS);

		// European Options
		final VanillaOption europeanOption = new EuropeanOption(payoff,
				europeanExercise);

		europeanOption.setPricingEngine(new AnalyticEuropeanEngine(bsmProcess));
		// Black-Scholes for European
		return europeanOption.NPV();
	}
    
    
}

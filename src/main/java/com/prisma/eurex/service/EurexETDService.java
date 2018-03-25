package com.prisma.eurex.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.opengamma.margining.eurex.prisma.api.DataSource;
import com.opengamma.margining.eurex.prisma.api.FilesystemDataSource;
import com.opengamma.margining.eurex.prisma.api.LiquidationGroupMargin;
import com.opengamma.margining.eurex.prisma.api.LiquidationGroupSplitMargin;
import com.opengamma.margining.eurex.prisma.api.Margin;
import com.opengamma.margining.eurex.prisma.api.MarginCalculator;
import com.opengamma.margining.eurex.prisma.api.MarginData;
import com.opengamma.margining.eurex.prisma.api.PnlCalculationMethod;
import com.opengamma.margining.eurex.prisma.api.PortfolioFailure;
import com.opengamma.margining.eurex.prisma.api.PortfolioLoader;
import com.opengamma.margining.eurex.prisma.api.TradeFailure;
import com.opengamma.margining.eurex.prisma.api.Var;
import com.opengamma.margining.eurex.prisma.impl.data.DataFilesFactory;
import com.opengamma.margining.eurex.prisma.impl.market.MarketDataUtils;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.common.PutCall;
import com.prisma.eurex.pojo.State;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;



@Service("etdService")
public class EurexETDService {
	
	
	@Value("${market.data.directory}")
	private String marketDataPath;

	@Value("${portfolio.file}")
	private String portfoliopath;
	
	@Autowired
	private State state;
	
	private MarginData marginData;

	private static final Logger logger = Logger.getLogger(EurexETDService.class);
	
	public void loadMarginData(String date) throws Exception {
		logger.info("Entering the loadMarginData");
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		state.setStatus("Unloaded");
		logger.info("State status changed to: " + state.getStatus());
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date inputDate = dateFormat.parse(date);
		Path marketDataDirectory = Paths.get(marketDataPath);
		DataFilesFactory dataFilesFactory = DataFilesFactory.standard(marketDataDirectory);
		//Path marketDataDirectory = Paths.get("data");
		//-------------------------------------------------------------
	    // Create the components which load the data and calculate the margin

	    // Thread pool used to load data and calculate margin in parallel
	    // The data source loads data from the Eurex Transparency Enabler files
	    DataSource dataSource = FilesystemDataSource.create(executor, dataFilesFactory);
	    // The margin calculator performs the calculations		
	    
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(inputDate);
	    int year = cal.get(Calendar.YEAR);
	    int month = cal.get(Calendar.MONTH)+1;
	    int day = cal.get(Calendar.DAY_OF_MONTH);
		
	    // The date for which margin will be calculated. Market data must be available for this date
	    LocalDate valuationDate = LocalDate.of(year,month,day);
	    state.setStatus("Loading");
	    logger.info("State status changed to: " + state.getStatus());
	    logger.info("Loading the loadMarginData");
	    marginData = dataSource.getEtdMarginData(valuationDate);
	    logger.info("Loading the loadMarginData completed");
	    state.setDatasetDate(inputDate);
		state.setStatus("Loaded");
		logger.info("State status changed to: " + state.getStatus());
		logger.info("DataSetDate: " + dateFormat.format(state.getDatasetDate()));
		executor.shutdown();
		
				
	}
	

	public void getPortfolioMargin(Element portfolios,Document document)throws Exception{
		logger.info("Entering the getPortfolioMargin");
		@SuppressWarnings("unchecked")
		List<Element> portfoliolist = portfolios.getChildren();
		Path portfolioFile = null;
		Element portfolioReports = new Element("PortfolioReports");
		Element resp = document.getRootElement();
		resp.addContent(portfolioReports);
		// Thread pool used to load data and calculate margin in parallel
	    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	    // The margin calculator performs the calculations
	    MarginCalculator marginCalculator = MarginCalculator.create(executor);
		for( Element portfolioElem : portfoliolist ){
			Element portfolioReport = new Element("PortfolioReport");
			portfolioReports.addContent(portfolioReport);
			Element file = portfolioElem.getChild("TradeFile");
			Element account = portfolioElem.getChild("Account");
			Element accountClone = (Element)account.clone();
			portfolioReport.addContent(accountClone);
			Element currencyElem = portfolioElem.getChild("BaseCurrency");
			Currency currency = Currency.of(currencyElem.getText());
			List<? extends CalculationTarget> portfolio = null;
			if ( file != null ){
				logger.info("The portfolio margin is calculated using file based");
				String tradeFileName = file.getText();
			    portfolioFile = Paths.get(portfoliopath + tradeFileName);
			    
			    // The result of loading the portfolio, including the trades and details of any errors which occurred
			    ValueWithFailures<List<? extends CalculationTarget>> portfolioResult =
			        PortfolioLoader.load(marginData.getReferenceData(), portfolioFile);
			    logger.info("The results loaded in portfolio");

			    // The trades in the portfolio
			    portfolio = portfolioResult.getValue();
			    
			}else{
				logger.info("The portfolio margin is calculated using portfolio builder");
				portfolio = this.buildPortfolio(portfolioElem);
				logger.info("The results loaded in portfolio");
			}
			//-------------------------------------------------------------
		    // Calculate the margin
			// The method used to calculate P&L for each trade
		    PnlCalculationMethod calculationMethod = PnlCalculationMethod.FULL_REVAL;
		    // Flag indicating whether cross-margining should be used
		    boolean crossMargining = false;
		    Margin margin = marginCalculator.margin(
		        portfolio,
		        marginData,
		        currency,
		        calculationMethod,
		        crossMargining);
		    logger.info("The portfolio margin is calculated");
		    // Print the calculated margin
		    CurrencyAmount initialMargin = margin.getInitialMargin();
		    Element methodology = new Element("Methodology");
		    portfolioReport.addContent(methodology);
		    if( initialMargin != null ){
			    Element imTotal = new Element("IMTotal");
			    imTotal.setAttribute("currency",initialMargin.getCurrency().toString());
			    imTotal.setText(""+initialMargin.getAmount());
			    logger.info("IMTotal:" + initialMargin.getAmount());
	    		portfolioReport.addContent(imTotal);
		    }
    		
		 // Print the margin for each liquidation group
		    Element LiquidationGroups = new Element("LiquidationGroups");
		    portfolioReport.addContent(LiquidationGroups);
		    logger.info("Calculating the margin for each liquidation group");
		    for (LiquidationGroupMargin liquidationGroupMargin : margin.getLiquidationGroupMargins()) {
		    	Element LiquidationGroup = new Element("LiquidationGroup");
		    	LiquidationGroups.addContent(LiquidationGroup);
		    	String liquidationGroupName = liquidationGroupMargin.getLiquidationGroupName();
		    	CurrencyAmount groupInitialMargin = liquidationGroupMargin.getInitialMargin();
		    	LiquidationGroup.setAttribute("name",liquidationGroupName);
		    	Element imTotal_liq = new Element("IMTotal");
		    	LiquidationGroup.addContent(imTotal_liq);
		    	imTotal_liq.setText("" + groupInitialMargin.getAmount());
		    	imTotal_liq.setAttribute("currency",groupInitialMargin.getCurrency().toString());
		      
		    	Element LiquidationSplits = new Element("LiquidationSplits");
		    	LiquidationGroup.addContent(LiquidationSplits);
		      // Print the margin for each liquidation group split
		    	for (LiquidationGroupSplitMargin splitMargin : liquidationGroupMargin.getLiquidationGroupSplitMargins()) {
			    		Element LiquidationSplit = new Element("LiquidationSplit");
			    		String liquidationGroupSplitName = splitMargin.getLiquidationGroupSplitName();
			    		CurrencyAmount splitInitialMargin = splitMargin.getInitialMargin();
			    		LiquidationSplit.setAttribute("name",liquidationGroupSplitName);
			    		LiquidationSplits.addContent(LiquidationSplit);
			    		Element imTotal_liqsplit = new Element("IMTotal");
			    		LiquidationSplit.addContent(imTotal_liqsplit);
			    		imTotal_liqsplit.setText("" + splitInitialMargin.getAmount());
			    		imTotal_liqsplit.setAttribute("currency",splitInitialMargin.getCurrency().toString());
			    		
			    		Var var = splitMargin.getVar();
				        CurrencyAmount totalVar = var.getTotalVar();
			    		Element imTotal_liqsplitTotalVar = new Element("TotalVaR");
			    		LiquidationSplit.addContent(imTotal_liqsplitTotalVar);
			    		imTotal_liqsplitTotalVar.setText("" + totalVar.getAmount());
			    		imTotal_liqsplitTotalVar.setAttribute("currency",totalVar.getCurrency().toString());
			    		
			    		CurrencyAmount liquidityAddOn = splitMargin.getLiquidityAddOn();
			    		Element imTotal_liqsplitAddOn = new Element("AddOn");
			    		LiquidationSplit.addContent(imTotal_liqsplitAddOn);
			    		imTotal_liqsplitAddOn.setText("" + liquidityAddOn.getAmount());
			    		imTotal_liqsplitAddOn.setAttribute("currency",liquidityAddOn.getCurrency().toString());
		    		}
		    }
		    Collection<TradeFailure> list = margin.getTradeFailures();
    		Collection<PortfolioFailure> list1 = margin.getPortfolioFailures();
		    if ( list != null && !list.isEmpty()){
		    	logger.info("Adding the Trade Errors to the response");
		    	Element tradeErrors = new Element("TradeErrors");
		    	portfolioReport.addContent(tradeErrors);
		    	for( TradeFailure x: list){
		    		SecurityPosition sec = (SecurityPosition)x.getTarget();
		    		Element tradeError = new Element("TradeError");
		    		if( sec.getInfo()!= null && sec.getInfo().getId() != null){
		    			Optional<StandardId> op = sec.getInfo().getId();
		    			if(op.isPresent()){
		    				tradeError.setAttribute("externalID",sec.getInfo().getId().get()+"");
		    			}
		    		}
		    		
		    		if(sec.getSecurityId() != null && sec.getSecurityId().getStandardId() != null){
		    			
		    			tradeError.setAttribute("productID",sec.getSecurityId().getStandardId() + "");
		    		}
		    		tradeError.setAttribute("message", x.getFailure().getMessage());
		    		
		    		tradeErrors.addContent(tradeError);
		    	}
		    }
		    
		    if ( list1 != null && !list1.isEmpty()){
		    	logger.info("Adding the Portfolio Errors to the response");
		    	Element tradeErrors = new Element("PortfolioErrors");
		    	portfolioReport.addContent(tradeErrors);
		    	for( PortfolioFailure x: list1){
		    		Element tradeError = new Element("PortfolioError");
		    		tradeError.setAttribute("message",x.getFailure().getMessage());
		    		tradeErrors.addContent(tradeError);
		    	}
		    }

		}
		executor.shutdown();
		logger.info("Leaving the getPortfolioMargin ");
	}
	
	@SuppressWarnings("unchecked")
	private List<? extends CalculationTarget> buildPortfolio(Element portfolioElem) throws Exception{
		logger.info("Entering the buildPortfolio ");
		Element trades = portfolioElem.getChild("Trades");
		List<Element> tradeList = trades.getChildren();
		@SuppressWarnings("rawtypes")
		List portfolioList = new ArrayList();
		for(Element trade: tradeList){
			SecurityPosition pos = createEtdTrade(trade);
			portfolioList.add(pos);
		}
		logger.info("Leaving the buildPortfolio ");
		return portfolioList;
	}
	
	private SecurityPosition createEtdTrade(Element etdTrade) throws Exception {
		Element External = etdTrade.getChild("ExternalID");
		String externalId = "";
		if(External != null){
			externalId = External.getText();	
		}
		logger.info("Entering the createEtdTrade method ");
		Element product = etdTrade.getChild("ProductID");
		String productId = product.getText();			
		Element expiryYearElem = etdTrade.getChild("ExpiryYear");
		int expiryYear = Integer.parseInt(expiryYearElem.getText());			
		Element expiryMonthElem = etdTrade.getChild("ExpiryMonth");
		int expiryMonth = Integer.parseInt(expiryMonthElem.getText());	
		Element versionNumberElem = etdTrade.getChild("VersionNumber");
		int versionNumber = 0;
		if ( versionNumberElem != null ){
			versionNumber = Integer.parseInt(versionNumberElem.getText());	
		}
		
		Element PutCallElem = etdTrade.getChild("CallPutFlag");
		PutCall putCall = null;
		if ( PutCallElem != null ){
			String putCallStr = PutCallElem.getText();	
			if (putCallStr != null && putCallStr.equalsIgnoreCase("C") ){
				putCall = PutCall.CALL;
			}else if (putCallStr != null && putCallStr.equalsIgnoreCase("P") ){
				putCall = PutCall.PUT;
			}
		}
		Element exercisePriceElem = etdTrade.getChild("ExercisePrice");
		BigDecimal exercisePrice = null;
		if ( exercisePriceElem != null ){
			exercisePrice = new BigDecimal(exercisePriceElem.getText());	
		}
		Element instrumentTypeElem = etdTrade.getChild("InstrumentType");
		String instrumentType = instrumentTypeElem.getText();
		Element qty = etdTrade.getChild("Qty");
		double quantity = Double.parseDouble(qty.getText());
		
		
		SecurityId id =  null;
		if (instrumentType != null && instrumentType.equalsIgnoreCase("Future")){
			logger.info("InstrumentType is Future");
			id = SecurityId.of(MarketDataUtils.buildClearedEtdFutureSecurityId(
			            productId,
			            expiryYear,
			            expiryMonth));
		}else if(instrumentType != null && instrumentType.equalsIgnoreCase("Option")){
			logger.info("InstrumentType is Option");
			id = SecurityId.of(MarketDataUtils.buildClearedEtdOptionSecurityId(
		            productId,expiryYear,expiryMonth,versionNumber,putCall,exercisePrice));
		}
		StandardId stdId = null;
		if ( External != null ){
			stdId = StandardId.of(id.getStandardId().getScheme(), externalId);
			logger.info("Leaving the createEtdTrade method ");
		    return SecurityPosition.ofNet(PositionInfo.of(stdId), id,quantity);
		}else{
			logger.info("Leaving the createEtdTrade method ");
		    return SecurityPosition.ofNet(id,quantity);
		}
    }
	

	
	

}

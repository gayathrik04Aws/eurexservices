package com.prisma.eurex.controller;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xml.sax.InputSource;
import com.prisma.eurex.pojo.State;
import com.prisma.eurex.service.EurexETDService;
import com.prisma.eurex.util.DatesUtil;
import com.prisma.eurex.util.XMLResponseUtil;

@Controller
public class PrismaController {

	@Autowired
	private State state;

	@Autowired
	private EurexETDService etdService;
	
	@Value("${market.data.directory}")
	private String marketDataPath;

	private static final Logger logger = Logger.getLogger(PrismaController.class);

	@RequestMapping(method = RequestMethod.POST, value = "command", headers = "Accept=application/xml")
	public @ResponseBody String command(@RequestBody String request) throws Exception {
		SAXBuilder xmlbuilder = new SAXBuilder();
		Document xmldocument = xmlbuilder.build(new InputSource(new StringReader(request.toString())));
		Element elem = xmldocument.getRootElement();
		String method = elem.getAttributeValue("type");
		XMLOutputter xmOut = new XMLOutputter(); 
		logger.info("Input Request:" + xmOut.outputString(xmldocument));
		logger.info("command type:" + method);
		if (method != null && method.equalsIgnoreCase("getState")) {
			String output = XMLResponseUtil.getStateResponse(method, state);
			logger.info("Output Response:" + output);
			return output;
		} else if (method != null && method.equalsIgnoreCase("LoadDataset")) {
			Element datasetdate = elem.getChild("params").getChild("DatasetDate");
			String status = "Success";
			String statusCode = "0";
			String message = "Success";
			try{
				etdService.loadMarginData(datasetdate.getText());
			}catch(Exception e){
				logger.error(e.getMessage(),e);
				e.printStackTrace();
				status = "Failure";
				statusCode = "-1";
				message = e.getMessage();
				if( message == null ){
					message = "Failure";
				}
				this.recursivelyLoadMarginData(datasetdate.getText());
			}
			String output = XMLResponseUtil.getLoadMarginDataResponse(method,status,statusCode,message);
			logger.info("Output Response:" + output);
			return output;
		} else if (method != null && method.equalsIgnoreCase("GetPortfolioMargin")) {
			Element portfolios = elem.getChild("params").getChild("Portfolios");
			Document resp = XMLResponseUtil.getResponseElement(method);
			String status = "Success";
			String statusCode = "0";
			String message = "Success";
			try{
				etdService.getPortfolioMargin(portfolios, resp);
			}catch(Exception e){
				logger.error(e.getMessage(),e);
				e.printStackTrace();
				status = "Failure";
				statusCode = "-1";
				message = e.getMessage();
				if( message == null ){
					message = "Failure";
				}
			}
			String output = XMLResponseUtil.getResponseAsString(resp,status,statusCode,message);
			logger.info("Output Response:" + output);
			return output;
		}
		logger.warn("No Command found");
		return request;

	}
	
	
	private void recursivelyLoadMarginData(String currentDataSet){
		logger.info("Data loadset failed loading the previous dataset");
		List<Date> dateList = DatesUtil.getFolderDates(marketDataPath);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		for ( Date dataset: dateList ){
			logger.info("Loading the dataset: " + dateFormat.format(dataset));
			if( currentDataSet.equals(dateFormat.format(dataset)) ){
				continue;
			}
			try{
				etdService.loadMarginData(dateFormat.format(dataset));
				logger.info("Previous DataSet successfully loaded: " + dateFormat.format(dataset) );
				return;
			}catch(Exception e){
				logger.error(e.getMessage(),e);
				e.printStackTrace();
				logger.info("Data loadset failed: " + dateFormat.format(dataset));
			}
		}
		       
	}
}

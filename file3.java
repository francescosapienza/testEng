package com.cervedgroup.resmart.ws.resmartexperianintegration.serviceImpl;


import it.pitagora.novasiri.service.invoker.SmartInvokerFactory;
import it.pitagora.util.env.Env;
import it.pitagora.util.ini.IniReader;
import it.pitagora.util.log.Log;
import it.pitagora.util.log.LogFactory;
import it.pitagora.util.xml.XMLUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.annotation.W3CDomHandler;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xml.utils.DOMBuilder;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.helper.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cervedgroup.resmart.service.smartexperian.interfaces.ISmartExperianService;
import com.cervedgroup.resmart.service.smartexperian.model.Certificato;
import com.cervedgroup.resmart.service.smartexperian.model.ConservatoriaRichiesta;
import com.cervedgroup.resmart.service.smartexperian.model.Intestazione;
import com.cervedgroup.resmart.service.smartexperian.model.ServiceDataOutput;
import com.cervedgroup.resmart.service.smartexperian.model.SmartExperianOutput;
import com.cervedgroup.resmart.ws.resmartexperianintegration.dao.IResmartWebDao;
import com.cervedgroup.resmart.ws.resmartexperianintegration.support.bean.JbSdpGetawayRec;
import com.cervedgroup.resmart.ws.resmartexperianintegration.support.bean.WkParametriElabRec;
import com.cervedgroup.resmart.ws.resmartexperianintegration.support.xsd.GetResponse;
import com.cervedgroup.resmart.ws.resmartexperianintegration.support.xsd.GetResponseResponse;
import com.cervedgroup.resmart.ws.resmartexperianintegration.support.xsd.GetResponseResponse.CORPO;
import com.cervedgroup.resmart.ws.resmartexperianintegration.util.Constant;
import com.cervedgroup.resmart.ws.resmartexperianintegration.util.Util;
import com.cervedgroup.resmart.ws.resmartexperianintegration.util.UtilJaxb;

public class ResmartExperianIntegration {
	
	private static String CLASSNAME = "ResmartExperianIntegration";
	private static String SERVICE = "resmartExperianIntegration";
	
	private static IniReader objIni = null;
	private static Log objLog = null;
	private static Util util = null;
	private IResmartWebDao dao ;
	
	
	static {
		try {
			System.out.println("path_home="+Env.getSqtpHome() + "etc/resmart/" + SERVICE);
			objIni = new IniReader(Env.getSqtpHome() + "etc/resmart/" + SERVICE);
			System.out.println("objIni="+objIni);
			objLog = new Log(objIni);
			System.out.println("objLog="+objLog);
			objLog.logInit(SERVICE, "ON_LINE");
			util = new Util(objLog);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Costruttore
	 */
	public ResmartExperianIntegration() {

	}
	/**
	 * Metodo di business.
	 * @param request	request
	 * @return response
	 * @throws Exception Exception
	 */
	public GetResponseResponse businessMethod (GetResponse request) throws Exception {
		String meth = "businessMethod";
		GetResponseResponse response = null;
		long startEsec = 0;
		String[] risCheckParam = new String[2];
		
		UtilJaxb utilJaxb = null;
	 	String formatoDataAAAAMMGG = "yyyyMMddHHmmss";
		SimpleDateFormat sdfDataAAAAMMGG = null;
		String sysdateAAAAMMGG = "";

		String serviceCode = "";
		String msg = "";
		Intestazione intestazione = null;
		Certificato certificato = null;
		Long idIstanza = null;
		Long idRichiesta = null;
		//LegacyCompliantRequestManager legacy = null;

		try {
			LogFactory.setSessionLogger(objLog);
			startEsec = System.currentTimeMillis();
			objLog.info("START");

			Date dateElab = new Date();
			
			sdfDataAAAAMMGG = new SimpleDateFormat(formatoDataAAAAMMGG);
			sysdateAAAAMMGG = sdfDataAAAAMMGG.format(dateElab);
			utilJaxb = new UtilJaxb(objLog);

			intestazione = new Intestazione();
			certificato = new Certificato();
			
			idRichiesta = dao.selectNextIdRichiestaSeq();
			idIstanza = dao.selectNextIdIstanzaSeq();
			
			objLog.info("idRichiesta = "+idRichiesta);
			
			//verifica i parametri ritornando un codice ed una descrizione in caso di errore
			//valorizza intestazione e certificato inserendo anche il serviceCode che serve sia in caso di errore che di risposta corretta
			risCheckParam = dataBindigCheckInput(intestazione,certificato,request);

			if (risCheckParam[0].equals("")) {
				
			} else {
				
				serviceCode = intestazione.getCodiceId();
				
				
				//invoca Smart service per effettuare la richiesta 
				//il quale verifica se esiste o meno la richiesta
				// e in base a quello crea la risposta 

				/*
				 * IF servizio smart ritorna errore
				 * allora bisogna ritornare al chiamante errore 
				 * 
				 * IF risposta intermedia 
				 * 
				 */
				
				
				ISmartExperianService service = 
					SmartInvokerFactory.getSmartInvoker(
							ISmartExperianService.class,"RESMART", "SMARTEXPERIAN");
				
				objLog.info("invoco il servizio RESMART.SMARTEXPERIAN ");
				
				SmartExperianOutput out = service.gestioneRichiesta(intestazione, certificato);

				objLog.info("invocato il servizio con esito = "+out.getCodEsito()+",  errore ="+out.getCodErrore()+", msg"+out.getDescErrore());
				
				String codEsito = out.getCodEsito();
				String codErrore = out.getCodErrore();
				msg = out.getDescErrore();
				
				
				if(codEsito.equalsIgnoreCase("1")){
					objLog.info("rispondo con una risposta intermedia ");

					//Risposta intermedia
					response = (GetResponseResponse)getIntermedyResponse(
							serviceCode, codEsito, msg, sysdateAAAAMMGG, utilJaxb, 
							intestazione, certificato, out,idIstanza,idRichiesta);
					
				}else if(codEsito.equalsIgnoreCase("0")){
					objLog.info("rispondo con l'evasione");
					//Evasione della richiesta
					response = (GetResponseResponse)getEvasaResponse(serviceCode, 
							codEsito, msg, 
							sysdateAAAAMMGG, intestazione, certificato, out,idIstanza,idRichiesta);
					
				}else{
					objLog.info("ci sono stati dei problemi");
					// ci sono stati dei problemi per cui rispondo con errore
					response = (GetResponseResponse) getErrorResponse(serviceCode,codErrore,
							msg,sysdateAAAAMMGG,utilJaxb,intestazione,certificato,
							idIstanza,out,idRichiesta);
					
				}
				archiviaEsito( codEsito, msg,
						 intestazione, certificato,
						 out, idRichiesta);
				
				
			}
		} catch (Exception e) {
			util.logExceptionStackTrace(objLog, e);
			
			if(intestazione == null){
				intestazione = new Intestazione();
			}
			if(certificato == null){
				certificato = new Certificato();
			}
			msg = "Errore durante l'esecuzione del servizio";
			//Long id=dao.selectNextIdRichiestaSeq();
			response = (GetResponseResponse) getErrorResponse(serviceCode,"-100",
					msg,sysdateAAAAMMGG,utilJaxb,intestazione,certificato,
					idIstanza,null,idRichiesta);
			
			archiviaEsito( "-100", msg,
					 intestazione, certificato,
					 null, idRichiesta);
			
		} finally {
			destroy();
			long endEsec = (System.currentTimeMillis() - startEsec);
			objLog.info("#"+CLASSNAME+"."+meth+"#"+endEsec+"#");		
			objLog.info("END");		
		}
		return response;
	}


	
	private Object getIntermedyResponse(String serviceCode,String code,String msg,String sysdate, 
			UtilJaxb utilJaxb,Intestazione intestazione,Certificato certificato,
			SmartExperianOutput out,Long idIstanza,Long idRichiestaSeq) throws Exception {
		
		objLog.info("START");
		
		String outputXml = "";
	 	String pkgSchema = "com.cervedgroup.resmart.ws.resmartexperianintegration.support.xsd";
		String qname = "";
		GetResponseResponse resp = null;
		
		ArrayList<ServiceDataOutput> list = out.getListServiceDataOutput();
		
		ServiceDataOutput ser = list.get(0);
		
		
		String codSorgente = "";
		String idCertificato= "";
		String idServizioA ="82";
		String idServizioB ="238";
		String foglioStile = "";
		
		String xmlIntermedio= null;
		
		String pathFile = Env.getHome() + "/dat/RESMART/RESMARTEXPERIANINTEGRATION/1/xsl/FOGLIO_DI_STILE.xsl";
		
		String encodedFoglioStile = new String(Base64.encodeBase64(read(pathFile)));
		
		if(serviceCode.equalsIgnoreCase("S040000009")){
			codSorgente = "SMART";
			idCertificato= "86";
			
			if(!certificato.getTipoOutput().equalsIgnoreCase("xml"))
				foglioStile ="						<FOGLIOSTILE xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
					+ encodedFoglioStile
					+"						</FOGLIOSTILE>";
			else
				foglioStile = "<FOGLIOSTILE />";
			
			
			//TODO verificando alcuni casi di esempio ho notato
			// che se il tipo soggetto è giuridico allora
			// l'idservizio della seconda busta SORGENTE vale 240
			// ATTENZIONE: essendo una deduzione fatta da una analisi 
			// di alcuni esempi, e non da specifiche fornite dal cliente
			// la cosa potrebbe non essere vera
			if(certificato.getTipoSoggetto()!=null && certificato.getTipoSoggetto().equalsIgnoreCase("G"))
				idServizioB ="240";
		
			xmlIntermedio= "<ns6:visure>";
			
			for (ServiceDataOutput serviceDataOutput : list) {
				
				String tempXML= "<ns6:listaVisure>"+
				"<ns6:conservatoria>"+
					"<ns4:codiceConservatoria>"+serviceDataOutput.getCodiceConservatoria()+"</ns4:codiceConservatoria>"+
				"</ns6:conservatoria>"+
				"<ns6:statoVisura>1</ns6:statoVisura>"+
			    " </ns6:listaVisure>";
				xmlIntermedio = xmlIntermedio +tempXML;
				
			}
			
			
			xmlIntermedio = xmlIntermedio+ "</ns6:visure>";
			
			
			
		}
		
		if(serviceCode.equalsIgnoreCase("S110000000")){
			codSorgente = "DATI CAT SOAR";
			idCertificato= "93";
			foglioStile = "<FOGLIOSTILE />";
			idServizioB ="248";
		}
		
		String idRich = ser.getIdRichiesta();
		Long idRichL = Long.valueOf(idRich);
		
		String codRich = dao.selectCodRichClie(idRichL) ;
		
		
		String hhh ="         <CORPO>"
		+"				<CORPO>"
		+"					<ROOT DATAISTANZA=\""+sysdate+"\" ERRORE=\"0\" IDRICHIESTA=\""+idRichiestaSeq+"\""
		+" PREZZABILE=\"1\" CRITICO=\"0\" CODICERICHIESTA=\""+codRich+"\" EVASA=\"0\">"
		+"						<DATICERT IDISTANZA=\""+idIstanza+"\" DATASTART=\""+sysdate+"\""
		+" DATAEND=\""+sysdate+"\" IDCERTIFICATO=\""+idCertificato+"\">"
		+"							<SORGENTE NOMESORG=\"STATISTICHE\" IDSERVIZIO=\""+idServizioA+"\" TEMPOINT=\"141\" PREZZABILE=\"0\""
		+" ERRORE=\"1\">"
		+"								<soap:Envelope  xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
		+"									<soap:Body>"
		+"										<STATISTICServicesResponse xmlns=\"http://tempuri.org/WsStat/Service1\">"
		+"											<STATISTICServicesResult>"
		+                          getBustaCertificato(certificato,true)
		+"											</STATISTICServicesResult>"
		+"										</STATISTICServicesResponse>"
		+"									</soap:Body>"
		+"								</soap:Envelope>"
		+"							</SORGENTE>"
		+"							<SORGENTE NOMESORG=\""+codSorgente+"\" IDSERVIZIO=\""+idServizioB+"\" TEMPOINT=\"469\" PREZZABILE=\"0\""
		+" ERRORE=\"0\">"
		+"								<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">"
		+"									<S:Body>"
		+                 getBodyRispostaIntermedia( serviceCode, code, certificato, xmlIntermedio,sysdate )
		+"									</S:Body>"
		+"								</S:Envelope>"
		+"							</SORGENTE>"
		+"							<DATIRICHIESTA>"
		+                          getBustaCertificato(certificato,false)
		+"								<PROGRESSIVO>0000</PROGRESSIVO>"
		+"							</DATIRICHIESTA>"
		+"							<IDSESSIONE/>"
		+"						</DATICERT>"
		
		+   foglioStile
		
		
		+"						<ERRORE IDERRORE=\"\" DESCRIZIONE=\"\" />"
		+                   getBustaElencoCriticitaBytypte(certificato)
		+"					</ROOT>"
		+"		</CORPO>"
		+"</CORPO>";
		
		outputXml = " <GetResponseResponse xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
						"xmlns=\"http://tempuri.org/WS_SDPGateway/Service1\">"+
					"  <CORPO> "+
					"<![CDATA["+hhh+
					"]]>"+
					"  </CORPO> "+
					" </GetResponseResponse> ";
		
		
		objLog.debug(outputXml);
		//qname = "{http://tempuri.org/WS_SDPGateway/Service1}GetResponseResponse";
		qname = "GetResponseResponse";
		utilJaxb.setPkgSchema(pkgSchema);
		utilJaxb.setQname(qname);
		resp = (GetResponseResponse)utilJaxb.unmarshal(new ByteArrayInputStream(outputXml.toString().getBytes("UTF-8")));
		//setNSResponse();
		objLog.info("END");
		return resp;
	}
	
	private Object getErrorResponse(String serviceCode,String code,String msg,String sysdate, 
			UtilJaxb utilJaxb,Intestazione intestazione,Certificato certificato
			,Long idIstanza,SmartExperianOutput out,Long idRichiesta) throws Exception {
		
		objLog.info("START");
		String outputXmlError = "";
	 	String pkgSchema = "com.cervedgroup.resmart.ws.resmartexperianintegration.support.xsd";
		String qname = "";
		GetResponseResponse resp = null;
		
		String codRich = certificato.getCodiceRichiesta();
		
		if(out!=null){
			ArrayList<ServiceDataOutput> list = out.getListServiceDataOutput();
			
			if(list!=null && !list.isEmpty()){
				ServiceDataOutput ser = list.get(0);
				if(ser!=null){
					String idRich = ser.getIdRichiesta();
					Long idRichL = Long.valueOf(idRich);
					
					codRich = dao.selectCodRichClie(idRichL) ;
				}
			}
		}
		
		
		String codSorgente = "";
		String idCertificato= "";
		String idServizioA ="82";
		String idServizioB ="238";

		String foglioStile = "";

		String pathFile = Env.getHome() + "/dat/RESMART/RESMARTEXPERIANINTEGRATION/1/xsl/FOGLIO_DI_STILE.xsl";
		
		String encodedFoglioStile = new String(Base64.encodeBase64(read(pathFile)));
		
		
		//objLog.info(encodedFoglioStile);
		
		if(serviceCode.equalsIgnoreCase("S040000009")){
			codSorgente = "SMART";
			idCertificato= "86";
			
			if(!certificato.getTipoOutput().equalsIgnoreCase("xml"))
				foglioStile ="						<FOGLIOSTILE xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
				+ encodedFoglioStile
				+"						</FOGLIOSTILE>";
			else
				foglioStile = "<FOGLIOSTILE />";
			
			//TODO verificando alcuni casi di esempio ho notato
			// che se il tipo soggetto è giuridico allora
			// l'idservizio della seconda busta SORGENTE vale 240
			// ATTENZIONE: essendo una deduzione fatta da una analisi 
			// di alcuni esempi, e non da specifiche fornite dal cliente
			// la cosa potrebbe non essere vera
			if(certificato.getTipoSoggetto()!=null && certificato.getTipoSoggetto().equalsIgnoreCase("G"))
				idServizioB ="240";
		}
		
		if(serviceCode.equalsIgnoreCase("S110000000")){
			codSorgente = "DATI CAT SOAR";
			idCertificato= "93";
			foglioStile = "<FOGLIOSTILE />";
			idServizioB ="248";
		}
		
		String hhh ="<CORPO>"
			+"				<CORPO>"
			+"					<ROOT DATAISTANZA=\""+sysdate+"\" ERRORE=\""+code+"\" IDRICHIESTA=\""+idRichiesta+"\""
			+" PREZZABILE=\"1\" CRITICO=\"0\" CODICERICHIESTA=\""+codRich+"\" EVASA=\"0\">"
			+"						<DATICERT IDISTANZA=\""+idIstanza+"\" DATASTART=\""+sysdate+"\""
			+" DATAEND=\""+sysdate+"\" IDCERTIFICATO=\""+idCertificato+"\">"
			+"							<SORGENTE NOMESORG=\"STATISTICHE\" IDSERVIZIO=\""+idServizioA+"\" TEMPOINT=\"141\" PREZZABILE=\"0\""
			+" ERRORE=\""+code+"\">"
			+"								<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
			+"									<soap:Body>"
			+"										<STATISTICServicesResponse xmlns=\"http://tempuri.org/WsStat/Service1\">"
			+"											<STATISTICServicesResult>"
			+                          getBustaCertificato(certificato,true)
			+"											</STATISTICServicesResult>"
			+"										</STATISTICServicesResponse>"
			+"									</soap:Body>"
			+"								</soap:Envelope>"
			+"							</SORGENTE>"
			+"							<SORGENTE NOMESORG=\""+codSorgente+"\" IDSERVIZIO=\""+idServizioB+"\" TEMPOINT=\"469\" PREZZABILE=\"0\""
			+" ERRORE=\"0\">"
			+"								<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+"									<S:Body>"
			+getBodyRispostaErrore(serviceCode, code, certificato, sysdate)
			+"									</S:Body>"
			+"								</S:Envelope>"
			+"							</SORGENTE>"
			+"							<DATIRICHIESTA>"
			+                          getBustaCertificato(certificato,false)
			+"								<PROGRESSIVO>0000</PROGRESSIVO>"
			+"							</DATIRICHIESTA>"
			+"							<IDSESSIONE/>"
			+"						</DATICERT>"
			+    foglioStile
			+"						<ERRORE IDERRORE=\""+code+"\" DESCRIZIONE=\""+msg+"\" />"
			+                   getBustaElencoCriticitaBytypte(certificato)
			+"					</ROOT>"
			+"		</CORPO>"
			+"</CORPO>";
			
			outputXmlError = " <GetResponseResponse xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
							"xmlns=\"http://tempuri.org/WS_SDPGateway/Service1\">"+
						"  <CORPO> "+
						
						"<![CDATA["+hhh+
						"]]>"+
						
						"  </CORPO> "+
						" </GetResponseResponse> ";
		
		objLog.debug(outputXmlError);
			
		qname = "GetResponseResponse";
		
		utilJaxb.setPkgSchema(pkgSchema);
		utilJaxb.setQname(qname);
		resp = (GetResponseResponse)utilJaxb.unmarshal(new ByteArrayInputStream(outputXmlError.toString().getBytes("UTF-8")));
		objLog.info("END");
				
		
		return resp;
	}
	
	private Object getEvasaResponse(String serviceCode,String code,String msg,
			String sysdate, 
			Intestazione intestazione,Certificato certificato,
			SmartExperianOutput out,Long idIstanza,Long idRichiestaSeq) throws Exception {
		
		objLog.info("START");
		
		
		ArrayList<ServiceDataOutput> list = out.getListServiceDataOutput();
		
		ServiceDataOutput ser = list.get(0);
		
		String codSorgente = "";
		String idCertificato= "";
		String idServizioA ="82";
		String idServizioB ="";
		String foglioStile = "";

		String pathFile = Env.getHome() + "/dat/RESMART/RESMARTEXPERIANINTEGRATION/1/xsl/FOGLIO_DI_STILE.xsl";
		
		String encodedFoglioStile = new String(Base64.encodeBase64(read(pathFile)));
		
		if(serviceCode.equalsIgnoreCase("S040000009")){
			codSorgente = "SMART";
			idCertificato= "86";
			if(!certificato.getTipoOutput().equalsIgnoreCase("xml"))
				foglioStile ="						<FOGLIOSTILE xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
					+ encodedFoglioStile
					+"</FOGLIOSTILE>";
			else
				foglioStile = "<FOGLIOSTILE />";
			
			
			idServizioB ="238";
			
			//TODO verificando alcuni casi di esempio ho notato
			// che se il tipo soggetto è giuridico allora
			// l'idservizio della seconda busta SORGENTE vale 240
			// ATTENZIONE: essendo una deduzione fatta da una analisi 
			// di alcuni esempi, e non da specifiche fornite dal cliente
			// la cosa potrebbe non essere vera
			if(certificato.getTipoSoggetto()!=null && certificato.getTipoSoggetto().equalsIgnoreCase("G"))
				idServizioB ="240";
		}
		
		if(serviceCode.equalsIgnoreCase("S110000000")){
			codSorgente = "DATI CAT SOAR";
			idCertificato= "93";
			foglioStile = "<FOGLIOSTILE />";
			idServizioB ="248";
		}
		
		String idRich = ser.getIdRichiesta();
		Long idRichL = Long.valueOf(idRich);
		
		String codRich = dao.selectCodRichClie(idRichL) ;
		
		
		String hhh ="         <CORPO>"
		+"				<CORPO>"
		+"					<ROOT DATAISTANZA=\""+sysdate+"\" ERRORE=\"0\" IDRICHIESTA=\""+idRichiestaSeq+"\""
		+" PREZZABILE=\"1\" CRITICO=\"0\" CODICERICHIESTA=\""+codRich+"\" EVASA=\"0\">"
		+"						<DATICERT IDISTANZA=\""+idIstanza+"\" DATASTART=\""+sysdate+"\""
		+" DATAEND=\""+sysdate+"\" IDCERTIFICATO=\""+idCertificato+"\">"
		+"							<SORGENTE NOMESORG=\"STATISTICHE\" IDSERVIZIO=\""+idServizioA+"\" TEMPOINT=\"141\" PREZZABILE=\"0\""
		+" ERRORE=\"1\">"
		+"								<soap:Envelope  xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
		+"									<soap:Body>"
		+"										<STATISTICServicesResponse xmlns=\"http://tempuri.org/WsStat/Service1\">"
		+"											<STATISTICServicesResult>"
		+                          getBustaCertificato(certificato,true)
		+"											</STATISTICServicesResult>"
		+"										</STATISTICServicesResponse>"
		+"									</soap:Body>"
		+"								</soap:Envelope>"
		+"							</SORGENTE>"
		+"							<SORGENTE NOMESORG=\""+codSorgente+"\" IDSERVIZIO=\""+idServizioB+"\" TEMPOINT=\"469\" PREZZABILE=\"0\""
		+" ERRORE=\"0\">"
		+"								<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">"
		+"									<S:Body>"
		+             getBodyEvasa(serviceCode, code, certificato, out ,sysdate)
		+"									</S:Body>"
		+"								</S:Envelope>"
		+"							</SORGENTE>"
		+"							<DATIRICHIESTA>"
		+                          getBustaCertificato(certificato,false)
		+"								<PROGRESSIVO>0000</PROGRESSIVO>"
		+"							</DATIRICHIESTA>"
		+"							<IDSESSIONE/>"
		+"						</DATICERT>"
		
		+  foglioStile
		
		+"						<ERRORE IDERRORE=\"\" DESCRIZIONE=\"\" />"
		+                   getBustaElencoCriticitaBytypte(certificato)
		+"					</ROOT>"
		+"		</CORPO>"
		+"</CORPO>";
		
		objLog.debug(hhh);
		
		GetResponseResponse response = new GetResponseResponse();
		
		CORPO value = new CORPO();
		value.getContent().add("<![CDATA["+hhh+"]]>");//soapui aggiunge di default il CDATA quindi cosi' ce ne sono 2 innestati verificare
		//value.getContent().add(hhh);
		
		response.setCORPO(value);
		objLog.info("setCORPO");

		objLog.info("END");
		return response;
	}
	
	/**
	 * Controllo parametri.
	 * @throws Exception Exception
	 */
	private String[] dataBindigCheckInput(Intestazione intestazione,Certificato certificato,GetResponse request) throws Exception {
		String meth = "checkParam";
		long startEsec = System.currentTimeMillis();
		long endEsec = 0;
		objLog.info("START");
		
		String[] out = new String[2];
		//Intestazione intestazione = new Intestazione();
		
		ArrayList<String> xPathList = new ArrayList<String>();
		ArrayList<String> datiList = null;
		
	    String xPathCodiceId = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/SERVIZI/CODICE/@ID";
		String xPathIdProdotto = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/IDPRODOTTO";
		String xPathTipoOutput = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/TIPOOUTPUT";
		String xPathUserId = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/USERID";
		String xPathPassword = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/PASSWORD";
		String xPathIpCliente = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/IPCLIENTE";
		String xPathAmbienteIntestazione = "/GetResponse/Request/RICHIESTA/INTESTAZIONE/AMBIENTE";
		
		xPathList.add(xPathCodiceId);
		xPathList.add(xPathIdProdotto);
		xPathList.add(xPathTipoOutput);
		xPathList.add(xPathUserId);
		xPathList.add(xPathPassword);
		xPathList.add(xPathIpCliente);
		xPathList.add(xPathAmbienteIntestazione);
		
		String requestXml = util.getStringFromObject(request);
		
		objLog.trace("request="+requestXml);
		datiList = util.getValueFromXmlByXPath(requestXml, xPathList);
		objLog.info("intestazione="+datiList);
		
		intestazione.setCodiceId(datiList.get(0));
		intestazione.setIdProdotto(datiList.get(1));
		intestazione.setTipoOutput(datiList.get(2));
		intestazione.setUserId(datiList.get(3));
		intestazione.setPassword(datiList.get(4));
		intestazione.setIpCliente(datiList.get(5));
		intestazione.setAmbiente(datiList.get(6));
		
		//Certificato certificato = new Certificato();
		xPathList.clear();
		datiList.clear();
		
		
		String xPathTipoCertificato ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/TIPOCERTIFICATO";
		String xpathTipoOutput ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/TIPOOUTPUT";
		String xpathTipoPersona ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/TIPOPERSONA";
		String xpathTipoServizioRichiesta = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/TIPOSERVIZIORICHIESTA";
		String xpathTipoVisuraRichiesta = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/TIPOVISURARICHIESTA";
		String xpathDataAggiornamentoVisura = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/DATAAGGIORNAMENTOVISURA";
		String xpathGravami = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/GRAVAMI";
		String xpathUrgenza = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/URGENZA";
		String xpathTipoSoggetto = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/TIPOSOGGETTO";
		String xpathCodiceRichiesta = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/CODICERICHIESTA";
		String xpathDenominazione = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/DENOMINAZIONE";
		String xpathPartitaIva = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/PARTITAIVA";
		String xpathFormaGiuridicaEfx = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/FORMAGIURIDICAEFX";
		String xpathDataDiCostituzione = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/DATADICOSTITUZIONE";
		String xpathProvinciaCciaa = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/PROVINCIACCIAA";
		String xPathNumeroRea ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/NUMEROREA";
		String xpathNome = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/NOME";
		String xpathCognome = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/COGNOME";
		String xPathCodiceFiscale ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/CODICEFISCALE";
		String xpathDataNascita = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/DATANASCITA";
		String xpathComuneNascita = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/COMUNENASCITA";
		String xpathProvinciaNascita = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/PROVINCIANASCITA";
		String xpathSesso = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/SESSO";
		String xpathComune = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/COMUNE";
		String xpathProvincia = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/PROVINCIA";
		String xpathIndirizzo = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/INDIRIZZO";
		String xpathCodiceRichiestaXlocator = "/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/CODRICHIESTA";
		
		
		
		xPathList.add( xPathTipoCertificato );
		xPathList.add( xpathTipoOutput );
		xPathList.add( xpathTipoPersona );
		xPathList.add( xpathTipoServizioRichiesta ); 
		xPathList.add( xpathTipoVisuraRichiesta ); 
		xPathList.add( xpathDataAggiornamentoVisura ); 
		xPathList.add( xpathGravami ); 
		xPathList.add( xpathUrgenza ); 
		xPathList.add( xpathTipoSoggetto ); 
		xPathList.add( xpathCodiceRichiesta ); 
		xPathList.add( xpathDenominazione ); 
		xPathList.add( xpathPartitaIva ); 
		xPathList.add( xpathFormaGiuridicaEfx ); 
		xPathList.add( xpathDataDiCostituzione ); 
		xPathList.add( xpathProvinciaCciaa ); 
		xPathList.add( xPathNumeroRea );
		xPathList.add( xpathNome ); 
		xPathList.add( xpathCognome ); 
		xPathList.add( xPathCodiceFiscale );
		xPathList.add( xpathDataNascita ); 
		xPathList.add( xpathComuneNascita ); 
		xPathList.add( xpathProvinciaNascita ); 
		xPathList.add( xpathSesso ); 
		xPathList.add( xpathComune ); 
		xPathList.add( xpathProvincia ); 
		xPathList.add( xpathIndirizzo ); 
		xPathList.add( xpathCodiceRichiestaXlocator ); 
		
		datiList = util.getValueFromXmlByXPath(requestXml, xPathList);
		objLog.info("certificato="+datiList);
		
		certificato.setTipoCertificato(datiList.get(0)) ;
		certificato.setTipoOutput(datiList.get(1)) ;
		certificato.setTipoPersona(datiList.get(2));
		certificato.setTipoServizioRichiesta(datiList.get(3)) ;
		certificato.setTipoVisuraRichiesta(datiList.get(4)) ;
		certificato.setDataAggiornamentoVisura(datiList.get(5)) ;
		certificato.setGravami(datiList.get(6)) ;
		certificato.setUrgenza(datiList.get(7)) ;
		certificato.setTipoSoggetto(datiList.get(8)) ;
		certificato.setCodiceRichiesta(datiList.get(9)) ;
		certificato.setDenominazione(datiList.get(10)) ;
		certificato.setPartitaIva(datiList.get(11)) ;
		certificato.setFormaGiuridicaEfx(datiList.get(12)) ;
		certificato.setDataDiCostituzione(datiList.get(13)) ;
		certificato.setProvinciaCciaa(datiList.get(14)) ;		
		certificato.setNumeroRea(datiList.get(15)) ;		
		certificato.setNome(datiList.get(16)) ;
		certificato.setCognome(datiList.get(17)) ;		
		certificato.setCodiceFiscale(datiList.get(18)) ;
		certificato.setDataNascita(datiList.get(19));
		
		certificato.setComuneNascita(datiList.get(20)) ;
		certificato.setProvinciaNascita(datiList.get(21)) ;
		
		certificato.setSesso(datiList.get(22)) ;
		certificato.setComune(datiList.get(23)) ;
		certificato.setProvincia(datiList.get(24)) ;
		
		
		certificato.setIndirizzo(datiList.get(25)) ;
		
		String codRichLocator = datiList.get(26);
		
		if(intestazione.getCodiceId().equalsIgnoreCase("S110000000"))
			certificato.setCodiceRichiesta(codRichLocator) ;
		
		certificato.setServiceCode(intestazione.getCodiceId());
		out[0]=intestazione.getCodiceId();
		
		out[1]="";
		
		objLog.info("CodiceId="+intestazione.getCodiceId());
		objLog.info("TipoCertificato="+certificato.getTipoCertificato());
		
		xPathList.clear();
		datiList.clear();
		/*
		<CONSERVATORIACOMUNE1>XXXXX</CONSERVATORIACOMUNE1>"
		<CONSERVATORIAPROVINCIA1>XXXX</CONSERVATORIAPROVINCIA1>"
		*/
		String xPathConservatoriaComune ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/CONSERVATORIACOMUNE";
		String xpathConservatoriaProvincia ="/GetResponse/Request/RICHIESTA/CORPO/"+intestazione.getCodiceId()+"/CERTIFICATO/CONSERVATORIAPROVINCIA";

		ArrayList<ConservatoriaRichiesta> listCons = new ArrayList<ConservatoriaRichiesta>();
		
		recuperaDatiCons: 
			for(int i = 1; i<=10;i++){
			
			xPathList.clear();
			datiList.clear();
			
			
			xPathList.add(xPathConservatoriaComune+i);
			xPathList.add(xpathConservatoriaProvincia+i);
			
			ArrayList<String> datiListCons = util.getValueFromXmlByXPath(requestXml, xPathList);
			
			
			if(!datiListCons.isEmpty()){

				String comu = datiListCons.get(0);
				String prov = datiListCons.get(1);

				if(comu!=null && !comu.equals("")
						||(prov!=null && !prov.equals(""))	){

					ConservatoriaRichiesta elem = new ConservatoriaRichiesta();

					elem.setConservatoriaComune(comu);
					elem.setConservatoriaProvincia(prov);

					listCons.add(elem);
				}
			}else
				break recuperaDatiCons;
			
		}
		objLog.info("listCons.size="+listCons.size());
		certificato.setElencoConservatoria(listCons);
		
		
		
		objLog.info("GetResponse="+util.getStringFromObject(request));
		endEsec = (System.currentTimeMillis() - startEsec);
	 	objLog.info("#"+CLASSNAME+"."+meth+"#"+endEsec+"#");
		objLog.info("END");	
		return out;	
	}
	
	
	private String getBodyEvasa(String serviceCode,String code,Certificato certificato,
			SmartExperianOutput in,String sysdate ) throws Exception {
		
		
		String out ="";
		//2010-06-30+02:00
		//yyyyMMddHHmmss
		
		String xmlVisura = "";
		
		String data = sysdate.substring(0,4)+"-"+sysdate.substring(4,6)+"-"+sysdate.substring(6,8);
		String ora = sysdate.substring(8,10)+":"+sysdate.substring(10,12);
		
		String tipoOutput = certificato.getTipoOutput();
		
		if(tipoOutput == null)
			tipoOutput = "file";
		
		if(serviceCode.equalsIgnoreCase("S040000009")){

			ArrayList<ServiceDataOutput> list = in.getListServiceDataOutput();
			xmlVisura = "<ns6:visure>";
			for (ServiceDataOutput serviceDataOutput : list) {

				if(tipoOutput!= null && tipoOutput.equalsIgnoreCase("both") ){

					//in tal caso è necessario inserire dentro l'xml
					// il byte stream nel tag ns6:fileVisura
					/*
					 * <ns6:listaVisure>
							<ns6:fileVisura/>
							<ns6:xmlVisura>
								<ns7:reportImmobiliareSmartPlus logo="N" />
							</ns6:xmlVisura>
							<ns6:conservatoria>
								<ns4:codiceConservatoria>FI</ns4:codiceConservatoria>
							</ns6:conservatoria>
							<ns6:statoVisura>0</ns6:statoVisura>
						</ns6:listaVisure>
					 */

					if(serviceDataOutput.getOutPdf()!=null)
						try {
							if(serviceDataOutput.getOutXml()==null || serviceDataOutput.getOutXml().trim().equalsIgnoreCase("")){
								objLog.error("XML restituito per la richiesta "+serviceDataOutput.getIdRichiesta()+" e' vuoto. Ci sono stati dei problemi, si manda in errore la richiesta ");
								throw new Exception("XML restituito per la richiesta "+serviceDataOutput.getIdRichiesta()+" e' vuoto. Ci sono stati dei problemi, si manda in errore la richiesta ");
							}
								
							// SWLIVISSMA-142 - gestione encoding - inizio
							// leggo a db evntuale encoding: se presente lo utilizzo, altrimenti no (funzionamento precedente)
							ByteArrayInputStream bis = null;
							String encoding = "";
							try {
								objLog.info("Lettura chiave [" + Constant.CHIAVE_SMARTEXPERIANOUTPUT_CHARSET + "] dalla tabella WK_PARAMETRI_ELAB");
								WkParametriElabRec rec = new WkParametriElabRec();
								rec.setNomeProcesso(Constant.PROCESSO_SMARTEXPERIANINTEGRATION);
								rec.setChiaveProcesso(Constant.CHIAVE_SMARTEXPERIANOUTPUT_CHARSET);
								encoding = dao.selectWkParametriElab(rec);
								objLog.info("Lettura effettuata con successo. Encoding [" + encoding + "]");
							}
							catch (Throwable t) {
								objLog.warn("Errore nella lettura della chiave " + Constant.CHIAVE_SMARTEXPERIANOUTPUT_CHARSET + ". EX: " + t.getMessage() + ". Si procede con encoding di default");
							}	
							if (encoding != null && !encoding.equalsIgnoreCase("")) {
								objLog.info("Encoding utilizzato: [" + encoding + "]");
								bis = new ByteArrayInputStream(serviceDataOutput.getOutXml().getBytes(encoding));
							}
							else {
								objLog.info("Non utilizzato alcun encoding");
								bis = new ByteArrayInputStream(serviceDataOutput.getOutXml().getBytes());	
							}
							// SWLIVISSMA-142 - gestione encoding - fine

							InputSource is = new InputSource(bis);
							objLog.info("is.getEncoding(): [" + is.getEncoding() + "]");
							is.setEncoding(encoding);
							objLog.info("is.getEncoding() 2: [" + is.getEncoding() + "]");
							
							objLog.info("1");
							org.jsoup.nodes.Document docSoup = Jsoup.parse(serviceDataOutput.getOutXml(), encoding, Parser.xmlParser());
							objLog.info("2");
							
							//Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

//							String enco = doc.getInputEncoding();
//							objLog.info("enco: [" + enco + "]");
							
							//Node employee = doc.getElementsByTagName("ns6:fileVisura").item(0);
							org.jsoup.nodes.Element employeeSoup = docSoup.getElementsByTag("ns6:fileVisura").get(0);
							objLog.info("3");
							String encodedPdf = new String(Base64.encodeBase64(serviceDataOutput.getOutPdf()));
							objLog.info("4");
							//if(employee!=null)
							//	employee.setTextContent(encodedPdf);
							
							employeeSoup.text(encodedPdf);
							objLog.info("5");
							//String xmlModif = XMLUtil.dom2String(doc, enco);
							
							String xmlModif = docSoup.toString();
							objLog.info("6");
							// pulisco l'xml modificato da eventuale tag iniziale
							
							xmlModif = clearXmlDeclaration(xmlModif);
							
							xmlVisura = xmlVisura + xmlModif;

						} catch (SAXException e) {
							objLog.error("SAXException: EX: "+e.getMessage());
							throw new Exception(e.getMessage());
						} catch (IOException e) {
							objLog.error("IOException: EX: "+e.getMessage());
							throw new Exception(e.getMessage());
						} catch (ParserConfigurationException e) {
							objLog.error("ParserConfigurationException: EX: "+e.getMessage());
							throw new Exception(e.getMessage());
						}

				}
				
				if( tipoOutput!= null && tipoOutput.equalsIgnoreCase("file")){
					String encodedPdf = new String(Base64.encodeBase64(serviceDataOutput.getOutPdf()));
					
					String xmlTemp = "<ns6:listaVisure>"+
					"<ns6:fileVisura/>"+
					encodedPdf+
					"</ns6:fileVisura/>"+
					"<ns6:conservatoria>"+
						"<ns4:codiceConservatoria>"+serviceDataOutput.getCodiceConservatoria()+"</ns4:codiceConservatoria>"+
					"</ns6:conservatoria>"+
					"<ns6:statoVisura>0</ns6:statoVisura>"+
				    "</ns6:listaVisure>";
					
					xmlVisura = xmlVisura + xmlTemp;
				}
				
                if( certificato.getTipoOutput()!= null &&  certificato.getTipoOutput().equalsIgnoreCase("xml")){
					xmlVisura = xmlVisura + serviceDataOutput.getOutXml();
				}
			}
			xmlVisura = xmlVisura +"</ns6:visure>";
			
			out = "										<ns5:getIspezioneImmobiliareResponse "
				+" xmlns:ns2=\"http://bi.cerved.com/XMLSchema/AnagraficaSoggetto\""
				+" xmlns:ns3=\"http://bi.cerved.com/XMLSchema/GenericType\""
				+" xmlns:ns4=\"http://bi.cerved.com/XMLSchema/Conservatorie\" " 
				+" xmlns:ns5=\"http://impl.ws.cerved.com/\""
				+" xmlns:ns6=\"http://bi.cerved.com/XMLSchema/Visura\""
				+" xmlns:ns7=\"http://bi.cerved.com/XMLSchema/VisureSmartPlus\""
				+" xmlns:ns8=\"http://bi.cerved.com/XMLSchema/VisureSmart\""
				+" xmlns:ns9=\"http://bi.cerved.com/XMLSchema/VisureFull\">"
				+"											<header>"
				+"												<requestKey>"+certificato.getCodiceRichiesta()+"</requestKey>"
				+"												<state>"+code+"</state>"
				+"											</header>"
				+"											<body>"
				+"											"+xmlVisura
				+"											</body>"
				+"										</ns5:getIspezioneImmobiliareResponse>";

		}
		
		if(serviceCode.equalsIgnoreCase("S110000000")){
			
			Date date = new Date();
	        TimeZone localTimezone = TimeZone.getDefault();
	        int hour=localTimezone.getRawOffset()/(3600000);
	        
		    
	        if(localTimezone.inDaylightTime(date))
	        	hour = hour+1;
			
			ArrayList<ServiceDataOutput> list = in.getListServiceDataOutput();
			
			ServiceDataOutput serviceDataOutput = list.get(0);
			
			xmlVisura = xmlVisura + serviceDataOutput.getOutXml();
			
			String pdfVisura = null;
			if (serviceDataOutput.getOutPdf()!=null) {
				String encodedPdf = new String(Base64.encodeBase64(serviceDataOutput.getOutPdf()));
				pdfVisura = "<fileVisura>" + encodedPdf + "</fileVisura>";
			}
			
			out = "<ns2:getIspezioneCatastaleResponse xmlns:ns2=\"http://impl.ws.cerved.com/\">"
				+"<header>"
			       +"<requestKey>"+certificato.getCodiceRichiesta()+"</requestKey>"
				   +"<dataElabReport>"+data+"+"+hour+":00</dataElabReport>"
				   +"<state details=\"99\">PROCESSED</state>"
				+"</header>"
				+"<body>"
				+xmlVisura
				+(pdfVisura != null ? pdfVisura : "")
			    +"</body>"
	          + "</ns2:getIspezioneCatastaleResponse>";
			}
		
		return out;
	}
	
	private String getBodyRispostaIntermedia(String serviceCode,String code,
			Certificato certificato,String xmlVisura,String sysdate) {
		
		String out ="";

		String data = sysdate.substring(0,4)+"-"+sysdate.substring(4,6)+"-"+sysdate.substring(6,8);
		String ora = sysdate.substring(8,10)+"-"+sysdate.substring(10,12);
		
		
		
		
		
		if(serviceCode.equalsIgnoreCase("S040000009")){
		
			 out = "										<ns5:getIspezioneImmobiliareResponse "
					+" xmlns:ns2=\"http://bi.cerved.com/XMLSchema/AnagraficaSoggetto\""
					+" xmlns:ns3=\"http://bi.cerved.com/XMLSchema/GenericType\""
					+" xmlns:ns4=\"http://bi.cerved.com/XMLSchema/Conservatorie\" " 
					+" xmlns:ns5=\"http://impl.ws.cerved.com/\""
					+" xmlns:ns6=\"http://bi.cerved.com/XMLSchema/Visura\""
					+" xmlns:ns7=\"http://bi.cerved.com/XMLSchema/VisureSmartPlus\""
					+" xmlns:ns8=\"http://bi.cerved.com/XMLSchema/VisureSmart\""
					+" xmlns:ns9=\"http://bi.cerved.com/XMLSchema/VisureFull\">"
					+"											<header>"
					+"												<requestKey>"+certificato.getCodiceRichiesta()+"</requestKey>"
					+"												<state>"+code+"</state>"
					+"											</header>"
					+"											<body>"
					+"											"+xmlVisura
					+"											</body>"
					+"										</ns5:getIspezioneImmobiliareResponse>";
		
		}
		
		if(serviceCode.equalsIgnoreCase("S110000000")){
	        Date date = new Date();
	        TimeZone localTimezone = TimeZone.getDefault();
	        int hour=localTimezone.getRawOffset()/(3600000);
	        
		    
	        if(localTimezone.inDaylightTime(date))
	        	hour = hour+1;
			
			out = "<ns2:getIspezioneCatastaleResponse xmlns:ns2=\"http://impl.ws.cerved.com/\">"
				+"<header>"
			       +"<requestKey>"+certificato.getCodiceRichiesta()+"</requestKey>"
				   +"<dataElabReport>"+data+"+"+hour+":00</dataElabReport>"
				   +"<state details=\""+code+"\">IN_PROGRESS</state>"
				+"</header>"
	          + "</ns2:getIspezioneCatastaleResponse>";
		}
		
		return out;
	}	

	private String getBodyRispostaErrore(String serviceCode,String code,
			Certificato certificato,String sysdate) {
		
		String out ="";

		String data = sysdate.substring(0,4)+"-"+sysdate.substring(4,6)+"-"+sysdate.substring(6,8);
		String ora = sysdate.substring(8,10)+"-"+sysdate.substring(10,12);
		
		
		
		
		
		if(serviceCode.equalsIgnoreCase("S040000009")){
		
			 out = "										<ns5:getIspezioneImmobiliareResponse "
					+" xmlns:ns2=\"http://bi.cerved.com/XMLSchema/AnagraficaSoggetto\""
					+" xmlns:ns3=\"http://bi.cerved.com/XMLSchema/GenericType\""
					+" xmlns:ns4=\"http://bi.cerved.com/XMLSchema/Conservatorie\" " 
					+" xmlns:ns5=\"http://impl.ws.cerved.com/\""
					+" xmlns:ns6=\"http://bi.cerved.com/XMLSchema/Visura\""
					+" xmlns:ns7=\"http://bi.cerved.com/XMLSchema/VisureSmartPlus\""
					+" xmlns:ns8=\"http://bi.cerved.com/XMLSchema/VisureSmart\""
					+" xmlns:ns9=\"http://bi.cerved.com/XMLSchema/VisureFull\">"
					+"											<header>"
					+"												<requestKey>"+certificato.getCodiceRichiesta()+"</requestKey>"
					+"												<state>"+code+"</state>"
					+"											</header>"
					+"										</ns5:getIspezioneImmobiliareResponse>";
		
		}
		
		if(serviceCode.equalsIgnoreCase("S110000000")){
	        Date date = new Date();
	        TimeZone localTimezone = TimeZone.getDefault();
	        int hour=localTimezone.getRawOffset()/(3600000);
	        
		    
	        if(localTimezone.inDaylightTime(date))
	        	hour = hour+1;
			
			out = "<ns2:getIspezioneCatastaleResponse xmlns:ns2=\"http://impl.ws.cerved.com/\">"
				+"<header>"
			       +"<requestKey>"+certificato.getCodiceRichiesta()+"</requestKey>"
				   +"<dataElabReport>"+data+"+"+hour+":00</dataElabReport>"
				   +"<state details=\""+code+"\"></state>"
				+"</header>"
	          + "</ns2:getIspezioneCatastaleResponse>";
		}
		
		return out;
	}

	
	private void destroy() throws Exception {
		String meth = "destroy";
		long startEsec = System.currentTimeMillis();
		objLog.info("START");
		long endEsec = (System.currentTimeMillis() - startEsec);
	 	objLog.info("#"+CLASSNAME+"."+meth+"#"+endEsec+"#");
		objLog.info("END");
	}


	private String getBustaCertificato(Certificato certificato,boolean sorgentStatistiche){
		
		String out = "";
		
		if(certificato.getServiceCode().equalsIgnoreCase("S040000009")){
			out = 
				(!sorgentStatistiche?"								<CERTIFICATO>"
						:"												<CERTIFICATO xmlns=\"\">")
				
				+"													<TIPOCERTIFICATO>"+certificato.getTipoCertificato()+"</TIPOCERTIFICATO>"
				+"													<TIPOOUTPUT>"+certificato.getTipoOutput()+"</TIPOOUTPUT>"
				+"													<TIPOSERVIZIORICHIESTA>"+certificato.getTipoServizioRichiesta()+"</TIPOSERVIZIORICHIESTA>"
				+"													<TIPOVISURARICHIESTA>"+certificato.getTipoVisuraRichiesta()+"</TIPOVISURARICHIESTA>"
				+((certificato.getDataAggiornamentoVisura()!=null && !certificato.getDataAggiornamentoVisura().equalsIgnoreCase(""))?
						"													<DATAAGGIORNAMENTOVISURA>"+certificato.getDataAggiornamentoVisura()+"</DATAAGGIORNAMENTOVISURA>":"")
				+"													<GRAVAMI>"+certificato.getGravami()+"</GRAVAMI>"
				+"													<URGENZA>"+certificato.getUrgenza()+"</URGENZA>"
				+"													<TIPOSOGGETTO>"+certificato.getTipoSoggetto()+"</TIPOSOGGETTO>"
				+"													<CODICERICHIESTA>"+certificato.getCodiceRichiesta()+"</CODICERICHIESTA>"
				+"													<COMUNE>"+filtraCaratToXml(certificato.getComune())+"</COMUNE>"
				+"													<PROVINCIA>"+certificato.getProvincia()+"</PROVINCIA>"
				+(certificato.getTipoSoggetto().equalsIgnoreCase("F")?"													<COMUNENASCITA>"+filtraCaratToXml(certificato.getComuneNascita())+"</COMUNENASCITA>":"")
				+(certificato.getTipoSoggetto().equalsIgnoreCase("F")?"													<PROVINCIANASCITA>"+certificato.getProvinciaNascita()+"</PROVINCIANASCITA>":"")
			    
				+(certificato.getTipoSoggetto().equalsIgnoreCase("G")?"											        <DENOMINAZIONE>"+filtraCaratToXml(certificato.getDenominazione())+"</DENOMINAZIONE>":"")
                +(certificato.getTipoSoggetto().equalsIgnoreCase("G")?"											        <PARTITAIVA>"+certificato.getPartitaIva()+"</PARTITAIVA>":"")
                +((certificato.getTipoSoggetto().equalsIgnoreCase("G") 
                		&& certificato.getDataDiCostituzione()!=null 
                		&& !certificato.getDataDiCostituzione().equalsIgnoreCase(""))?"											        <DATADICOSTITUZIONE>"+certificato.getDataDiCostituzione()+"</DATADICOSTITUZIONE>":"")
                +(certificato.getTipoSoggetto().equalsIgnoreCase("G")?"											        <FORMAGIURIDICAEFX>"+certificato.getFormaGiuridicaEfx()+"</FORMAGIURIDICAEFX>":"")
				+(certificato.getTipoSoggetto().equalsIgnoreCase("F")?"													<COGNOME>"+filtraCaratToXml(certificato.getCognome())+"</COGNOME>":"")
				+(certificato.getTipoSoggetto().equalsIgnoreCase("F")?"													<NOME>"+filtraCaratToXml(certificato.getNome())+"</NOME>":"")
				+(certificato.getTipoSoggetto().equalsIgnoreCase("F")?"													<CODICEFISCALE>"+certificato.getCodiceFiscale()+"</CODICEFISCALE>":"")
				+(certificato.getTipoSoggetto().equalsIgnoreCase("F")?"													<SESSO>"+certificato.getSesso()+"</SESSO>":"");

			ArrayList<ConservatoriaRichiesta> list = certificato.getElencoConservatoria();

			for (int i=0; i<list.size(); i++ ) {
				ConservatoriaRichiesta conservatoriaRichiesta = list.get(i);
				
				out = out + "													<CONSERVATORIACOMUNE"+(i+1)+">"
				+conservatoriaRichiesta.getConservatoriaComune()+"</CONSERVATORIACOMUNE"+(i+1)+">";

				out = out + "													<CONSERVATORIAPROVINCIA"+(i+1)+">"
				+conservatoriaRichiesta.getConservatoriaProvincia()+"</CONSERVATORIAPROVINCIA"+(i+1)+">";
			}
			if(sorgentStatistiche){
				out = out +"													<SCENARIO>0</SCENARIO>";
				out = out +"													<PROFILOUTENTEBIZ/>";
			}

			out = out +"								</CERTIFICATO>";

		
		}
		
		if(certificato.getServiceCode().equalsIgnoreCase("S110000000")){
			
			
			out = 
				(!sorgentStatistiche?"								<CERTIFICATO>"
						:"												<CERTIFICATO xmlns=\"\">")
				
				+"													<TIPOCERTIFICATO>"+certificato.getTipoCertificato()+"</TIPOCERTIFICATO>"
				+"													<TIPOPERSONA>"+certificato.getTipoPersona()+"</TIPOPERSONA>"
				+"													<CODRICHIESTA>"+certificato.getCodiceRichiesta()+"</CODRICHIESTA>"
				+"													<COMUNE>"+filtraCaratToXml(certificato.getComune())+"</COMUNE>"
				+"													<PROVINCIA>"+certificato.getProvincia()+"</PROVINCIA>"
				+(certificato.getTipoPersona().equalsIgnoreCase("F")?"													<COGNOME>"+filtraCaratToXml(certificato.getCognome())+"</COGNOME>":"")
				+(certificato.getTipoPersona().equalsIgnoreCase("F")?"													<NOME>"+filtraCaratToXml(certificato.getNome())+"</NOME>":"")
				+(certificato.getTipoPersona().equalsIgnoreCase("G")?"													<DENOMINAZIONE>"+filtraCaratToXml(certificato.getDenominazione())+"</DENOMINAZIONE>":"")
				+"													<CODICEFISCALE>"+certificato.getCodiceFiscale()+"</CODICEFISCALE>"
				+(certificato.getTipoPersona().equalsIgnoreCase("F")?"													<SESSO>"+certificato.getSesso()+"</SESSO>":"");
			if(sorgentStatistiche){
				out = out +"													<SCENARIO>0</SCENARIO>";
				out = out +"													<PROFILOUTENTEBIZ/>";
			}
			out = out +"								</CERTIFICATO>";
		}
		
		return out;
	}
	
	private String getBustaElencoCriticitaBytypte(Certificato certificato){
		String out ="";
		
		if(certificato.getServiceCode().equalsIgnoreCase("S040000009")){
			
			out ="						<ELENCOCRITICITA>"
				+"							<REGOLA LIVELLO=\"1\" CRITICA=\"0\" IDREGOLA=\"211\" VISIBILE=\"0\">"
				+"								<DESCRIZIONE>controllo msg</DESCRIZIONE>"
				+"								<ETICHETTA>controllo msg</ETICHETTA>"
				+"								<SOTTOREGOLA>"
				+"									<IDSOTTOREGOLA>296</IDSOTTOREGOLA>"
				+"									<DESCRIZIONETERMINI/>"
				+"									<DESCRIZIONE>controllo msg pf =''</DESCRIZIONE>"
				+"								</SOTTOREGOLA>"
				+"								<SOTTOREGOLA>"
				+"									<IDSOTTOREGOLA>297</IDSOTTOREGOLA>"
				+"									<DESCRIZIONETERMINI/>"
				+"									<DESCRIZIONE>controllo msg pg =''</DESCRIZIONE>"
				+"								</SOTTOREGOLA>"
				+"							</REGOLA>"
				+"						</ELENCOCRITICITA>";
		
		}
		
		if(certificato.getServiceCode().equalsIgnoreCase("S110000000")){
			out = "			<ELENCOCRITICITA>"
				+"				<REGOLA LIVELLO=\"2\" CRITICA=\"0\" IDREGOLA=\"214\" VISIBILE=\"0\">"
				+"					<DESCRIZIONE>CHECK PRESENZA DATI CATASTALI</DESCRIZIONE>"
				+"					<ETICHETTA>CHECK PRESENZA DATI CATASTALI</ETICHETTA>"
				+"					<SOTTOREGOLA>"
				+"						<IDSOTTOREGOLA>305</IDSOTTOREGOLA>"
				+"						<DESCRIZIONETERMINI />"
				+"						<DESCRIZIONE>CHECK PRESENZA DATI CATASTALI</DESCRIZIONE>"
				+"					</SOTTOREGOLA>"
				+"				</REGOLA>"
				+"			</ELENCOCRITICITA>";
		}
		
		return out;
	}
	
	private byte[] read(String pathFile) throws IOException {

	    File file = new File(pathFile);

	    byte []buffer = new byte[(int) file.length()];
	    InputStream ios = null;
	    try {
	        ios = new FileInputStream(file);
	        if ( ios.read(buffer) == -1 ) {
	            throw new IOException("EOF reached while trying to read the whole file");
	        }        
	    } finally { 
	        try {
	             if ( ios != null ) 
	                  ios.close();
	        } catch ( IOException e) {
	        }
	    }

	    return buffer;
	}
	/**
	 * @return the dao
	 */
	public IResmartWebDao getDao() {
		return dao;
	}
	/**
	 * @param dao the dao to set
	 */
	public void setDao(IResmartWebDao dao) {
		this.dao = dao;
	}
	
	public String clearXmlDeclaration(String s) throws Exception {	
		objLog.trace("START");
		String ret = new String(s);
		int i = -1;
		while ((i = ret.indexOf("<?xml")) != -1) {
			int j = ret.indexOf("?>");
			ret = ret.substring(0, i) + ret.substring(j + 2);
		}
		objLog.trace("END");
		return ret;
	}	
	private void archiviaEsito(String codEsito,String msg,
			Intestazione intestazione,Certificato certificato,
			SmartExperianOutput out,Long idRichiestaSeq){
		objLog.info("INIZIO");
		objLog.info("Archiviazione esito elaborazione");
		

		if(out!=null){
			String esitoComunicazione = "";
			String codErrore = out.getCodErrore();
			String codEsitoDb ="";
			String msgerr = null;
			
			if(codEsito!=null 
					&& ( codEsito.equalsIgnoreCase("1") 
							|| codEsito.equalsIgnoreCase("0"))){
				esitoComunicazione = "OK";
				codEsitoDb =codEsito;
				 msgerr = null;
			}else{
				esitoComunicazione = "KO";
				codEsitoDb =codErrore;
				 msgerr = out.getDescErrore();
			}

			ArrayList<ServiceDataOutput>  list =out.getListServiceDataOutput();
			objLog.info("Esito = "+esitoComunicazione);
			objLog.info("Cod Esito = "+codEsitoDb);
			
			if(list!=null && !list.isEmpty()){
				for (ServiceDataOutput serviceDataOutput : list) {
					
					JbSdpGetawayRec input = new JbSdpGetawayRec();

					input.setCodEsito(codEsitoDb);
					input.setEsito(esitoComunicazione);
					input.setCodRichiestaCliente(certificato.getCodiceRichiesta());
					input.setIdRichiestaResmart(Long.valueOf(serviceDataOutput.getIdRichiesta()));
					input.setIdRichiestaSdp(idRichiestaSeq);
					input.setCodUteIns(intestazione.getUserId());
					input.setNota(msgerr);
					dao.insertRecordJb(input);
				}

			}else{
				JbSdpGetawayRec input = new JbSdpGetawayRec();

				input.setCodEsito(codEsitoDb);
				input.setEsito(esitoComunicazione);
				input.setCodRichiestaCliente(certificato.getCodiceRichiesta());
				input.setIdRichiestaResmart(null);
				input.setIdRichiestaSdp(idRichiestaSeq);
				input.setCodUteIns(intestazione.getUserId());
				input.setNota(msgerr);
				dao.insertRecordJb(input);
			}
		}else{
			
			JbSdpGetawayRec input = new JbSdpGetawayRec();

			input.setCodEsito(codEsito);
			input.setEsito("KO");
			input.setCodRichiestaCliente(certificato.getCodiceRichiesta());
			input.setIdRichiestaResmart(null);
			input.setIdRichiestaSdp(idRichiestaSeq);
			input.setCodUteIns(intestazione.getUserId());
			input.setNota(msg);
			dao.insertRecordJb(input);
		}
		
		objLog.info("FINE");
	}
	public String removeXmlStringNamespaceAndPreamble(String xmlString) {
		  return xmlString.replaceAll("(<\\?[^<]*\\?>)?", ""). /* remove preamble */
		  replaceAll("xmlns.*?(\"|\').*?(\"|\')", "") /* remove xmlns declaration */
		  .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") /* remove opening tag prefix */
		  .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
	}

	
	private String filtraCaratToXml(String in) {
		
		if(in!=null){
			String escapedXml = StringEscapeUtils.escapeXml(in);
			objLog.info("prima: "+in);
			objLog.info("StringEscapeUtils.escapeXml() dopo: "+escapedXml);
			return escapedXml;
		}else
			return in;
		
	}

	
}

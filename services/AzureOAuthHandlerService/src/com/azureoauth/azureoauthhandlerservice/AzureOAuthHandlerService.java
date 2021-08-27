/*Copyright (c) 2020-2021 wavemaker.com All Rights Reserved.
 This software is the confidential and proprietary information of wavemaker.com You shall not disclose such Confidential Information and shall use it only in accordance
 with the terms of the source code license agreement you entered into with wavemaker.com*/
package com.azureoauth.azureoauthhandlerservice;

import java.io.InputStreamReader;
import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.wavemaker.runtime.service.annotations.ExposeToClient;

//import com.azureoauth.azureoauthhandlerservice.model.*;

/**
 * This is a singleton class with all its public methods exposed as REST APIs via generated controller class.
 * To avoid exposing an API for a particular public method, annotate it with @HideFromClient.
 *
 * Method names will play a major role in defining the Http Method for the generated APIs. For example, a method name
 * that starts with delete/remove, will make the API exposed as Http Method "DELETE".
 *
 * Method Parameters of type primitives (including java.lang.String) will be exposed as Query Parameters &
 * Complex Types/Objects will become part of the Request body in the generated API.
 *
 * NOTE: We do not recommend using method overloading on client exposed methods.
 */
@ExposeToClient
public class AzureOAuthHandlerService {

    private static final Logger logger = LoggerFactory.getLogger(AzureOAuthHandlerService.class);

    // @Autowired
    // private SecurityService securityService;

    /**
     * This is sample java operation that accepts an input from the caller and responds with "Hello".
     *
     * SecurityService that is Autowired will provide access to the security context of the caller. It has methods like isAuthenticated(),
     * getUserName() and getUserId() etc which returns the information based on the caller context.
     *
     * Methods in this class can declare HttpServletRequest, HttpServletResponse as input parameters to access the
     * caller's request/response objects respectively. These parameters will be injected when request is made (during API invocation).
     */
    @Autowired
    private ServletContext context;
    
    private static final String AUTH_REQUEST_URL = "https://login.microsoftonline.com/2fe69d87-d212-439a-a6d4-3dca3ffdcef9/oauth2/v2.0/authorize";
    private static final String TOKEN_REQUEST_URL = "https://login.microsoftonline.com/2fe69d87-d212-439a-a6d4-3dca3ffdcef9/oauth2/v2.0/token";
    private static final String GRANT_TYPE="authorization_code";
    private static final String STATE="csrf9999";
    private static final String SCOPE="openid";
    private static final String RESPONSE_TYPE="code";
    private static final String CALLBACK_CODE="code";
    private static final String CALLBACK_ACCESS_TOKEN = "access_token";
    private static final String RESPONSE_MODE = "query";
    private static final String CODE_CHALLENGE = "mVjNDE5YmEyZGRhOGYyM2IzNjdmZWFhMTYTFjNjI1OWYzMzA3MTI4ZDY2Njg5M2RkNQ1ODg3NDcxY2Nl";
    private static final String CODE_CHALLENGE_METHOD = "S256";
    
    private static final String ACCESS_TOKEN_NAME="azure.accesstoken";
    
    @Value("${Appid}")
    private String appid;
    
    @Value("${Secret}")
    private String secret;
    
    @Value("${Scope}")
    private String scope;
    
    @Value("${Page}")
    private String page;
    
    @Value("${AuthReqURL}")
    private String authReqURL;
    
    @Value("${TokenReqURL}")
    private String tokenReqURL;
    
    public String getAccessToken(HttpServletRequest request) throws ServletException {
 	    HttpSession session = request.getSession(false);
 	    String accessToken = (session != null) ? (String) session.getAttribute(ACCESS_TOKEN_NAME) : null;
 	    logger.info("Accestoken from getAccesToken: " + accessToken);
 	    return accessToken;
 	}
 	
 	public String getLoginURL(HttpServletRequest request,String state) {
        String oauthRedirectURL = authReqURL; //AUTH_REQUEST_URL;
        
        
        /*
        https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize?
client_id=6731de76-14a6-49ae-97bc-6eba6914391e
&response_type=code
&redirect_uri=http%3A%2F%2Flocalhost%2Fmyapp%2F
&response_mode=query
&scope=https%3A%2F%2Fgraph.microsoft.com%2Fmail.read%20api%3A%2F%2F
&state=12345
&code_challenge=mVjNDE5YmEyZGRhOGYyM2IzNjdmZWFhMTYTFjNjI1OWYzMzA3MTI4ZDY2Njg5M2RkNQ1ODg3NDcxY2Nl
&code_challenge_method=S256
        
        
        */
        
        
        oauthRedirectURL += "?client_id=" + appid;
        oauthRedirectURL += "&response_type=" + RESPONSE_TYPE;
        oauthRedirectURL += "&redirect_uri=" + getCallbackURL(request);
        oauthRedirectURL += "&state=" + state;
        oauthRedirectURL += "&scope=" + scope;
        return oauthRedirectURL;
 	}
 	
 	public String getCallbackURL(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String returnUrl = requestURL.substring(0, requestURL.lastIndexOf("/")) + "/callback";
        logger.info("Request url is {}", requestURL + " :: callbackURL :" + returnUrl);
        return returnUrl;
    }

    public void callback(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        logger.debug("Oauth callback invoked");
        
 	    try {
 	        String code = request.getParameter(CALLBACK_CODE);
 	        logger.debug("callback "+code);
 	        
 	        String accessToken = fetchAccessToken(request, code);
 	        request.getSession(true).setAttribute(ACCESS_TOKEN_NAME, accessToken);
            String state = String.valueOf(request.getParameter("state"));
            logger.info("State Info: " + state);
            response.sendRedirect(request.getContextPath() + "/#" + page + "?state=" +
					URLEncoder.encode(state, "UTF-8"));
 	        logger.info("Access token is = " + accessToken);
 	    } catch (Exception ex) {
 	        throw new ServletException(ex);
 	    }
    }
    
    private String fetchAccessToken(HttpServletRequest request, String accessCode) throws ServletException
 	{
 	   String accessToken = null;
 	   try
 	   {
 	       /*logger.info("code "+ accessCode);
 	       logger.info("grant_type "+ GRANT_TYPE);
 	       logger.info("client_id "+ appid);
 	       logger.info("client_secret "+ secret);*/
 	      HttpClient httpclient = new HttpClient();
 
 	      PostMethod post = new PostMethod(tokenReqURL);
 	      post.addParameter("code", accessCode);
	      post.addParameter("grant_type", GRANT_TYPE);
 	      post.addParameter("client_id", appid);
 	      post.addParameter("client_secret", secret);
   	      post.addParameter("redirect_uri", request.getRequestURL().toString());
 	      
 	      logger.info("redirect_uri " + request.getRequestURL().toString());
 
 	      httpclient.executeMethod(post);
 	      
 	      try {
 	         JSONObject authResponse = new JSONObject(new JSONTokener(new InputStreamReader(post.getResponseBodyAsStream())));
 	         logger.info("Auth response: "+ authResponse.toString(2));
 
 	         accessToken = authResponse.getString(CALLBACK_ACCESS_TOKEN);
 	         logger.info("Got access token: " + accessToken);
 	      } catch (JSONException e) {
 	         logger.error(e.getMessage(), e);
 	         throw new ServletException(e);
 	      }	finally {
 	         post.releaseConnection();
 	      }
 	   } catch(Exception e) {
 	       logger.error(e.getMessage(), e);
 	       throw new ServletException(e);
 	   }
 	   return accessToken;
   }

}

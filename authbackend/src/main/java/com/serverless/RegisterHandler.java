package com.serverless;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.services.UserService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RegisterHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final UserService userService = UserService.getInstance();

	private static final Logger LOG = LogManager.getLogger(RegisterHandler.class);

	@Override
	public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		LOG.info("received: {}", input);
		String requestBody = input.getBody();
		Map<String, Object> inputMap;
		try {
			inputMap = objectMapper.readValue(requestBody, Map.class);
			LOG.info(inputMap);

			if (this.userService.userExists(inputMap.get("email").toString())) {
				return ApiGatewayResponse.builder()
						.setStatusCode(400)
						.setObjectBody(new Response("User already exists", new HashMap<>()))
						.build();
			}

			String passwordValidation = this.userService.validatePassword(inputMap.get("password").toString());

			LOG.info("Password Validation Result: {}", passwordValidation);

			if (passwordValidation != "valid") {
				return ApiGatewayResponse.builder()
						.setStatusCode(400)
						.setObjectBody(new Response(passwordValidation, new HashMap<>()))
						.build();
			}
			this.userService.createUser(inputMap);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Response responseBody = new Response("Go Serverless v1.x! Your function executed successfully!", new HashMap<>());

		return ApiGatewayResponse.builder()
				.setStatusCode(200)
				.setObjectBody(responseBody)
				.header("X-Powered-By", "AWS Lambda & serverless")
				.build();
	}
}

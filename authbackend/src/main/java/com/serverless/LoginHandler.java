package com.serverless;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.services.UserService;

import com.serverless.utils.PasswordUtils;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class LoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final UserService userService = UserService.getInstance();

	private static final Logger LOG = LogManager.getLogger(LoginHandler.class);

	@Override
	public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		LOG.info("received: {}", input);
		String requestBody = input.getBody();
		Map<String, Object> inputMap;
		try {
			inputMap = objectMapper.readValue(requestBody, Map.class);
			LOG.info(inputMap);

			Map<String, AttributeValue> user = this.userService.findUserByEmail(inputMap.get("email").toString());

			if (user == null) {
				return ApiGatewayResponse.builder()
						.setStatusCode(400)
						.setObjectBody(new Response("User does not exist", new HashMap<>()))
						.build();
			}

			String password = user.get("password").s();
			boolean passwordValid = PasswordUtils.validatePassword(inputMap.get("password").toString(), password);

			if (!passwordValid) {
				return ApiGatewayResponse.builder()
						.setStatusCode(400)
						.setObjectBody(new Response("Password is not valid", new HashMap<>()))
						.build();
			}

			String token = userService.generateToken(inputMap.get("email").toString());

			HashMap<String, Object> responseObj = new HashMap<String, Object>();
			responseObj.put("token", token);

			Response responseBody = new Response("Login Handler", responseObj);

			Map<String, String> headers = new HashMap<String, String>();
			headers.put("X-Powered-By", "AWS Lambda & serverless");

			return ApiGatewayResponse.builder()
				.setStatusCode(200)
				.setObjectBody(responseBody)
				.setHeaders(headers)
				.build();

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ApiGatewayResponse.builder()
				.setStatusCode(500)
				.setObjectBody(new Response("Internal Server Error", new HashMap<>()))
				.build();
	}
}

package com.serverless.services;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.serverless.utils.PasswordUtils;

public class UserService {
  private static final Logger LOG = LogManager.getLogger(UserService.class);
  private static final String TABLE_NAME = System.getenv("DYNAMODB_TABLE");
  private final DynamoDbClient dynamoDbClient;
  private static UserService instance;

  private UserService() {
    dynamoDbClient = DynamoDbClient.builder()
      .region(Region.SA_EAST_1) // replace with your desired region
      .credentialsProvider(DefaultCredentialsProvider.create())
      .build();
  }

  public static UserService getInstance() {
    if (instance == null) {
      instance = new UserService();
    }
    return instance;
  }

  public void createUser(Map<String, Object> input) throws NoSuchAlgorithmException {
		Map<String, AttributeValue> itemValues = new HashMap<>();

    String password = PasswordUtils.hashPassword((String) input.get("password"));
    
		itemValues.put("PK", AttributeValue.builder().s("USER#" + input.get("email")).build());
		itemValues.put("SK", AttributeValue.builder().s("USER#" + input.get("email")).build());
		itemValues.put("email", AttributeValue.builder().s((String) input.get("email")).build());
		itemValues.put("password", AttributeValue.builder().s(password).build());
		itemValues.put("entityType", AttributeValue.builder().s("USER").build());

		PutItemRequest request = PutItemRequest.builder()
						.tableName(TABLE_NAME)
						.item(itemValues)
						.build();

		PutItemResponse response = dynamoDbClient.putItem(request);
		LOG.info("Write succeeded with ID: " + response.sdkHttpResponse().statusCode());
	}

  // a function to check if the user exists
  public boolean userExists(String email) {
    Map<String, AttributeValue> keyToGet = new HashMap<>();
    keyToGet.put("PK", AttributeValue.builder().s("USER#" + email).build());
    keyToGet.put("SK", AttributeValue.builder().s("USER#" + email).build());

    GetItemRequest request = GetItemRequest.builder()
      .key(keyToGet)
      .tableName(TABLE_NAME)
      .build();

    GetItemResponse result = dynamoDbClient.getItem(request);

    if (result.hasItem()) {
      return true;
    }

    return false;
  }

  public static String validatePassword(String input) {
    // Check length
    if (input.length() < 10) {
        return "Password must have at least 10 characters";
    }

    // Check for required characters
    boolean hasSpecialChar = false;
    for (char c : input.toCharArray()) {
        if (c == '!' || c == '@' || c == '#' || c == '?' || c == ']') {
            hasSpecialChar = true;
            break;
        }
    }
    if (!hasSpecialChar) {
        return "Password must contain at least one of the following characters: !, @, #, ?, ]";
    }

    // Input meets all conditions
    return "valid";
  }

  // find user by email
  public Map<String, AttributeValue> findUserByEmail(String email) {
    Map<String, AttributeValue> keyToGet = new HashMap<>();
    keyToGet.put("PK", AttributeValue.builder().s("USER#" + email).build());
    keyToGet.put("SK", AttributeValue.builder().s("USER#" + email).build());

    GetItemRequest request = GetItemRequest.builder()
      .key(keyToGet)
      .tableName(TABLE_NAME)
      .build();

    GetItemResponse result = dynamoDbClient.getItem(request);

    if (result.hasItem()) {
      return result.item();
    }

    return null;
  }

  public String generateToken(String email) {
    try {
      Algorithm algorithm = Algorithm.HMAC256("secret");
      String token = JWT.create()
          .withIssuer("57blocksauthbackend")
          // token expires in 20 minutes
          .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 20 * 60 * 1000))
          .withSubject(email)
          .sign(algorithm);

      return token;
    } catch (JWTCreationException exception){
        // Invalid Signing configuration / Couldn't convert Claims.
    }

    return null;
  }
}

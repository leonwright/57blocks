from jwt import decode

def auth(event, context):
    whole_auth_token = event.get('authorizationToken')
    if not whole_auth_token:
        raise Exception('Unauthorized')

    print('Client token: ' + whole_auth_token)
    print('Method ARN: ' + event['methodArn'])

    token_parts = whole_auth_token.split(' ')
    auth_token = token_parts[1]
    print('Auth token: ' + auth_token)
    token_method = token_parts[0]
    print('Token method: ' + token_method)

    if not (token_method.lower() == 'bearer' and auth_token):
        print("Failing due to invalid token_method or missing auth_token")
        raise Exception('Unauthorized')

    try:
        print('Verifying token')
        principal_id = jwt_verify(auth_token, "secret")
        print('principal_id: ' + principal_id)
        policy = generate_policy(principal_id, 'Allow', event['methodArn'])
        return policy
    except Exception as e:
        print(f'Exception encountered: {e}')
        raise Exception('Unauthorized')
    
def jwt_verify(auth_token, secret):
    # verify token with secret "secret" and algorithm HS256
    decoded = decode(auth_token, secret, verify=True, algorithms=['HS256'])
    print('Decoded: ' + str(decoded))
    return decoded['sub']


def generate_policy(principal_id, effect, resource):
    return {
        'principalId': principal_id,
        'policyDocument': {
            'Version': '2012-10-17',
            'Statement': [
                {
                    "Action": "execute-api:Invoke",
                    "Effect": effect,
                    "Resource": resource

                }
            ]
        }
    }
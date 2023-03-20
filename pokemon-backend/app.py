import os

import boto3
from botocore.config import Config
from flask import Flask, jsonify, make_response, request
from controllers.pokemon_controller import pokemon_app
from boto3.dynamodb.types import TypeDeserializer, TypeSerializer
import json

app = Flask(__name__)

my_config = Config(
    region_name = 'sa-east-1',
    signature_version = 'v4',
    retries = {
        'max_attempts': 10,
        'mode': 'standard'
    }
)

client = boto3.client('kinesis', config=my_config)
dynamodb_client = boto3.client('dynamodb', config=my_config)

if os.environ.get('IS_OFFLINE'):
    dynamodb_client = boto3.client(
        'dynamodb', region_name='sa-east-1', endpoint_url='http://localhost:8000'
    )


USERS_TABLE = os.environ['USERS_TABLE']

@app.route('/pokemon', methods=['POST'])
def create_pokemon():
    print('request.json', request.json)
    print('request', request)

    event = request.environ['serverless.event']
    print('event', event)

    request_context = request.environ['serverless.context']
    print('request_context', request_context)

    # get principalId from request_context
    print('eventType', type(event))
    print('eventType.authorizor', type(event.get('requestContext')))
    print('eventType.authorizor', type(event.get('requestContext').get('authorizer')))
    print(event.get('requestContext'))
    principal_id = event['requestContext']['authorizer']['principalId']
    print('principal_id', type(principal_id))

    pokemon = request.json.get('pokemon')
    if not pokemon:
        return jsonify({'error': 'Please provide a pokemon'}), 400
    
    # add PK and SK
    pokemon['PK'] = 'POKEMON'
    pokemon['SK'] = pokemon['name']
    pokemon['principalId'] = principal_id
    
    # create pokemon object and serialize it
    serializer = TypeSerializer()
    serialized_pokemon = {k: serializer.serialize(v) for k,v in pokemon.items()}

    print('serialized_pokemon', serialized_pokemon)

    dynamodb_client.put_item(
        TableName=USERS_TABLE, Item=serialized_pokemon
    )

    return jsonify(pokemon)

# a handler that will get all pokemon with the visibility set to public and the ones created by the user, pagination is also supported
@app.route('/pokemon', methods=['GET'])
def get_pokemon():
    event = request.environ['serverless.event']
    print('event', event)

    request_context = request.environ['serverless.context']
    print('request_context', request_context)

    # get principalId from request_context
    print('eventType', type(event))
    print('eventType.authorizor', type(event.get('requestContext')))
    print('eventType.authorizor', type(event.get('requestContext').get('authorizer')))
    print(event.get('requestContext'))
    principal_id = event['requestContext']['authorizer']['principalId']
    print('principal_id', type(principal_id))

    # get query params
    limit = request.args.get('limit')
    if limit:
        limit = int(limit)
    else:
        limit = 10

    last_evaluated_key = request.args.get('lastEvaluatedKey')

    serializer = TypeSerializer()

    # get all pokemon
    if last_evaluated_key:
        serialized_last_evaludated_key = {k: serializer.serialize(v) for k,v in json.loads(last_evaluated_key.replace("'", '"')).items()}
        response = dynamodb_client.query(
            TableName=USERS_TABLE,
            Limit=limit,
            ExclusiveStartKey=serialized_last_evaludated_key,
            KeyConditionExpression='PK = :pk',
            FilterExpression='visibility = :visibility OR principalId = :principalId',
            ExpressionAttributeValues = {
                ':pk': {"S":"POKEMON"},
                ":visibility": {"S":"public"},
                ":principalId": {"S":principal_id}
            }
        )
    else:
        response = dynamodb_client.query(
            TableName=USERS_TABLE,
            Limit=limit,
            KeyConditionExpression='PK = :pk',
            FilterExpression='visibility = :visibility OR principalId = :principalId',
            ExpressionAttributeValues={
                ':pk': {'S': 'POKEMON'},
                ':visibility': {'S': 'public'},
                ':principalId': {'S': principal_id}
            }
        )

    # deserialize the response
    deserializer = TypeDeserializer()
    items = response['Items']
    for item in items:
        for k, v in item.items():
            item[k] = deserializer.deserialize(v)

    # filter the pokemon
    filtered_items = []
    for item in items:
        if item['visibility'] == 'public':
            filtered_items.append(item)
        elif item['principalId'] == principal_id:
            filtered_items.append(item)

    # add pagination
    if 'LastEvaluatedKey' in response:
        last_evaluated_key = response['LastEvaluatedKey']
        last_evaluated_key = {k: deserializer.deserialize(v) for k,v in last_evaluated_key.items()}
        last_evaluated_key = str(last_evaluated_key)
    else:
        last_evaluated_key = None

    return jsonify({'items': filtered_items, 'lastEvaluatedKey': last_evaluated_key})


@app.errorhandler(404)
def resource_not_found(e):
    return make_response(jsonify(error='Not found!'), 404)

# start the app
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)

from flask import Flask, jsonify

pokemon_app = Flask(__name__)

@pokemon_app.route('/', methods=['GET'])
def health_check():
    return jsonify({'status': 'ok'})

@pokemon_app.route('/example')
def example():
    data = {'key': 'value'}
    return jsonify(data)
import json

def lambda_handler(event, context):
    print("Evento recebido do S3:")
    print(json.dumps(event))
    return {
        "statusCode": 200,
        "body": "Arquivo processado com sucesso!"
    }

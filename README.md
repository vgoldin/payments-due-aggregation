# Pre-Requisites
https://serverless.com/framework/docs/providers/aws/guide/quick-start/

https://gradle.org/

# Steps
export AWS_ACCESS_KEY_ID=[your aws access key id]

export AWS_SECRET_ACCESS_KEY=[your aws secret key]

./gradlew build

serverless deploy

# Test
curl -XPOST https://xxx.execute-api.xxx.amazonaws.com/dev/loans/payments/due --data @./request.json

serverless invoke --function repaymentSettlement --log

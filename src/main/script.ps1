. "$PSScriptRoot\utils.ps1"

# 定义要发送的数据
$key = "1" # itemId
$json = @"
  {\"user_id\":\"1\",\"user_name\":\"zhexiang\"}
"@
$ingress_topic = "login"

# 调用 Send-ToKafka 函数
Write-Host "Sending ""$json"" with key ""$key"" to ""$ingress_topic"" topic"
docker exec statefun-tests-shopping-cart-kafka-1 bash -c ("echo '${key}: $json' | /usr/bin/kafka-console-producer --topic $ingress_topic --broker-list kafka:9092 --property 'parse.key=true' --property 'key.separator=:'")

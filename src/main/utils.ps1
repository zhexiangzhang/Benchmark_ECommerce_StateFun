function Send-ToKafka {
    param(
        [string]$Key,
        [string]$Payload,
        [string]$Topic
    )

    Write-Host "Sending ""$Payload"" with key ""$Key"" to ""$Topic"" topic"
    docker-compose exec kafka bash -c "echo '$Key: $Payload' | /usr/bin/kafka-console-producer --topic $Topic --broker-list localhost:9092 --property 'parse.key=true' --property 'key.separator=:'"
}

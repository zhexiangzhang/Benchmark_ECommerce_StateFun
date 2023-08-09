#!/bin/bash

source $(dirname "$0")/utils.sh

key="1" # itemId
json=$(cat <<JSON
  {"user_id":"1","user_name":"zhexiang"}
JSON
)
ingress_topic="login" # StockFn
send_to_kafka $key $json $ingress_topic
done

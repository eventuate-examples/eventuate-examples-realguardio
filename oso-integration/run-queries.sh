#! /bin/bash -e 

query() {
    echo ==== "$@"
    echo 
    oso-cloud query "$@"
    echo 
}

source ./set-oso-env.sh

query has_permission User:alice String:disarm SecuritySystem:ss1
query has_permission User:alice String:disarm SecuritySystem:ss2
query has_permission User:bob String:disarm SecuritySystem:ss2
query has_permission User:bob String:disarm SecuritySystem:ss1
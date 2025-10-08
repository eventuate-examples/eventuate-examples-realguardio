#! /bin/bash -e 

query_fail() {
    echo ==== "$@"
    echo 
    result=$(oso-cloud query "$@")
    if [[ "$result" == "(no results)" ]]; then
        echo "$result"
    else
        echo "Expected (no results) but got: $result"
        exit 1
    fi
    echo 
}

query_success() {
    echo ==== "$@"
    echo 
    result=$(oso-cloud query "$@")
    if [[ "$result" == *has_permission* ]]; then
        echo "$result"
    else
        echo "Expected result containing 'has_permission' but got: $result"
        exit 1
    fi
    echo 
}

source ./set-oso-env.sh

query_success has_permission CustomerEmployee:alice String:disarm SecuritySystem:ss1
query_fail has_permission CustomerEmployee:alice String:disarm SecuritySystem:ss2

query_success has_permission CustomerEmployee:bob String:disarm SecuritySystem:ss2
query_fail has_permission CustomerEmployee:bob String:disarm SecuritySystem:ss1

query_success has_permission CustomerEmployee:mary String:disarm SecuritySystem:ss3

query_success has_permission CustomerEmployee:charlie String:disarm SecuritySystem:ss1
query_fail has_permission CustomerEmployee:charlie String:disarm SecuritySystem:ss2
query_fail has_permission CustomerEmployee:charlie String:disarm SecuritySystem:ss3

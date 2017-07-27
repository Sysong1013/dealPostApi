#!/bin/bash

##################################

## need to modify every api or job
MAIN_CLASS="com.dangdang.stock.dealpost.StockDealPostServiceProviderMain"
###/dubbo.port/ 正则匹配dubbo.port行 !该行取反  d删除  ;连接后面的命令
### s/.*=// =前任意字符进行替换成空 tr -d '\r' 删除换行符
SERVER_DUBBO_PORT=`sed '/dubbo.port/!d;s/.*=//' ../conf/common.properties | tr -d '\r'`

##################################
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=${DEPLOY_DIR}/conf
SERVER_NAME=`pwd | awk -F '/' '{print $NF}' `
LOGS_FILE=""

PIDS=`ps aux | grep "$CONF_DIR" | grep -v grep |awk '{print $2}'`
if [ -n "${PIDS}" ]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

if [ -n "${SERVER_DUBBO_PORT}" ]; then
    SERVER_PORT_COUNT=`netstat -tln | grep ${SERVER_DUBBO_PORT} | wc -l`
    if [ ${SERVER_PORT_COUNT} -gt 0 ]; then
        echo "ERROR: The ${SERVER_NAME} port ${SERVER_DUBBO_PORT} already used!"
        exit 1
    fi
fi

LOGS_DIR=""
if [ -n "$LOGS_FILE" ]; then
    LOGS_DIR=`dirname ${LOGS_FILE}`
else
    LOGS_DIR=${DEPLOY_DIR}/logs
fi
if [ ! -d ${LOGS_DIR} ]; then
    mkdir ${LOGS_DIR}
fi

STDOUT_FILE=${LOGS_DIR}/stdout.log

LIB_DIR=${DEPLOY_DIR}/lib
LIB_JARS=${DEPLOY_DIR}/lib/*

JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "

JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
    JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -Xmn1024m -XX:PermSize=128m -Xss512k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
else
    JAVA_MEM_OPTS=" -server -Xms1g -Xmx1g -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi

echo -e "Starting the ${SERVER_NAME} ...\c"

nohup java ${JAVA_OPTS} ${JAVA_MEM_OPTS} -classpath ${CONF_DIR}:${LIB_JARS} ${MAIN_CLASS} > ${STDOUT_FILE} 2>&1 &

COUNT=0
while [ ${COUNT} -lt 1 ]; do
    echo -e ".\c"
    sleep 1 
    if [ -n "$SERVER_DUBBO_PORT" ]; then
        COUNT=`netstat -lntp | grep ${SERVER_DUBBO_PORT} | wc -l`
    else
    	COUNT=`ps aux | grep "${DEPLOY_DIR}" | grep -v grep | awk '{print $2}' | wc -l`
    fi
    if [ ${COUNT} -gt 0 ]; then
        break
    fi
done

echo "OK!"
PIDS=`ps aux | grep "${DEPLOY_DIR}" | grep -v grep| awk '{print $2}'`
echo "PID: ${PIDS}"
echo "STDOUT: ${STDOUT_FILE}"

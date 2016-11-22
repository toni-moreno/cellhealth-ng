#!/bin/bash
function  usage() {
    cat << EOF
    usage: $0 script_options [CELLHEALTH_OPTIONS]

        start      start cellhealth with [CELLHEALTH_OPTIONS]
        status     show cellhealth status
        stop       stop cellhealth
        restart    restar cellhealth
        chopts     show cellhealth options

EOF
}

CELLHEALTH_HOME=/opt/cellhealth-ng
LIB_DIR=${CELLHEALTH_HOME}/lib
CELLHEALTH_PATH="-Dch_dir_path=/opt/cellhealth-ng/"
source $CELLHEALTH_HOME/conf/setup.conf
JAVA_HOME="$WAS_HOME"/java

[ -z "$SETUPCMDLINE_PATH" ] && {
        echo "ERROR: you should configure SetupCmdLine.sh path first"
        exit 1
} || {
        source $SETUPCMDLINE_PATH
}

DMGR_PATH=`echo $SETUPCMDLINE_PATH | sed 's|/bin/setupCmdLine.sh||'`

#check if  wsadmin.properties exist

WSADMIN_PROPERTIES=$DMGR_PATH/properties/wsadmin.properties

[ -f "$WSADMIN_PROPERTIES"  ] || {
        echo "ERROR: not wsadmin.properties file found on $WSADMIN_PROPERTIES"
        exit 1
}

DMGR_HOST=`grep "^com.ibm.ws.scripting.host" $WSADMIN_PROPERTIES | awk -F'=' '{ print $2}' `
DMGR_PORT=`grep "^com.ibm.ws.scripting.port" $WSADMIN_PROPERTIES | awk -F'=' '{ print $2}' `

DMGR_CONFIG=""

if [[ -n "$DMGR_HOST" ]]
then
     DMGR_CONFIG="$DMGR_CONFIG --host $DMGR_HOST"
fi

if [[ -n "$DMGR_PORT" ]]
then
     DMGR_CONFIG="$DMGR_CONFIG --port $DMGR_PORT"
fi

CLASSPATH=$(JARS=("$LIB_DIR"/*.jar); IFS=:; echo "${JARS[*]}"):${WAS_HOME}/runtimes/:${WAS_HOME}/lib/

if [[ -n $LOGFILE ]]
then
        LOG4JAVA="-Dch_l4j_configuration_path=$LOGFILE"
else
        LOG4JAVA="-Dch_l4j_configuration_path=${CELLHEALTH_HOME}/conf/log4j.properties"
fi

if [[ -n $CONFFILE ]]
then
        CONFILE="-Dch_config_path=$CONFILE"
else
        CONFILE="-Dch_config_path=${CELLHEALTH_HOME}/conf/config.properties"
fi

MAIN_JAR=`ls -1tr ${CELLHEALTH_HOME}/bin/cellhealth-ng.*.jar|tail -1`

function get_pidof() {
    ps -ef | grep java |grep ${MAIN_JAR}| grep -v grep |awk '{ print $2}'
}

function start() {
        CMD="${JAVA_HOME}/bin/java $CELLHEALTH_PATH $CONFILE $CLIENTSAS $STDINCLIENTSAS $SERVERSAS $CLIENTSOAP $CLIENTIPC $JAASSOAP $CLIENTSSL $WAS_LOGGING -cp $CLASSPATH:${CELLHEALTH_HOME}/lib $LOG4JAVA -jar ${MAIN_JAR} $DMGR_CONFIG $@"

        NUID=`id -nu`
        if [ "$EXEC_USER" == "$NUID" -o  -z "$EXEC_USER" ]
        then
                eval nohup "$CMD" 0<&- &> $CELLHEALTH_HOME/logs/start.log &
        else
                nohup  su - $EXEC_USER -c "$CMD" 0<&- &> $CELLHEALTH_HOME/logs/start.log &
        fi

}

function show_ch_option() {
        ${JAVA_HOME}/bin/java $CELLHEALTH_PATH $CONFILE $CLIENTSAS $STDINCLIENTSAS $SERVERSAS $CLIENTSOAP $CLIENTIPC $JAASSOAP $CLIENTSSL $WAS_LOGGING -cp $CLASSPATH:${CELLHEALTH_HOME}/lib $LOG4JAVA -jar ${MAIN_JAR} $DMGR_CONFIG $1
}



function show_ch_opts() {
        ${JAVA_HOME}/bin/java $CELLHEALTH_PATH $CONFILE $CLIENTSAS $STDINCLIENTSAS $SERVERSAS $CLIENTSOAP $CLIENTIPC $JAASSOAP $CLIENTSSL $WAS_LOGGING -cp $CLASSPATH:${CELLHEALTH_HOME}/lib $LOG4JAVA -jar ${MAIN_JAR} $DMGR_CONFIG -h
}

function status() {
        PID=`get_pidof`

        if [[ -n $PID ]]
        then
                echo "CellHealth working with PID=$PID"
                echo $PID > $CELLHEALTH_HOME/logs/cellhealth.pid
                echo "===================================================================================="
                ps -ef | grep java |grep ${MAIN_JAR}
                echo "===================================================================================="
                exit 0
        else
                echo "CellHealth is not working !!!"
                exit 1
        fi
}

function stop() {

        PID=`get_pidof`
        if [[ -n $PID ]]
        then
                echo "Stopping CellHealth with PID=$PID"
                kill -9 $PID
                rm -f $CELLHEALTH_HOME/logs/cellhealth.pid
                exit 0
        else
                echo "CellHealth is not working !!!"
                exit 0
        fi

}

case "$1" in
  status)
    shift
    status $@
    ;;
  start)
    PID=`get_pidof`
    if [[ -n $PID ]]
    then
      echo "Error Other Cellhealth PID exists($PID)"
      exit 1
    fi
    shift
    start $@
    sleep 4
    status
    ;;
  stop)
    shift
    stop $@
    ;;
  restart)
    shift
    stop $@
    start $@
    ;;
  chopts) 
    OPTION="$2"
    [ -z "$OPTION" ] && {
	#echo "OPTION -h"
    	show_ch_opts
    } || {
	#echo "OPTION $2"
	show_ch_option $2
    }	 
    ;;
  *)
    usage
esac

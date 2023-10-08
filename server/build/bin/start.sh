#!/bin/sh

#===========================================================================================
# Java Environment Setting
#===========================================================================================
error_exit() {
  echo "ERROR: $1 !!"
  exit 1
}

[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"

export JAVA_HOME
export JAVA="$JAVA_HOME/bin/java"
export BASE_DIR
BASE_DIR=$(dirname $0)/..
export CLASSPATH=.:${BASE_DIR}/lib/*:${CLASSPATH}

#===========================================================================================
# JVM Configuration
#===========================================================================================
# The RAMDisk initializing size in MB on Darwin OS for gc-log
DIR_SIZE_IN_MB=600

choose_gc_log_directory() {
  case "$(uname)" in
  Darwin)
    if [ ! -d "/Volumes/RAMDisk" ]; then
      # create ram disk on Darwin systems as gc-log directory
      DEV=$(hdiutil attach -nomount ram://$((2 * 1024 * DIR_SIZE_IN_MB))) >/dev/null
      diskutil eraseVolume HFS+ RAMDisk ${DEV} >/dev/null
      echo "Create RAMDisk /Volumes/RAMDisk for gc logging on Darwin OS."
    fi
    GC_LOG_DIR="/Volumes/RAMDisk"
    ;;
  *)
    # check if /dev/shm exists on other systems
    if [ -d "/dev/shm" ]; then
      GC_LOG_DIR="/dev/shm"
    else
      GC_LOG_DIR=${BASE_DIR}
    fi
    ;;
  esac
}

choose_gc_options() {
  JAVA_MAJOR_VERSION=$("$JAVA" -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
  if [ -z "$JAVA_MAJOR_VERSION" ] || [ "$JAVA_MAJOR_VERSION" -lt "8" ]; then
    JAVA_OPT="${JAVA_OPT} -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 -XX:-UseParNewGC"
  else
    JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25 -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0"
  fi

  if [ -z "$JAVA_MAJOR_VERSION" ] || [ "$JAVA_MAJOR_VERSION" -lt "9" ]; then
    JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:${GC_LOG_DIR}/mqtt_srv_gc_%p_%t.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintAdaptiveSizePolicy"
    JAVA_OPT="${JAVA_OPT} -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
  else
    JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25 -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0"
    JAVA_OPT="${JAVA_OPT} -Xlog:gc*:file=${GC_LOG_DIR}/mqtt_srv_gc_%p_%t.log:time,tags:filecount=5,filesize=30M"
  fi
}

# command param
export CONFIG_FILE=""

parse_args() {
  while getopts ":c:" opt; do
    case ${opt} in
    c)
      CONFIG_FILE=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG:" 1>&2
      exit 1
      ;;
    :)
      echo "Invalid option: -$OPTARG requires an argument" 1>&2
      exit 1
      ;;
    esac
  done
  shift $((OPTIND - 1))
}

check_args() {
  if [ -z "$CONFIG_FILE" ]; then
    echo "Error: The -c option is required." 1>&2
    exit 1
  fi
}

choose_gc_log_directory

JAVA_OPT="${JAVA_OPT} -server -Xms8g -Xmx8g"
choose_gc_options
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:+AlwaysPreTouch"
JAVA_OPT="${JAVA_OPT} -XX:MaxDirectMemorySize=15g"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages -XX:-UseBiasedLocking"
JAVA_OPT="${JAVA_OPT} ${JAVA_OPT_EXT}"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"

# 调用解析命令行参数的函数
parse_args "$@"

check_args

nohup $JAVA ${JAVA_OPT} -jar ${BASE_DIR}/lib/easy-mqtt-server-*.jar >/dev/null 2>&1 &

if [ $? -eq 0 ]; then
  echo $! >${BASE_DIR}/bin/pid
else
  echo "Start fail"
fi

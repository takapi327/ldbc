#!/bin/bash

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

OUT_DIR="benchmark"
OPTIONS="-t 1"

sbt=sbt

OUTPUTS=()
TARGETS=()
FEATURES=()
PREFIX=''
SUFFIX=''

file_path() {
    PROJECT=$1
    TARGET=$2
    TYPE=$3
    FEATURE=$4

    echo "${PROJECT}/src/main/scala/benchmark/${TYPE}/${TARGET}/${FEATURE}.scala"
}

run() {
    PROJECT=$1
    TARGET=$2
    TYPE=$3
    FEATURE=$4

    FILE=$(file_path "$PROJECT" "$TARGET" "$TYPE" "$FEATURE")
    [ -r "$FILE" ] || return 0

    mkdir -p "script/${OUT_DIR}/${TYPE}/${TARGET}"
    OUTPUT="script/${OUT_DIR}/${TYPE}/${TARGET}/${FEATURE}.json"

    $sbt "${PROJECT} / clean"
    $sbt "${PROJECT} / Jmh / run $OPTIONS ${TYPE}[.]${TARGET}[.]${FEATURE}[.] -rf json -rff ../${OUTPUT}"
    OUTPUTS+=("$OUTPUT")
}

to_json_rows() {
    jq -s '[ .[] | {
        benchmark:.benchmark,
        target: (.benchmark | split(".") | .[2] + "_" + .[4]),
        feature: .benchmark | split(".")[3],
        index: .params.len | tonumber,
        score: (.primaryMetric.rawData[] | .[])
    } ]'
}

while :
do
    [ "$1" = "-0" ] && {
        # Skip JMH run but accumulate outpus and plot the charts
        # This is expecting the following workflow:
        #   1. run `script/benchmark.sh <project> <target> <feature>` one by one
        #   2. run `script/benchmark.sh -0 to accumulate all the results and plot the charts
        shift
        sbt=:
        continue
    }

    [ "$1" = "-1" ] && { # just run once for test
        shift
        OPTIONS="-wi 0 -i 1 -t 1 -f 1"
        continue
    }

    [ "$1" = "-3" ] && { # just run few iterations for test
        shift
        OPTIONS="-wi 0 -i 3 -t 1 -f 1"
        continue
    }

    [ "$1" = '-t' ] && [ -n "$2" ] && {
        TARGETS+=("$2")
        shift; shift
        continue
    }

    [ "$1" = '-f' ] && [ -n "$2" ] && {
        FEATURES+=("$2")
        shift; shift
        continue
    }

    [ "$1" = '-p' ] && [ -n "$2" ] && {
        PREFIX="${2}_"
        shift; shift
        continue
    }

    [ "$1" = '-s' ] && [ -n "$2" ] && {
        SUFFIX="_${2}"
        shift; shift
        continue
    }

    break
done

[ ${#TYPES[@]} = 0 ] && {
    TYPES=(
        'connector'
        'wrapper'
    )
}

[ ${#TARGETS[@]} = 0 ] && {
    TARGETS=(
        'jdbc'
        'ldbc'
        'doobie'
        'slick'
    )
}

[ ${#FEATURES[@]} = 0 ] && {
    FEATURES=(
        'Insert'
        'Batch'
        'Select'
    )
}

EXCLUDE=('doobie' 'slick')

run_feature() {
    TYPE="$1"
    FEATURE="$2"
    # If TYPE is connector, only jdbc and ldbc are targeted.
    [ "$TYPE" = "connector" ] && TARGETS=(${TARGETS[@]/doobie/})
    [ "$TYPE" = "connector" ] && TARGETS=(${TARGETS[@]/slick/})

    for target in ${TARGETS[@]}; do
        run "benchmark" "$target" "${TYPE}" "${FEATURE}"
    done

    CHART_INPUT="script/${OUT_DIR}/${TYPE}/${PREFIX}${FEATURE}${SUFFIX}.json"
    CHART_OUTPUT="docs/src/main/mdoc/img/${TYPE}/${PREFIX}${FEATURE}${SUFFIX}.svg"

    #OUTPUTS+=("script/${OUT_DIR}/${TYPE}/jdbc/${FEATURE}.json")
    #OUTPUTS+=("script/${OUT_DIR}/${TYPE}/ldbc/${FEATURE}.json")
    #OUTPUTS+=("script/${OUT_DIR}/${TYPE}/doobie/${FEATURE}.json")
    #OUTPUTS+=("script/${OUT_DIR}/${TYPE}/slick/${FEATURE}.json")

    for output in ${OUTPUTS[@]}; do
        jq '.[]' "${output}"
    done | to_json_rows > "${CHART_INPUT}"

    script/python/visualize.sh "${FEATURE}" "${CHART_INPUT}" "${CHART_OUTPUT}"

    OUTPUTS=()
}

run_type() {
  TYPE="$1"
  for feature in ${FEATURES[@]}; do
      run_feature "$TYPE" "$feature"
  done
}

for type in ${TYPES[@]}; do
    run_type "$type"
done

#!/bin/sh

. ./rest-common-env.sh

printf "Username: "
read loginName
printf "Password: "
read password

if [ "$3" = "--prod" ]; then
    baseServiceURL=$PRODUCTION_BASE_SERVICE_URL
    certificate=$PRODUCTION_CERTIFICATE
elif [ "$3" = "--dev" ]; then
   baseServiceURL=$DEVELOPMENT_BASE_SERVICE_URL
   certificate=$DEVELOPMENT_CERTIFICATE
else
   baseServiceURL=$DEMO_BASE_SERVICE_URL
   certificate=$DEMO_CERTIFICATE
fi

file=$2

if [ "$file" = "" ]; then
    printf "Missing file\n" 1>&2
    exit 1
fi

authorization=`echo -n "$loginName:$password" | base64`

result=`curl -sv -X POST $certificate -d @$file \
    --header "Content-type: application/xml" \
    --header "Authorization: Basic $authorization" \
    $baseServiceURL/$1`

if hash tidy &> /dev/null; then
    echo $result | tidy -xml -i -q -utf8
else
    echo $result
fi

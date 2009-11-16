#!/bin/bash

NAVALPLANNER_WEBAPP="../navalplanner-webapp";
NAVALPLANNER_GANTTZK="../ganttzk";
NAVALPLANNER_BUSINESS="../navalplanner-business";

WEBAPP_KEYS="$NAVALPLANNER_WEBAPP/src/main/resources/i18n/keys.pot"
GANTTZK_KEYS="$NAVALPLANNER_GANTTZK/src/main/resources/i18n/keys.pot"

# Parse webapp java and zul and ganttzk validation messages
WKDIR=`pwd`
cd $NAVALPLANNER_WEBAPP
mvn gettext:gettext 2> /dev/null
cd "${WKDIR}"

if [ ! -f $WEBAPP_KEYS ]
    then touch $WEBAPP_KEYS
fi
./gettext-keys-generator.pl -d $NAVALPLANNER_WEBAPP -k $WEBAPP_KEYS 2> /dev/null
./gettext-keys-generator.pl --java -d $NAVALPLANNER_BUSINESS -k $WEBAPP_KEYS 2> /dev/null

# Parse ganttzk java and zul
cd $NAVALPLANNER_GANTTZK
mvn gettext:gettext 2> /dev/null
cd "${WKDIR}"

if [ ! -f $GANTTZK_KEYS ]
    then touch $GANTTZK_KEYS
fi
./gettext-keys-generator.pl -d $NAVALPLANNER_GANTTZK -k $GANTTZK_KEYS 2> /dev/null

python genSpacecraft.py FoxPlus-A rt ../FoxPlus-A/DownlinkSpecFoxPlus-A.csv "commonRtMinmaxWodPayload_t commonRtWodPayload_t realtimeSpecific_t" LEGACY_IHU
python genSpacecraft.py FoxPlus-A max ../FoxPlus-A/DownlinkSpecFoxPlus-A.csv "commonRtMinmaxWodPayload_t maxSpecific_t" LEGACY_IHU
python genSpacecraft.py FoxPlus-A min ../FoxPlus-A/DownlinkSpecFoxPlus-A.csv "commonRtMinmaxWodPayload_t minSpecific_t" LEGACY_IHU
python genSpacecraft.py FoxPlus-A wod ../FoxPlus-A/DownlinkSpecFoxPlus-A.csv "commonRtMinmaxWodPayload_t commonRtWodPayload_t wodSpecific_t" LEGACY_IHU
python genSpacecraft.py FoxPlus-A diagnostic ../FoxPlus-A/DownlinkSpecFoxPlus-A.csv "infrequentDownlink_t legacyErrors_t" LEGACY_IHU
echo "Sending to 1.13j"
cp FoxPlus-A_*.csv ~/Dropbox/BF\ Dropboxes\ Link/FoxTelem/FoxTelem_1.13j/FoxTelem/spacecraft/

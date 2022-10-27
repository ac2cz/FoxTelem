# This generates part of the GOLF-T files.  WOD and EXP are not included
python genSpacecraft.py GOLF-T header DownlinkSpecGolf-T.csv "header_t" LEGACY_IHU 
python genSpacecraft.py GOLF-T rt DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t commonRtWodPayload_t realtimeSpecific_t" LEGACY_IHU 
python genSpacecraft.py GOLF-T min DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t minSpecific_t" LEGACY_IHU 
python genSpacecraft.py GOLF-T max DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t maxSpecific_t" LEGACY_IHU 
python genSpacecraft.py GOLF-T diagnostic DownlinkSpecGolf-T.csv "infrequentDownlink_t legacyErrors_t rt1Errors_t rt2Errors_t" LEGACY_IHU 
python genSpacecraft.py GOLF-T wodrag DownlinkSpecGolf-T.csv "ragnarok_t ragWodSpecific_t" LEGACY_IHU 
cp GOLF-T*.csv ../spacecraft

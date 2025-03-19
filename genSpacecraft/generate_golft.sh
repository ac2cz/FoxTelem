#Note: The 2nd parameter to genSpacecraft.py is only for the name of the output csv file.  It is not the type despite the fact
# that it appears in the type column of the CSV.  The actual type is only in the master file.  (rt,max,min,wod may be special--
# don't mess with them) 
python3 genSpacecraft.py GOLF-T rt ../Golf-Tee/DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t commonRtWodPayload_t realtimeSpecific_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T max ../Golf-Tee/DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t maxSpecific_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T min ../Golf-Tee/DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t minSpecific_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T rad ../Golf-Tee/DownlinkSpecGolf-T.csv "radiation_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T rag ../Golf-Tee/DownlinkSpecGolf-T.csv "ragnarok_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T wod ../Golf-Tee/DownlinkSpecGolf-T.csv "commonRtMinmaxWodPayload_t commonRtWodPayload_t wodSpecific_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T wodrad ../Golf-Tee/DownlinkSpecGolf-T.csv "radiation_t radWodSpecific_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T wodrag ../Golf-Tee/DownlinkSpecGolf-T.csv "ragnarok_t ragWodSpecific_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T diagnostic ../Golf-Tee/DownlinkSpecGolf-T.csv "infrequentDownlink_t rt1Errors_t rt2Errors_t  legacyErrors_t" LEGACY_IHU
python3 genSpacecraft.py GOLF-T ADCSLog ../Golf-Tee/DownlinkSpecGolf-T.csv "ADCSLogData_t ADCSLog_t" LEGACY_IHU
cp GOLF-T*.csv ../spacecraft

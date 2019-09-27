'''
Created on Feb 27, 2018

@author: chris g0kla
AMSAT FOXDB Difference tool
'''
import sys
import os
import glob
import errno
from shutil import copyfile

spacecraftIds = [1,2,4]
resetStarts = [0,745,10,0,8]
uptimeStarts = [0,475144,1451233,0,15202]
resetEnds = [0,745,10,0,8]
uptimeEnds = [0,595102,1571191,0,135160]
DATE_COL = 0;
ID_COL = 1;
RESET_COL = 2;
UPTIME_COL = 3;
TYPE_COL = 4;

def loadLog(log, store):
    #print("  loading: " + log + " ...")
    with open(log, "r") as f:
        for line in f:
            fields = line.split(',')
            id = fields[ID_COL]
            reset = fields[RESET_COL]
            uptime = fields[UPTIME_COL]
            type = fields[TYPE_COL]
			# store the records
            key = id + "_" + reset + "_" + uptime + "_" + type
            store[key] = line
    f.close()
    
def loadLogs(dir, match, store):
    logs = glob.glob(dir + os.sep + match + "*.log")
    if len(logs) == 0:
        #print ('No log files found in ' + dir + ' for ' + match)
        return False
    for item in logs:
        log = os.path.basename(item)
        loadLog(item, store)
    return True

def diff(id, server, local, match):
    resetField = 1
    uptimeField = 2
    missing = 0
    log = match + ".log"
    with open(log, "w") as f:
        for k in local:
            keys = k.split('_')
            if (int(keys[resetField]) >= resetStarts[id]) and (int(keys[resetField]) <= resetEnds[id]):
                if (int(keys[uptimeField]) >= uptimeStarts[id]) and (int(keys[uptimeField]) <= uptimeEnds[id]):
                    if (k not in server):
                        f.write(local[k])
                        missing = missing + 1
        print(match + " - Missed records written to file: " + str(missing))
    f.close()
    return missing

def diffAndSave(id, serverDir, dir, match):
    total = 0
    serverRecords = {}
    records = {}
    if (loadLogs(dir, match, records)):
        if(loadLogs(serverDir, match, serverRecords)):
            total = total + diff(id, serverRecords, records, match)
    return total

def main():
    total = 0
    if len(sys.argv) <= 2:
        print("Usage: diffFoxdb <Server FOXDB> <FOXDB>")
        sys.exit(1)
    serverDir = sys.argv[1]
    dir = sys.argv[2]
    if not os.path.isdir(serverDir):
        print("Invalid server directory: " + serverDir)
        exit(1)
    if not os.path.isdir(dir):
        print("Invalid directory: " + dir)
        exit(1)
    print("DiffFOXDB:")
    for id in spacecraftIds:
        total = total + diffAndSave(id, serverDir, dir, "Fox"+str(id)+"rttelemetry")
        total = total + diffAndSave(id, serverDir, dir, "Fox"+str(id)+"maxtelemetry")
        total = total + diffAndSave(id, serverDir, dir, "Fox"+str(id)+"mintelemetry")
        total = total + diffAndSave(id, serverDir, dir, "Fox"+str(id)+"radtelemetry")
    print("TOTAL MISSING: " + str(total))
main()
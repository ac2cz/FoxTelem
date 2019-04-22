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

spacecraftIds = [4]
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
            store[key] = fields
    f.close()
    
def loadLogs(dir, match, store):
    logs = glob.glob(dir + os.sep + match)
    if len(logs) == 0:
        print ('No log files found in ' + dir + ' for ' + match)
        return
    for item in logs:
        log = os.path.basename(item)
        loadLog(item, store)

def load(dir, store):
    for id in spacecraftIds:
        loadLogs(dir, "Fox"+str(id)+"rttelemetry*.log", store)
        loadLogs(dir, "Fox"+str(id)+"mintelemetry*.log", store)
        loadLogs(dir, "Fox"+str(id)+"maxtelemetry*.log", store)
        loadLogs(dir, "Fox"+str(id)+"radtelemetry*.log", store)
        print("... ID:"+str(id)+" Loaded: " + str(len(store)) + " records")

def diff(server, local):
    missing = 0
    for k in local:
        if (k not in server):
            
            missing = missing + 1
    print("Total Records Missed: " + str(missing))

def main():
    serverRecords = {}
    records = {}
    if len(sys.argv) <= 2:
        print("Usage: diffFoxdb <Server FOXDB> <FOXDB>")
        sys.exit(1)
    serverDir = sys.argv[1]
    dir = sys.argv[2]
    print("DiffFOXDB:")
    load(dir, records)
    load(serverDir, serverRecords)
    diff(serverRecords, records)

main()
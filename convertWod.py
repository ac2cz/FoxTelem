'''
Created on Dec 22, 2017

@author: chris
'''
import sys
import os
import glob
import errno
from shutil import copyfile

def processLog(log):
    print("  processing: " + log + " ...")
    excludeList = [5,6,7]
    includeValues = ['NULL', '-999', '-998', '-997']
    firstRun = True
    copyfile(log, log + ".bak")
    with open(log + ".bak", "r") as f:
        for line in f:
            fields = line.split(',')
            # next check the format
            if fields[excludeList[0]] in includeValues or '.' in fields[excludeList[0]]:
                if firstRun:
                    newLog = open(log, "w")
                    firstRun = False
                for x in range(0, len(fields)-1):
                    if (x not in excludeList):
                        newLog.write(fields[x] + ",")
                newLog.write(fields[len(fields)-1])
            else:
                print(" skipping, already processed")
                f.close()
                os.remove(log + ".bak")
                return
        if not firstRun:    
            newLog.close()
    f.close()
    os.remove(log + ".bak")
    
def convertLogs(dir, match):
    logs = glob.glob(dir + os.sep + match)
    if len(logs) == 0:
        print ('No log files found in ' + dir + ' for ' + match)
    for item in logs:
        log = os.path.basename(item)
        processLog(item)
        
def main():
    if len(sys.argv) <= 1:
        print("Usage: convert <FOXDB path>")
        sys.exit(1)
    dir = sys.argv[1]
    print("Converting WOD files to v1.06e format")
    convertLogs(dir, "Fox5wodtelemetry*.log")
    convertLogs(dir, "Fox5wodradtelemetry*.log")

main()
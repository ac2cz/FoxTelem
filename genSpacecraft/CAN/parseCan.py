# Generate the spacecraft files needed for FoxTelem from the UW HuskySat
# csv files saved from the CAN Packets Requirements GDOC
# g0kla@arrl.net

import sys

lineNum = 0;

def processSignals(frame, fileName, outFile, moduleNum):
    "This processes a set of signals for a given frame"

    outFileName = outFile + frame + ".csv"
    # Open the output file and write out the header and the structures
    lineNum = 0

    lines = loadCsvFile(fileName)
    moduleLine = 1
    lineType = 0
    data = ""
    for line in lines:
        frameName = line[2]
        if (frameName == frame):
            signal = line[0]
            conversion = 0
            unit = line[11]
            if (unit == ""):
                unit = "-"
            bits = line[5]
            print("  " + frame +": " + signal)
            data += str(lineNum) + ","
            lineNum = lineNum + 1
            data += "CAN" + ","
            data += signal + ","
            data += bits + ","
            data += unit + ","
            data += str(conversion) + ","
            data += frame + ","
            data += str(moduleNum) + ","
            data += str(moduleLine) + ","
            moduleLine = moduleLine + 1
            data += str(lineType) + ","
            data += signal + "," #shortName
            data += line[1] + "," # description
            data += '\n'

    outfile = open(outFileName, "w" )
    outfile.write(str(lineNum) + "," + 
        "TYPE" + "," + 
        "FIELD" +"," +
        "BITS" +"," +
        "UNIT" +"," +
        "CONVERSION" +"," +
        "MODULE" +"," +
        "MODULE_NUM" +"," +
        "MODULE_LINE" +"," +
        "LINE_TYPE" +"," +
        "SHORT_NAME" +"," +    
        "DESCRIPTION" + '\n')
    outfile.write(data)
    outfile.close()

if len(sys.argv) <= 3:
    print ('Usage: parseCan <FOXID> <frames.csv> <signals.csv>')
    print ('Generate the spacecraft files needed for FoxTelem from the CAN csv files')
    sys.exit(1)

def loadCsvFile(fileName):
    firstLine = True
    line = ""
    fields = []
    lines = []
    # open the infile and read all the content in as a set of lines
    try:
        with open(fileName) as infile:
            for line in infile:
                if (firstLine):
                    firstLine = False
                else:
                    # skip first line
                    fields = line.split(',')
                    lines.append(fields)
        infile.close()
        return lines
    except UnicodeDecodeError as e:
        print ("ERROR: Binary data found in the file.  Is it a CSV file?  Or the raw XLS?")
        print (e)
        print (line)
        print (fields)
        exit(1)

def processFrames(fileName, signalsFile, outFile):
    lines = loadCsvFile(fileName)
    moduleNum = 1
    for line in lines:
        frame = line[0]
        if (line[11].rstrip('\n') == "descoped"):
            print(frame + " ignored - descoped")
        else:
            print(frame)
            processSignals(frame, signalsFile, outFile, moduleNum)
            moduleNum = moduleNum + 1

foxId = sys.argv[1]    
framesFileName = sys.argv[2]
signalsFileName = sys.argv[3]
outFileName = foxId + '_CAN_'

print ('Processing '+ framesFileName + ' & ' + signalsFileName)
processFrames(framesFileName, signalsFileName, outFileName)
    

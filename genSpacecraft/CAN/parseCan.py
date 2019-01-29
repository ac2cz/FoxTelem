# Generate the spacecraft files needed for FoxTelem from the UW HuskySat
# csv files saved from the CAN Packets Requirements GDOC
# g0kla@arrl.net

import sys

lineNum = 0;

def writeCanIdLine(outfile, line, frame, moduleNum, lineType):
    outfile.write(str(line) + "," +
    "CAN" + "," + 
    "UW_ID" + str(line) +"," +
    "8" +"," +
    "NONE" +"," +
    "0" +"," +
    "NONE" +"," +
    "0," +
    "0," +
    str(lineType) +"," +
    "ID" + str(line) +"," +    
    "CAN ID with Length Encoded\n")


def processSignals(frame, fileName, outFile, moduleNum):
    "This processes a set of signals for a given frame"

    outFileName = outFile + frame + ".csv"
    # Open the output file and write out the header and the structures
    lineNum = 4 # start at 4 as we add in line 0-3 with the CAN ID

    lines = loadCsvFile(fileName)
    moduleLine = 1 # start at 1 because we ignore CAN_ID on 1
    lineType = 0
    data = ""
    totalBits = 0
    # Signal Columns
    FRAME = 2
    SIGNAL = 0
    UNITS = 11
    BITS = 5
    DESC = 1
    for line in lines:
        frameName = line[FRAME]
        if (frameName == frame):
            signal = line[SIGNAL].replace(frameName+"_",'') # if the signal contains the frame name then strip it
            conversion = 0
            unit = line[UNITS]
            if (unit == ""):
                unit = "-"
            bits = line[BITS]
            totalBits = totalBits + int(bits)
            description = line[DESC]
            if (description == ""):
                description = "No description provided"
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
            data += description + "," # description
            data += '\n'
    if (totalBits < 64):
        lineNum = lineNum + 1
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
    writeCanIdLine(outfile, 0, frame, moduleNum, lineType)
    writeCanIdLine(outfile, 1, frame, moduleNum, lineType)
    writeCanIdLine(outfile, 2, frame, moduleNum, lineType)
    writeCanIdLine(outfile,3, frame, moduleNum, lineType)
    outfile.write(data)
    if (totalBits < 64):
        # Add a row to pad the layout to 8 bytes as we seem to frequently have packets too long
        pad = (64 - totalBits)
        outfile.write(str(lineNum-1) + "," +
        "CAN" + "," + 
        "JUNK" +"," +
        str(pad) +"," +
        "NONE" +"," +
        "0" +"," +
        "NONE" +"," +
        "0" +"," +
        "0" +"," +
        str(lineType) +"," +
        "Junk" + "," +    
        "Unexpected bytes at the end of the CAN Packet\n")
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
        note = line[11].rstrip('\n')
        if (note == "descoped"):   # contains descope
            print(frame + " ignored - descoped")
        else:
            print(frame)
            processSignals(frame, signalsFile, outFile, moduleNum)
            #Leave all module nums as 1 so that we can allocate them dynamically
            #moduleNum = moduleNum + 1

foxId = sys.argv[1]    
framesFileName = sys.argv[2]
signalsFileName = sys.argv[3]
outFileName = ""

print ('Processing '+ framesFileName + ' & ' + signalsFileName)
processFrames(framesFileName, signalsFileName, outFileName)
    
